package de.jied.terminfinder.feature.setup

import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.CatalogSearch
import de.jied.terminfinder.domain.TaskDraft
import de.jied.terminfinder.domain.TaskInput

enum class SetupStep { SERVICE, LOCATIONS, INTERVAL }

data class SetupState(
    val step: SetupStep = SetupStep.SERVICE,
    val service: String = "",
    val serviceName: String? = null,
    val suggestions: List<CatalogEntry> = emptyList(),
    val searching: Boolean = false,
    val catalogFailed: Boolean = false,
    val locations: String = "",
    val locationQuery: String? = null,
    val locationOptions: List<CatalogEntry> = emptyList(),
    val locationsLoading: Boolean = false,
    val locationsFailed: Boolean = false,
    val officesFor: Int? = null,
    val interval: String = "",
    val error: SetupStep? = null,
    val saving: Boolean = false,
) {
    val serviceId: Int? get() = TaskInput.serviceId(service)
    val locationIds: List<Int>? get() = TaskInput.locationIds(locations)
    val locationField: String get() = locationQuery ?: locations
    val visibleLocations: List<CatalogEntry>
        get() = locationQuery
            ?.takeIf { it.trim().length >= CatalogSearch.MIN_QUERY_LENGTH }
            ?.let { CatalogSearch.match(it, locationOptions) }
            ?: locationOptions

    val selectedLocations: Set<Int> get() = locationIds?.toSet() ?: emptySet()
    val intervalMinutes: Int? get() = TaskInput.intervalMinutes(interval)
    fun withoutOffices(): SetupState = copy(
        locations = "",
        locationQuery = null,
        locationOptions = emptyList(),
        locationsLoading = false,
        locationsFailed = false,
        officesFor = null,
    )

    fun isValid(step: SetupStep): Boolean = when (step) {
        SetupStep.SERVICE -> serviceId != null
        SetupStep.LOCATIONS -> locationIds != null
        SetupStep.INTERVAL -> intervalMinutes != null
    }

    fun draft(): TaskDraft? {
        return TaskDraft(
            serviceId = serviceId ?: return null,
            locations = locationIds ?: return null,
            intervalMinutes = intervalMinutes ?: return null,
            serviceName = serviceName,
        )
    }
}

sealed interface SetupIntent {
    data class ServiceChanged(val value: String) : SetupIntent
    data class ServiceSelected(val service: CatalogEntry) : SetupIntent
    data class LocationsChanged(val value: String) : SetupIntent
    data class LocationToggled(val location: CatalogEntry) : SetupIntent
    data class IntervalChanged(val value: String) : SetupIntent
    data object Next : SetupIntent
    data object Submit : SetupIntent

    data class NameResolved(val serviceId: Int, val name: String?) : SetupIntent
    data class SuggestionsLoaded(val query: String, val suggestions: List<CatalogEntry>) : SetupIntent
    data class SearchFailed(val query: String) : SetupIntent
    data class OfficesLoaded(val serviceId: Int, val offices: List<CatalogEntry>) : SetupIntent
    data class OfficesFailed(val serviceId: Int) : SetupIntent
    data object Saved : SetupIntent
}

sealed interface SetupEffect {
    data class ResolveName(val serviceId: Int) : SetupEffect
    data class Search(val query: String) : SetupEffect
    data class LoadOffices(val serviceId: Int) : SetupEffect
    data class Save(val draft: TaskDraft) : SetupEffect
}

sealed interface SetupMessage {
    data object Done : SetupMessage
}
