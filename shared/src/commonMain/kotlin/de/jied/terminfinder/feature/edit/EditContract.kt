package de.jied.terminfinder.feature.edit

import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.CatalogSearch
import de.jied.terminfinder.domain.MonitorTask
import de.jied.terminfinder.domain.TaskInput

enum class EditField { LOCATIONS, INTERVAL }

data class EditState(
    val serviceId: Int = 0,
    val serviceName: String? = null,
    val locations: String = "",
    val locationQuery: String? = null,
    val locationOptions: List<CatalogEntry> = emptyList(),
    val locationsLoading: Boolean = false,
    val locationsFailed: Boolean = false,
    val interval: String = "",
    val error: EditField? = null,
    val loaded: Boolean = false,
    val saving: Boolean = false,
) {
    val locationIds: List<Int>? get() = TaskInput.locationIds(locations)

    val intervalMinutes: Int? get() = TaskInput.intervalMinutes(interval)

    val locationField: String get() = locationQuery ?: locations
    val visibleLocations: List<CatalogEntry>
        get() = locationQuery
            ?.takeIf { it.trim().length >= CatalogSearch.MIN_QUERY_LENGTH }
            ?.let { CatalogSearch.match(it, locationOptions) }
            ?: locationOptions
    val selectedLocations: Set<Int> get() = locationIds?.toSet() ?: emptySet()
}

sealed interface EditIntent {
    data class LocationsChanged(val value: String) : EditIntent
    data class LocationToggled(val location: CatalogEntry) : EditIntent
    data class IntervalChanged(val value: String) : EditIntent
    data object Save : EditIntent

    data class Loaded(val task: MonitorTask) : EditIntent
    data object Missing : EditIntent
    data class OfficesLoaded(val offices: List<CatalogEntry>) : EditIntent
    data object OfficesFailed : EditIntent
    data object Saved : EditIntent
}

sealed interface EditEffect {
    data class Load(val taskId: Long) : EditEffect
    data class LoadOffices(val serviceId: Int) : EditEffect
    data class Save(val taskId: Long, val locations: List<Int>, val interval: Int) : EditEffect
}

sealed interface EditMessage {
    data object Done : EditMessage
}
