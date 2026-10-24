package de.jied.terminfinder.feature.setup

import androidx.lifecycle.viewModelScope
import de.jied.terminfinder.domain.MonitorScheduler
import de.jied.terminfinder.domain.ServiceCatalog
import de.jied.terminfinder.domain.ServiceInfo
import de.jied.terminfinder.domain.ServiceSearch
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
    private val searchDebounce: Duration = SEARCH_DEBOUNCE,
) : MviViewModel<SetupIntent, SetupState, SetupEffect>(SetupState()) {

    private var searchJob: Job? = null
    private var indexed: List<ServiceInfo> = emptyList()
    private var index: List<ServiceSearch.Entry> = emptyList()

    override fun dispatch(intent: SetupIntent) = when (intent) {
        is SetupIntent.ServiceChanged -> serviceChanged(intent.value)
        is SetupIntent.ServiceSelected -> serviceSelected(intent.service)
        is SetupIntent.LocationsChanged -> reduce { copy(locations = intent.value, error = null) }
        is SetupIntent.IntervalChanged -> reduce { copy(interval = intent.value, error = null) }
        SetupIntent.Next -> next()
        SetupIntent.Submit -> submit()
    }

    /** A number is taken as the service id; anything else is a name to look up in the catalog. */
    private fun serviceChanged(value: String) {
        searchJob?.cancel()
        val id = TaskInput.serviceId(value)
        reduce {
            copy(
                service = value,
                serviceName = id?.let { typed -> catalog.cached().firstOrNull { it.id == typed }?.name },
                suggestions = emptyList(),
                searching = false,
                catalogFailed = false,
                error = null,
            )
        }
        if (id == null && value.trim().length >= ServiceSearch.MIN_QUERY_LENGTH) search(value)
    }

    private fun search(query: String) {
        searchJob = viewModelScope.launch {
            delay(searchDebounce)
            reduce { copy(searching = true) }
            try {
                val found = ServiceSearch.search(query, indexOf(catalog.load()))
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
    private fun indexOf(services: List<ServiceInfo>): List<ServiceSearch.Entry> {
        if (services !== indexed) {
            indexed = services
            index = ServiceSearch.index(services)
        }
        return index
    }

    private fun serviceSelected(service: ServiceInfo) {
        searchJob?.cancel()
        reduce {
            copy(
                service = service.id.toString(),
                serviceName = service.name,
                suggestions = emptyList(),
                searching = false,
                error = null,
            )
        }
    }

    private fun next() = reduce {
        when {
            !isValid(step) -> copy(error = step)
            step == SetupStep.entries.last() -> this
            else -> copy(step = SetupStep.entries[step.ordinal + 1])
        }
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
