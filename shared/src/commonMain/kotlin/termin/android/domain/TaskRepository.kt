package termin.android.domain

import kotlinx.coroutines.flow.Flow

interface TaskRepository {
    fun observeAll(): Flow<List<MonitorTask>>
    suspend fun add(draft: TaskDraft): MonitorTask
    suspend fun update(id: Long, locations: List<Int>, intervalMinutes: Int)
    suspend fun remove(id: Long)
}
