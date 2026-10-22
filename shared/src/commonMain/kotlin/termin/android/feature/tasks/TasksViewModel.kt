package termin.android.feature.tasks

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import termin.android.domain.CheckLog
import termin.android.domain.TaskRepository
import termin.android.mvi.MviViewModel

class TasksViewModel(
    tasks: TaskRepository,
    log: CheckLog,
) : MviViewModel<TasksIntent, TasksState, TasksEffect>(TasksState()) {

    init {
        combine(tasks.observeAll(), log.observeLatest()) { all, latest ->
            all.map { TaskItem(it.id, it.serviceId, latest[it.id]?.status, latest[it.id]?.at) }
        }
            .onEach { items -> reduce { copy(items = items) } }
            .launchIn(viewModelScope)
    }

    override fun dispatch(intent: TasksIntent) = when (intent) {
        is TasksIntent.Open -> emit(TasksEffect.ToStatus(intent.id))
        TasksIntent.Add -> emit(TasksEffect.ToSetup)
        TasksIntent.Settings -> emit(TasksEffect.ToSettings)
    }
}
