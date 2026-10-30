package de.jied.terminfinder.feature.setup

import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.CatalogSearch
import de.jied.terminfinder.domain.TaskInput
import de.jied.terminfinder.mvi.EffectHandler
import de.jied.terminfinder.mvi.MviViewModel
import de.jied.terminfinder.mvi.Transition
import de.jied.terminfinder.mvi.noEffect
import de.jied.terminfinder.mvi.perform
import de.jied.terminfinder.mvi.send
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

class SetupViewModel(
    effects: EffectHandler<SetupEffect, SetupIntent>,
    background: CoroutineDispatcher = Dispatchers.Default,
) : MviViewModel<SetupIntent, SetupState, SetupEffect, SetupMessage>(SetupState(), effects, background) {

    override fun dispatch(intent: SetupIntent) = when (intent) {
        is SetupIntent.ServiceChanged -> serviceChanged(intent.value)
        is SetupIntent.ServiceSelected -> serviceSelected(intent.service)
        is SetupIntent.LocationsChanged -> locationsChanged(intent.value)
        is SetupIntent.LocationToggled -> locationToggled(intent.location)
        is SetupIntent.IntervalChanged -> reduce { copy(interval = intent.value, error = null).noEffect }
        SetupIntent.Next -> next()
        SetupIntent.Submit -> submit()
        is SetupIntent.NameResolved -> reduce {
            if (serviceId == intent.serviceId) copy(serviceName = intent.name).noEffect else noEffect
        }
        is SetupIntent.SuggestionsLoaded -> reduce {
            if (stale(intent.query)) noEffect
            else copy(suggestions = intent.suggestions, searching = false).noEffect
        }
        is SetupIntent.SearchFailed -> reduce {
            if (stale(intent.query)) noEffect else copy(searching = false, catalogFailed = true).noEffect
        }
        is SetupIntent.OfficesLoaded -> reduce {
            if (officesFor != intent.serviceId) noEffect
            else copy(locationOptions = intent.offices, locationsLoading = false).noEffect
        }
        is SetupIntent.OfficesFailed -> reduce {
            if (officesFor != intent.serviceId) noEffect
            else copy(locationsLoading = false, locationsFailed = true).noEffect
        }
        SetupIntent.Saved -> reduce { send(SetupMessage.Done) }
    }

    private fun SetupState.stale(query: String): Boolean = query != service

    private fun serviceChanged(value: String) = reduce {
        val id = TaskInput.serviceId(value)
        val keepOffices = id != null && id == officesFor
        val next = copy(
            service = value,
            serviceName = null,
            suggestions = emptyList(),
            searching = false,
            catalogFailed = false,
            error = null,
        ).let { if (keepOffices) it else it.withoutOffices() }
        when {
            id != null -> next.perform(SetupEffect.ResolveName(id))
            value.trim().length >= CatalogSearch.MIN_QUERY_LENGTH ->
                next.copy(searching = true).perform(SetupEffect.Search(value))
            else -> next.noEffect
        }
    }

    private fun serviceSelected(service: CatalogEntry) = reduce {
        val keepOffices = service.id == officesFor
        copy(
            service = service.id.toString(),
            serviceName = service.name,
            suggestions = emptyList(),
            searching = false,
            error = null,
        ).let { if (keepOffices) it else it.withoutOffices() }.noEffect
    }

    private fun locationsChanged(value: String) = reduce {
        when {
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

    private fun next() = reduce {
        when {
            !isValid(step) -> copy(error = step).noEffect
            step == SetupStep.entries.last() -> loadingOffices()
            else -> copy(step = SetupStep.entries[step.ordinal + 1]).loadingOffices()
        }
    }

    private fun SetupState.loadingOffices(): Transition<SetupState, SetupEffect, Nothing> {
        val id = serviceId
        if (step < SetupStep.LOCATIONS || id == null || officesFor == id) return noEffect
        return copy(
            locationOptions = emptyList(),
            locationsLoading = true,
            locationsFailed = false,
            officesFor = id,
        ).perform(SetupEffect.LoadOffices(id))
    }

    private fun submit() = reduce {
        val draft = draft()
        when {
            saving -> noEffect
            draft == null -> copy(error = SetupStep.entries.first { !isValid(it) }).noEffect
            else -> copy(saving = true).perform(SetupEffect.Save(draft))
        }
    }
}
