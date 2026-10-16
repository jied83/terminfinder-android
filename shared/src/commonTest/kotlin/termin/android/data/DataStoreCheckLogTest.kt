package termin.android.data

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import termin.android.domain.CheckRecord
import terminwatcher.core.api.Status
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DataStoreCheckLogTest {

    @Test
    fun keepsOnlyNewestRecordsPerTask() = runTest {
        val log = DataStoreCheckLog(newStore(backgroundScope), limit = 3)

        (1..5).forEach { log.append(record(taskId = 1, second = it)) }
        log.append(record(taskId = 2, second = 9))

        assertEquals(listOf(5L, 4L, 3L), log.observe(1).first().map { it.at.epochSeconds })
        assertEquals(mapOf(1L to 5L, 2L to 9L), log.observeLatest().first().mapValues { it.value.at.epochSeconds })
    }

    private fun record(taskId: Long, second: Int) =
        CheckRecord(taskId, Instant.fromEpochSeconds(second.toLong()), Status.NONE, emptyList())

    private fun newStore(scope: CoroutineScope) = DataStoreFactory.create(
        storage = OkioStorage(FileSystem.SYSTEM, StoredStateSerializer) {
            FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "termin-${Random.nextLong()}.json".toPath()
        },
        scope = scope,
    )
}
