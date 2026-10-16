package termin.android

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import termin.android.domain.CheckLog
import termin.android.domain.CheckRecord
import termin.android.domain.MonitorScheduler
import termin.android.domain.MonitorTask
import termin.android.domain.TaskDraft
import termin.android.domain.TaskRepository
import termin.android.domain.TerminChecker
import terminwatcher.core.api.Status
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class FakeTasks(vararg initial: MonitorTask) : TaskRepository {
    val tasks = MutableStateFlow(initial.toList())

    override fun observeAll(): Flow<List<MonitorTask>> = tasks

    override suspend fun add(draft: TaskDraft): MonitorTask {
        val task = MonitorTask(tasks.value.size + 1L, draft.serviceId, draft.locations, draft.intervalMinutes)
        tasks.update { it + task }
        return task
    }
}

class FakeLog : CheckLog {
    val records = MutableStateFlow<List<CheckRecord>>(emptyList())

    override fun observeLatest(): Flow<Map<Long, CheckRecord>> =
        records.map { all -> all.groupBy { it.taskId }.mapValues { it.value.first() } }

    override fun observe(taskId: Long): Flow<List<CheckRecord>> = records.map { all -> all.filter { it.taskId == taskId } }

    override suspend fun append(record: CheckRecord) = records.update { listOf(record) + it }
}

class FakeScheduler : MonitorScheduler {
    var calls = 0

    override suspend fun reschedule() {
        calls++
    }
}

class FakeClock(var now: Instant = Instant.fromEpochSeconds(1_800_000_000)) : Clock {
    override fun now(): Instant = now

    fun advance(by: Duration) {
        now += by
    }
}

class FakeChecker(private val clock: Clock, private val status: Status = Status.NONE) : TerminChecker {
    val checked = mutableListOf<Long>()

    override suspend fun check(task: MonitorTask): CheckRecord {
        checked += task.id
        return CheckRecord(task.id, clock.now(), status, emptyList())
    }
}

fun task(id: Long, intervalMinutes: Int) = MonitorTask(id, 120686, emptyList(), intervalMinutes)
