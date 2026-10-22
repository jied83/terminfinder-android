package de.jied.terminfinder.feature.edit

import de.jied.terminfinder.domain.TaskInput

enum class EditField { LOCATIONS, INTERVAL }

data class EditState(
    val serviceId: Int = 0,
    val serviceName: String? = null,
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
