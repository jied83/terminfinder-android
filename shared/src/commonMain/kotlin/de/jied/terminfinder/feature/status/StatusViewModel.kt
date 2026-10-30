package de.jied.terminfinder.feature.status

import androidx.lifecycle.viewModelScope
import de.jied.terminfinder.domain.CheckLog
import de.jied.terminfinder.domain.TaskRepository
import de.jied.terminfinder.mvi.EffectHandler
import de.jied.terminfinder.mvi.MviViewModel
import de.jied.terminfinder.mvi.noEffect
import de.jied.terminfinder.mvi.perform
import de.jied.terminfinder.mvi.send
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

class StatusViewModel(
    private val taskId: Long,
    tasks: TaskRepository,
    log: CheckLog,
    effects: EffectHandler<StatusEffect, StatusIntent>,
    background: CoroutineDispatcher = Dispatchers.Default,
) : MviViewModel<StatusIntent, StatusState, StatusEffect, StatusMessage>(StatusState(), effects, background) {

    init {
        val task = tasks.observeAll().map { all -> all.firstOrNull { it.id == taskId } }.filterNotNull()
        combine(task, log.observe(taskId)) { current, history -> current to history }
            .onEach { (current, history) ->
                reduce {
                    copy(
                        serviceId = current.serviceId,
                        serviceName = current.serviceName,
                        bookingUrl = current.query.calendarUrl,
                        history = history,
                    ).noEffect
                }
            }
            .launchIn(viewModelScope)
    }

    override fun dispatch(intent: StatusIntent) = when (intent) {
        StatusIntent.Book -> reduce { if (canBook) send(StatusMessage.OpenUrl(bookingUrl)) else noEffect }
        StatusIntent.Edit -> reduce { send(StatusMessage.ToEdit(taskId)) }
        StatusIntent.Delete -> reduce { copy(confirmingDelete = true).noEffect }
        StatusIntent.DismissDelete -> reduce { copy(confirmingDelete = false).noEffect }
        StatusIntent.ConfirmDelete -> reduce {
            if (!confirmingDelete) noEffect
            else copy(confirmingDelete = false).perform(StatusEffect.Delete(taskId))
        }
        StatusIntent.Removed -> reduce { send(StatusMessage.Deleted) }
    }
}
