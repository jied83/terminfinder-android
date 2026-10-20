package termin.android.feature.edit

import termin.android.domain.TaskInput

enum class EditField { LOCATIONS, INTERVAL }

data class EditState(
    val serviceId: Int = 0,
    val locations: String = "",
    val interval: String = "",
    val error: EditField? = null,
    val loaded: Boolean = false,
    val saving: Boolean = false,
) {
    val locationIds: List<Int>? get() = TaskInput.locationIds(locations)

    val intervalMinutes: Int? get() = TaskInput.intervalMinutes(interval)
}

sealed interface EditIntent {
    data class LocationsChanged(val value: String) : EditIntent
    data class IntervalChanged(val value: String) : EditIntent
    data object Save : EditIntent
}

sealed interface EditEffect {
    data object Done : EditEffect
}
