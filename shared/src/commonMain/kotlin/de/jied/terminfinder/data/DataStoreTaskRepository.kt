package de.jied.terminfinder.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import de.jied.terminfinder.domain.MonitorTask
import de.jied.terminfinder.domain.TaskDraft
import de.jied.terminfinder.domain.TaskRepository

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
                serviceName = draft.serviceName,
            )
            state.copy(tasks = state.tasks + task)
        }
        return task
    }

    override suspend fun update(id: Long, locations: List<Int>, intervalMinutes: Int) {
        store.updateData { state ->
            val requeried = state.tasks.any { it.id == id && it.locations != locations }
            state.copy(
                tasks = state.tasks.map { task ->
                    if (task.id == id) task.copy(locations = locations, intervalMinutes = intervalMinutes) else task
                },
                knownDates = if (requeried) state.knownDates - id else state.knownDates,
            )
        }
    }

    override suspend fun setServiceName(id: Long, name: String) {
        store.updateData { state ->
            if (state.tasks.none { it.id == id && it.serviceName != name }) return@updateData state
            state.copy(
                tasks = state.tasks.map { task ->
                    if (task.id == id) task.copy(serviceName = name) else task
                },
            )
        }
    }

    override suspend fun remove(id: Long) {
        store.updateData { state ->
            state.copy(
                tasks = state.tasks.filterNot { it.id == id },
                records = state.records - id,
                knownDates = state.knownDates - id,
            )
        }
    }
}
