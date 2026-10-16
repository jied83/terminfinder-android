package termin.android.feature.setup

import termin.android.domain.TaskDraft
import termin.android.domain.Ticks

enum class SetupStep { SERVICE, LOCATIONS, INTERVAL }

data class SetupState(
    val step: SetupStep = SetupStep.SERVICE,
    val service: String = "",
    val locations: String = "",
    val interval: String = "",
    val error: SetupStep? = null,
    val saving: Boolean = false,
) {
    val serviceId: Int? get() = service.trim().toIntOrNull()?.takeIf { it > 0 }

    val locationIds: List<Int>?
        get() = if (locations.isBlank()) {
            emptyList()
        } else {
            locations.split(',').map { part -> part.trim().toIntOrNull()?.takeIf { it > 0 } ?: return null }
        }

    val intervalMinutes: Int? get() = interval.trim().toIntOrNull()?.takeIf(Ticks::isValidInterval)

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
