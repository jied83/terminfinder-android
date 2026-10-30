package de.jied.terminfinder.feature.edit

import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.MonitorTask
import de.jied.terminfinder.domain.TaskInput
import de.jied.terminfinder.mvi.EffectHandler
import de.jied.terminfinder.mvi.MviViewModel
import de.jied.terminfinder.mvi.noEffect
import de.jied.terminfinder.mvi.perform
import de.jied.terminfinder.mvi.send
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class EditViewModel(
    private val taskId: Long,
    effects: EffectHandler<EditEffect, EditIntent>,
    background: CoroutineDispatcher = Dispatchers.Default,
) : MviViewModel<EditIntent, EditState, EditEffect, EditMessage>(EditState(), effects, background) {

    init {
        reduce { perform(EditEffect.Load(taskId)) }
    }

    override fun dispatch(intent: EditIntent) = when (intent) {
        is EditIntent.LocationsChanged -> locationsChanged(intent.value)
        is EditIntent.LocationToggled -> locationToggled(intent.location)
        is EditIntent.IntervalChanged -> reduce { copy(interval = intent.value, error = null).noEffect }
        EditIntent.Save -> save()
        is EditIntent.Loaded -> loaded(intent.task)
        EditIntent.Missing -> reduce { send(EditMessage.Done) }
        is EditIntent.OfficesLoaded -> reduce {
            copy(locationOptions = intent.offices, locationsLoading = false).noEffect
        }
        EditIntent.OfficesFailed -> reduce { copy(locationsLoading = false, locationsFailed = true).noEffect }
        EditIntent.Saved -> reduce { send(EditMessage.Done) }
    }

    private fun loaded(task: MonitorTask) = reduce {
        copy(
            serviceId = task.serviceId,
            serviceName = task.serviceName,
            locations = task.locations.joinToString(TaskInput.SEPARATOR),
            interval = task.intervalMinutes.toString(),
            loaded = true,
            locationsLoading = true,
            locationsFailed = false,
        ).perform(EditEffect.LoadOffices(task.serviceId))
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
        }.noEffect
    }

    private fun locationToggled(location: CatalogEntry) = reduce {
        copy(locations = TaskInput.toggleLocation(locations, location.id), error = null).noEffect
    }

    private fun save() = reduce {
        val ids = locationIds
        val minutes = intervalMinutes
        when {
            saving || !loaded -> noEffect
            ids == null -> copy(error = EditField.LOCATIONS).noEffect
            minutes == null -> copy(error = EditField.INTERVAL).noEffect
            else -> copy(saving = true).perform(EditEffect.Save(taskId, ids, minutes))
        }
    }
}
