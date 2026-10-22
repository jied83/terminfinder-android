package de.jied.terminfinder.feature.edit

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import de.jied.terminfinder.domain.MonitorScheduler
import de.jied.terminfinder.domain.TaskRepository
import de.jied.terminfinder.mvi.MviViewModel

class EditViewModel(
    private val taskId: Long,
    private val tasks: TaskRepository,
    private val scheduler: MonitorScheduler,
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
                        locations = task.locations.joinToString(", "),
                        interval = task.intervalMinutes.toString(),
                        loaded = true,
                    )
                }
            }
        }
    }

    override fun dispatch(intent: EditIntent) = when (intent) {
        is EditIntent.LocationsChanged -> reduce { copy(locations = intent.value, error = null) }
        is EditIntent.IntervalChanged -> reduce { copy(interval = intent.value, error = null) }
        EditIntent.Save -> save()
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
