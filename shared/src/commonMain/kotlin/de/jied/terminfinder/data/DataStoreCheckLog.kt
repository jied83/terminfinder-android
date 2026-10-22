package de.jied.terminfinder.data

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import de.jied.terminfinder.domain.CheckLog
import de.jied.terminfinder.domain.CheckRecord
import de.jied.terminfinder.domain.isParsed

class DataStoreCheckLog(
    private val store: DataStore<StoredState>,
    private val limit: Int = 30,
) : CheckLog {

    override fun observeLatest(): Flow<Map<Long, CheckRecord>> =
        store.data.map { state -> state.records.mapValues { it.value.first() } }.distinctUntilChanged()

    override fun observe(taskId: Long): Flow<List<CheckRecord>> =
        store.data.map { it.records[taskId].orEmpty() }.distinctUntilChanged()

    override suspend fun knownDates(): Map<Long, Set<String>> =
        store.data.first().knownDates.mapValues { it.value.toSet() }

    override suspend fun append(record: CheckRecord) {
        store.updateData { state ->
            if (state.tasks.none { it.id == record.taskId }) return@updateData state
            val history = (listOf(record) + state.records[record.taskId].orEmpty()).take(limit)
            state.copy(
                records = state.records + (record.taskId to history),
                knownDates = if (record.status.isParsed) {
                    state.knownDates + (record.taskId to record.dates)
                } else {
                    state.knownDates
                },
            )
        }
    }
}
