package de.jied.terminfinder.feature.setup

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import de.jied.terminfinder.domain.MonitorScheduler
import de.jied.terminfinder.domain.TaskRepository
import de.jied.terminfinder.mvi.MviViewModel

class SetupViewModel(
    private val tasks: TaskRepository,
    private val scheduler: MonitorScheduler,
) : MviViewModel<SetupIntent, SetupState, SetupEffect>(SetupState()) {

    override fun dispatch(intent: SetupIntent) = when (intent) {
        is SetupIntent.ServiceChanged -> reduce { copy(service = intent.value, error = null) }
        is SetupIntent.LocationsChanged -> reduce { copy(locations = intent.value, error = null) }
        is SetupIntent.IntervalChanged -> reduce { copy(interval = intent.value, error = null) }
        SetupIntent.Next -> next()
        SetupIntent.Submit -> submit()
    }

    private fun next() = reduce {
        when {
            !isValid(step) -> copy(error = step)
            step == SetupStep.entries.last() -> this
            else -> copy(step = SetupStep.entries[step.ordinal + 1])
        }
    }

    private fun submit() {
        val current = state.value
        if (current.saving) return
        val draft = current.draft()
        if (draft == null) {
            reduce { copy(error = SetupStep.entries.first { !isValid(it) }) }
            return
        }
        reduce { copy(saving = true) }
        viewModelScope.launch {
            tasks.add(draft)
            scheduler.reschedule()
            emit(SetupEffect.Done)
        }
    }
}
