package de.jied.terminfinder.feature.edit

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.LocationCatalog
import de.jied.terminfinder.domain.MonitorScheduler
import de.jied.terminfinder.domain.TaskInput
import de.jied.terminfinder.domain.TaskRepository
import de.jied.terminfinder.logDebug
import de.jied.terminfinder.mvi.MviViewModel

class EditViewModel(
    private val taskId: Long,
    private val tasks: TaskRepository,
    private val scheduler: MonitorScheduler,
    private val locationCatalog: LocationCatalog,
) : MviViewModel<EditIntent, EditState, EditEffect>(EditState()) {

    init {
        viewModelScope.launch {
            val task = tasks.observeAll().first().firstOrNull { it.id == taskId }
            if (task == null) {
                emit(EditEffect.Done)
            } else {
                reduce {
                    copy(
                        serviceId = task.serviceId,
                        serviceName = task.serviceName,
                        locations = task.locations.joinToString(TaskInput.SEPARATOR),
                        interval = task.intervalMinutes.toString(),
                        loaded = true,
                    )
                }
                loadOffices(task.serviceId)
            }
        }
    }

    private suspend fun loadOffices(serviceId: Int) {
        reduce { copy(locationsLoading = true, locationsFailed = false) }
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

    override fun dispatch(intent: EditIntent) = when (intent) {
        is EditIntent.LocationsChanged -> locationsChanged(intent.value)
        is EditIntent.LocationToggled -> locationToggled(intent.location)
        is EditIntent.IntervalChanged -> reduce { copy(interval = intent.value, error = null) }
        EditIntent.Save -> save()
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

    private fun save() {
        val current = state.value
        if (current.saving || !current.loaded) return
        val locations = current.locationIds
        val interval = current.intervalMinutes
        when {
            locations == null -> reduce { copy(error = EditField.LOCATIONS) }
            interval == null -> reduce { copy(error = EditField.INTERVAL) }
            else -> {
                reduce { copy(saving = true) }
                viewModelScope.launch {
                    tasks.update(taskId, locations, interval)
                    scheduler.reschedule()
                    emit(EditEffect.Done)
                }
            }
        }
    }
}
