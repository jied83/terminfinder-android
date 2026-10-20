package termin.android.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import termin.android.domain.CheckRecord
import termin.android.domain.TaskDraft
import terminwatcher.core.api.Status
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DataStoreCheckLogTest {

    @Test
    fun keepsOnlyNewestRecordsPerTask() = runTest {
        val store = newStore(backgroundScope)
        val tasks = DataStoreTaskRepository(store)
        tasks.add(TaskDraft(120686, emptyList(), 2))
        tasks.add(TaskDraft(120335, emptyList(), 4))
        val log = DataStoreCheckLog(store, limit = 3)

        (1..5).forEach { log.append(record(taskId = 1, second = it)) }
        log.append(record(taskId = 2, second = 9))

        assertEquals(listOf(5L, 4L, 3L), log.observe(1).first().map { it.at.epochSeconds })
        assertEquals(mapOf(1L to 5L, 2L to 9L), log.observeLatest().first().mapValues { it.value.at.epochSeconds })
    }

    @Test
    fun ignoresRecordsForTasksThatNoLongerExist() = runTest {
        val store = newStore(backgroundScope)
        val tasks = DataStoreTaskRepository(store)
        tasks.add(TaskDraft(120686, emptyList(), 2))
        val log = DataStoreCheckLog(store)

        tasks.remove(1)
        log.append(record(taskId = 1, second = 1))

        assertEquals(emptyList(), log.observe(1).first())
        assertEquals(emptyMap(), log.observeLatest().first())
    }

    private fun record(taskId: Long, second: Int) =
        CheckRecord(taskId, Instant.fromEpochSeconds(second.toLong()), Status.NONE, emptyList())

    private fun newStore(scope: CoroutineScope): DataStore<StoredState> = DataStoreFactory.create(
        storage = OkioStorage(FileSystem.SYSTEM, StoredStateSerializer) {
            FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "termin-${Random.nextLong()}.json".toPath()
        },
        scope = scope,
    )
}
