package de.jied.terminfinder.feature.setup

import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.CatalogSearch
import de.jied.terminfinder.domain.LocationCatalog
import de.jied.terminfinder.domain.MonitorScheduler
import de.jied.terminfinder.domain.ServiceCatalog
import de.jied.terminfinder.domain.TaskRepository
import de.jied.terminfinder.logDebug
import de.jied.terminfinder.mvi.EffectHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

val SEARCH_DEBOUNCE: Duration = 250.milliseconds

class SetupEffectHandler(
    private val tasks: TaskRepository,
    private val scheduler: MonitorScheduler,
    private val catalog: ServiceCatalog,
    private val locations: LocationCatalog,
    private val searchDebounce: Duration = SEARCH_DEBOUNCE,
) : EffectHandler<SetupEffect, SetupIntent> {

    private val indexLock = Mutex()
    private var indexed: List<CatalogEntry> = emptyList()
    private var index: List<CatalogSearch.Entry> = emptyList()

    override suspend fun handle(effect: SetupEffect): SetupIntent = when (effect) {
        is SetupEffect.ResolveName -> SetupIntent.NameResolved(
            serviceId = effect.serviceId,
            name = catalog.cached().firstOrNull { it.id == effect.serviceId }?.name,
        )
        is SetupEffect.Search -> try {
            delay(searchDebounce)
            SetupIntent.SuggestionsLoaded(effect.query, search(effect.query))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            logDebug("service search for '${effect.query}' failed: $failure")
            SetupIntent.SearchFailed(effect.query)
        }
        is SetupEffect.LoadOffices -> try {
            SetupIntent.OfficesLoaded(effect.serviceId, locations.load(effect.serviceId))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            logDebug("office list for service ${effect.serviceId} failed: $failure")
            SetupIntent.OfficesFailed(effect.serviceId)
        }
        is SetupEffect.Save -> {
            tasks.add(effect.draft)
            scheduler.reschedule()
            SetupIntent.Saved
        }
    }

    private suspend fun search(query: String): List<CatalogEntry> =
        CatalogSearch.search(query, indexOf(catalog.load()))

    private suspend fun indexOf(services: List<CatalogEntry>): List<CatalogSearch.Entry> = indexLock.withLock {
        if (services !== indexed) {
            val built = CatalogSearch.index(services)
            indexed = services
            index = built
        }
        index
    }
}
