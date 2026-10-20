package termin.android.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import termin.android.domain.MonitorTask
import termin.android.domain.TaskDraft
import termin.android.domain.TaskRepository

class DataStoreTaskRepository(private val store: DataStore<StoredState>) : TaskRepository {

    override fun observeAll(): Flow<List<MonitorTask>> = store.data.map { it.tasks }.distinctUntilChanged()

    override suspend fun add(draft: TaskDraft): MonitorTask {
        lateinit var task: MonitorTask
        store.updateData { state ->
            task = MonitorTask(
                id = (state.tasks.maxOfOrNull { it.id } ?: 0) + 1,
                serviceId = draft.serviceId,
                locations = draft.locations,
                intervalMinutes = draft.intervalMinutes,
            )
            state.copy(tasks = state.tasks + task)
        }
        return task
    }

    override suspend fun update(id: Long, locations: List<Int>, intervalMinutes: Int) {
        store.updateData { state ->
            state.copy(
                tasks = state.tasks.map { task ->
                    if (task.id == id) task.copy(locations = locations, intervalMinutes = intervalMinutes) else task
                },
            )
        }
    }

    override suspend fun remove(id: Long) {
        store.updateData { state ->
            state.copy(tasks = state.tasks.filterNot { it.id == id }, records = state.records - id)
        }
    }
}
