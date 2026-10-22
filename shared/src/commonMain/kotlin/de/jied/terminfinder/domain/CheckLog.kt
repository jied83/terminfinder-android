package de.jied.terminfinder.domain

import kotlinx.coroutines.flow.Flow

interface CheckLog {
    fun observeLatest(): Flow<Map<Long, CheckRecord>>
    fun observe(taskId: Long): Flow<List<CheckRecord>>
    suspend fun knownDates(): Map<Long, Set<String>>
    suspend fun append(record: CheckRecord)
}
