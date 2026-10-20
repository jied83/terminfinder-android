package termin.android.feature.setup

import termin.android.domain.TaskDraft
import termin.android.domain.TaskInput

enum class SetupStep { SERVICE, LOCATIONS, INTERVAL }

data class SetupState(
    val step: SetupStep = SetupStep.SERVICE,
    val service: String = "",
    val locations: String = "",
    val interval: String = "",
    val error: SetupStep? = null,
    val saving: Boolean = false,
) {
    val serviceId: Int? get() = TaskInput.serviceId(service)

    val locationIds: List<Int>? get() = TaskInput.locationIds(locations)

    val intervalMinutes: Int? get() = TaskInput.intervalMinutes(interval)

    fun isValid(step: SetupStep): Boolean = when (step) {
        SetupStep.SERVICE -> serviceId != null
        SetupStep.LOCATIONS -> locationIds != null
        SetupStep.INTERVAL -> intervalMinutes != null
    }

    fun draft(): TaskDraft? {
        return TaskDraft(serviceId ?: return null, locationIds ?: return null, intervalMinutes ?: return null)
    }
}

sealed interface SetupIntent {
    data class ServiceChanged(val value: String) : SetupIntent
    data class LocationsChanged(val value: String) : SetupIntent
    data class IntervalChanged(val value: String) : SetupIntent
    data object Next : SetupIntent
    data object Submit : SetupIntent
}

sealed interface SetupEffect {
    data object Done : SetupEffect
}
