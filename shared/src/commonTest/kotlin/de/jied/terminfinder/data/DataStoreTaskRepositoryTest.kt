package de.jied.terminfinder.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import okio.Path.Companion.toPath
import de.jied.terminfinder.domain.CheckRecord
import de.jied.terminfinder.domain.MonitorTask
import de.jied.terminfinder.domain.TaskDraft
import terminwatcher.core.api.Status
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DataStoreTaskRepositoryTest {

    @Test
    fun updateChangesLocationsAndIntervalOnly() = runTest {
        val repository = DataStoreTaskRepository(newStore(backgroundScope))
        repository.add(TaskDraft(120686, listOf(122210), 4))
        val untouched = repository.add(TaskDraft(120335, emptyList(), 6))

        repository.update(1, listOf(122217, 122231), 10)

        assertEquals(
            listOf(MonitorTask(1, 120686, listOf(122217, 122231), 10), untouched),
            repository.observeAll().first(),
        )
    }

    @Test
    fun changingOfficesDropsTheKnownDatesBaselineButChangingTheIntervalKeepsIt() = runTest {
        val store = newStore(backgroundScope)
        val repository = DataStoreTaskRepository(store)
        val log = DataStoreCheckLog(store)
        repository.add(TaskDraft(120686, listOf(122210), 4))
        log.append(CheckRecord(1, Instant.fromEpochSeconds(1), Status.AVAILABLE, listOf("2026-10-20")))

        repository.update(1, listOf(122210), 10)
        assertEquals(mapOf(1L to setOf("2026-10-20")), log.knownDates())

        repository.update(1, listOf(122217), 10)
        assertEquals(emptyMap(), log.knownDates())
    }

    @Test
    fun removeDropsTheTaskAndItsHistoryWhoseIdCanBeReused() = runTest {
        val store = newStore(backgroundScope)
        val repository = DataStoreTaskRepository(store)
        val log = DataStoreCheckLog(store)
        repository.add(TaskDraft(120686, emptyList(), 4))
        log.append(CheckRecord(1, Instant.fromEpochSeconds(1), Status.AVAILABLE, listOf("2026-10-20")))

        repository.remove(1)
        val reused = repository.add(TaskDraft(120335, emptyList(), 6))

        assertEquals(1L, reused.id)
        assertEquals(listOf(reused), repository.observeAll().first())
        assertEquals(emptyList(), log.observe(1).first())
    }

    private fun newStore(scope: CoroutineScope): DataStore<StoredState> = DataStoreFactory.create(
        storage = OkioStorage(FileSystem.SYSTEM, StoredStateSerializer) {
            FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "termin-${Random.nextLong()}.json".toPath()
        },
        scope = scope,
    )
}
