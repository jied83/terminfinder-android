package termin.android.feature.status

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import termin.android.domain.CheckLog
import termin.android.domain.MonitorScheduler
import termin.android.domain.TaskRepository
import termin.android.mvi.MviViewModel

class StatusViewModel(
    private val taskId: Long,
    private val tasks: TaskRepository,
    log: CheckLog,
    private val scheduler: MonitorScheduler,
) : MviViewModel<StatusIntent, StatusState, StatusEffect>(StatusState()) {

    init {
        val task = tasks.observeAll().map { all -> all.firstOrNull { it.id == taskId } }.filterNotNull()
        combine(task, log.observe(taskId)) { current, history -> current to history }
            .onEach { (current, history) ->
                reduce { copy(serviceId = current.serviceId, bookingUrl = current.query.serviceUrl, history = history) }
            }
            .launchIn(viewModelScope)
    }

    override fun dispatch(intent: StatusIntent) = when (intent) {
        StatusIntent.Book -> if (state.value.canBook) emit(StatusEffect.OpenUrl(state.value.bookingUrl)) else Unit
        StatusIntent.Edit -> emit(StatusEffect.ToEdit(taskId))
        StatusIntent.Delete -> reduce { copy(confirmingDelete = true) }
        StatusIntent.DismissDelete -> reduce { copy(confirmingDelete = false) }
        StatusIntent.ConfirmDelete -> delete()
    }

    private fun delete() {
        if (!state.value.confirmingDelete) return
        reduce { copy(confirmingDelete = false) }
        viewModelScope.launch {
            tasks.remove(taskId)
            scheduler.reschedule()
            emit(StatusEffect.Deleted)
        }
    }
}
