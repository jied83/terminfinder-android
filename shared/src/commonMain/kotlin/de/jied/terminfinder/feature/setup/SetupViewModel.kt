package de.jied.terminfinder.feature.setup

import androidx.lifecycle.viewModelScope
import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.CatalogSearch
import de.jied.terminfinder.domain.LocationCatalog
import de.jied.terminfinder.domain.MonitorScheduler
import de.jied.terminfinder.domain.ServiceCatalog
import de.jied.terminfinder.domain.TaskInput
import de.jied.terminfinder.domain.TaskRepository
import de.jied.terminfinder.logDebug
import de.jied.terminfinder.mvi.MviViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

val SEARCH_DEBOUNCE: Duration = 250.milliseconds

class SetupViewModel(
    private val tasks: TaskRepository,
    private val scheduler: MonitorScheduler,
    private val catalog: ServiceCatalog,
    private val locationCatalog: LocationCatalog,
    private val searchDebounce: Duration = SEARCH_DEBOUNCE,
) : MviViewModel<SetupIntent, SetupState, SetupEffect>(SetupState()) {

    private var searchJob: Job? = null
    private var indexed: List<CatalogEntry> = emptyList()
    private var index: List<CatalogSearch.Entry> = emptyList()
    private var officesJob: Job? = null
    private var officesFor: Int? = null

    override fun dispatch(intent: SetupIntent) = when (intent) {
        is SetupIntent.ServiceChanged -> serviceChanged(intent.value)
        is SetupIntent.ServiceSelected -> serviceSelected(intent.service)
        is SetupIntent.LocationsChanged -> locationsChanged(intent.value)
        is SetupIntent.LocationToggled -> locationToggled(intent.location)
        is SetupIntent.IntervalChanged -> reduce { copy(interval = intent.value, error = null) }
        SetupIntent.Next -> next()
        SetupIntent.Submit -> submit()
    }

    private fun locationsChanged(value: String) = reduce {
        when {
            // an emptied filter reveals the ids again; an emptied id list means every office
            value.isEmpty() -> if (locationQuery != null) {
                copy(locationQuery = null, error = null)
            } else {
                copy(locations = "", error = null)
            }
            TaskInput.looksLikeIds(value) -> copy(locations = value, locationQuery = null, error = null)
            else -> copy(locationQuery = value, error = null)
        }
    }

    private fun locationToggled(location: CatalogEntry) = reduce {
        copy(locations = TaskInput.toggleLocation(locations, location.id), error = null)
    }

    private fun loadOffices() {
        val serviceId = state.value.serviceId ?: return
        if (serviceId == officesFor) return
        officesJob?.cancel()
        officesFor = serviceId
        reduce { copy(locationOptions = emptyList(), locationsLoading = true, locationsFailed = false) }
        officesJob = viewModelScope.launch {
            try {
                val found = locationCatalog.load(serviceId)
                reduce { copy(locationOptions = found, locationsLoading = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                logDebug("office list for service $serviceId failed: $failure")
                reduce { copy(locationsLoading = false, locationsFailed = true) }
            }
        }
    }

    private fun serviceChanged(value: String) {
        searchJob?.cancel()
        val id = TaskInput.serviceId(value)
        val keepOffices = id != null && id == officesFor
        reduce {
            copy(
                service = value,
                serviceName = id?.let { typed -> catalog.cached().firstOrNull { it.id == typed }?.name },
                suggestions = emptyList(),
                searching = false,
                catalogFailed = false,
                error = null,
            ).let { if (keepOffices) it else it.withoutOffices() }
        }
        if (!keepOffices) forgetOffices()
        if (id == null && value.trim().length >= CatalogSearch.MIN_QUERY_LENGTH) search(value)
    }

    private fun search(query: String) {
        searchJob = viewModelScope.launch {
            delay(searchDebounce)
            reduce { copy(searching = true) }
            try {
                val found = CatalogSearch.search(query, indexOf(catalog.load()))
                reduce { copy(suggestions = found, searching = false) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                logDebug("service search for '$query' failed: $failure")
                reduce { copy(searching = false, catalogFailed = true) }
            }
        }
    }

    /** Names are folded once per catalog, not once per keystroke. */
    private fun indexOf(services: List<CatalogEntry>): List<CatalogSearch.Entry> {
        if (services !== indexed) {
            indexed = services
            index = CatalogSearch.index(services)
        }
        return index
    }

    private fun serviceSelected(service: CatalogEntry) {
        searchJob?.cancel()
        val keepOffices = service.id == officesFor
        reduce {
            copy(
                service = service.id.toString(),
                serviceName = service.name,
                suggestions = emptyList(),
                searching = false,
                error = null,
            ).let { if (keepOffices) it else it.withoutOffices() }
        }
        if (!keepOffices) forgetOffices()
    }

    private fun forgetOffices() {
        officesJob?.cancel()
        officesFor = null
    }

    private fun next() {
        reduce {
            when {
                !isValid(step) -> copy(error = step)
                step == SetupStep.entries.last() -> this
                else -> copy(step = SetupStep.entries[step.ordinal + 1])
            }
        }
        if (state.value.step >= SetupStep.LOCATIONS) loadOffices()
    }

    private fun submit() {
        val current = state.value
        if (current.saving) return
        val draft = current.draft()
        if (draft == null) {
            reduce { copy(error = SetupStep.entries.first { !isValid(it) }) }
            return
        }
        reduce { copy(saving = true) }
        viewModelScope.launch {
            tasks.add(draft)
            scheduler.reschedule()
            emit(SetupEffect.Done)
        }
    }
}
