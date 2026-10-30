package de.jied.terminfinder.feature.tasks

import androidx.lifecycle.viewModelScope
import de.jied.terminfinder.domain.CheckLog
import de.jied.terminfinder.domain.TaskRepository
import de.jied.terminfinder.mvi.MviViewModel
import de.jied.terminfinder.mvi.NoEffects
import de.jied.terminfinder.mvi.noEffect
import de.jied.terminfinder.mvi.send
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TasksViewModel(
    tasks: TaskRepository,
    log: CheckLog,
) : MviViewModel<TasksIntent, TasksState, Nothing, TasksMessage>(TasksState(), NoEffects) {

    init {
        combine(tasks.observeAll(), log.observeLatest()) { all, latest ->
            all.map { TaskItem(it.id, it.serviceId, it.serviceName, latest[it.id]?.status, latest[it.id]?.at) }
        }
            .onEach { items -> reduce { copy(items = items).noEffect } }
            .launchIn(viewModelScope)
    }

    override fun dispatch(intent: TasksIntent) = when (intent) {
        is TasksIntent.Open -> reduce { send(TasksMessage.ToStatus(intent.id)) }
        TasksIntent.Add -> reduce { send(TasksMessage.ToSetup) }
        TasksIntent.Settings -> reduce { send(TasksMessage.ToSettings) }
    }
}
