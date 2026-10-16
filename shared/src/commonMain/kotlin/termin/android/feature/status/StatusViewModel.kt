package termin.android.feature.status

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import termin.android.domain.CheckLog
import termin.android.domain.TaskRepository
import termin.android.mvi.MviViewModel

class StatusViewModel(
    taskId: Long,
    tasks: TaskRepository,
    log: CheckLog,
) : MviViewModel<StatusIntent, StatusState, StatusEffect>(StatusState()) {

    init {
        val task = tasks.observeAll().map { all -> all.firstOrNull { it.id == taskId } }.filterNotNull()
        combine(task, log.observe(taskId)) { current, history ->
            StatusState(current.serviceId, current.query.serviceUrl, history)
        }
            .onEach { reduce { it } }
            .launchIn(viewModelScope)
    }

    override fun dispatch(intent: StatusIntent) = when (intent) {
        StatusIntent.Book -> if (state.value.canBook) emit(StatusEffect.OpenUrl(state.value.bookingUrl)) else Unit
    }
}
