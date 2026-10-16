package termin.android.domain

import kotlinx.coroutines.flow.Flow

interface CheckLog {
    fun observeLatest(): Flow<Map<Long, CheckRecord>>
    fun observe(taskId: Long): Flow<List<CheckRecord>>
    suspend fun append(record: CheckRecord)
}
