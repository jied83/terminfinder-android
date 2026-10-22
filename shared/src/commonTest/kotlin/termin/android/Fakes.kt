package termin.android

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import termin.android.domain.AppSettings
import termin.android.domain.CheckLog
import termin.android.domain.CheckRecord
import termin.android.domain.MonitorScheduler
import termin.android.domain.MonitorTask
import termin.android.domain.NewDatesNotifier
import termin.android.domain.SettingsRepository
import termin.android.domain.TaskDraft
import termin.android.domain.TaskRepository
import termin.android.domain.TerminChecker
import termin.android.domain.isParsed
import terminwatcher.core.api.Status
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

class FakeTasks(vararg initial: MonitorTask) : TaskRepository {
    val tasks = MutableStateFlow(initial.toList())

    override fun observeAll(): Flow<List<MonitorTask>> = tasks

    override suspend fun add(draft: TaskDraft): MonitorTask {
        val id = (tasks.value.maxOfOrNull { it.id } ?: 0) + 1
        val task = MonitorTask(id, draft.serviceId, draft.locations, draft.intervalMinutes)
        tasks.update { it + task }
        return task
    }

    override suspend fun update(id: Long, locations: List<Int>, intervalMinutes: Int) = tasks.update { all ->
        all.map { if (it.id == id) it.copy(locations = locations, intervalMinutes = intervalMinutes) else it }
    }

    override suspend fun remove(id: Long) = tasks.update { all -> all.filterNot { it.id == id } }
}

class FakeLog : CheckLog {
    val records = MutableStateFlow<List<CheckRecord>>(emptyList())
    val known = mutableMapOf<Long, Set<String>>()

    override fun observeLatest(): Flow<Map<Long, CheckRecord>> =
        records.map { all -> all.groupBy { it.taskId }.mapValues { it.value.first() } }

    override fun observe(taskId: Long): Flow<List<CheckRecord>> = records.map { all -> all.filter { it.taskId == taskId } }

    override suspend fun knownDates(): Map<Long, Set<String>> = known.toMap()

    override suspend fun append(record: CheckRecord) {
        if (record.status.isParsed) known[record.taskId] = record.dates.toSet()
        records.update { listOf(record) + it }
    }
}

class FakeSettings(notifyNewDates: Boolean = true) : SettingsRepository {
    val settings = MutableStateFlow(AppSettings(notifyNewDates))

    override fun observe(): Flow<AppSettings> = settings

    override suspend fun setNotifyNewDates(enabled: Boolean) =
        settings.update { it.copy(notifyNewDates = enabled) }
}

class FakeNotifier : NewDatesNotifier {
    val sent = mutableListOf<Pair<Long, List<String>>>()

    override suspend fun notify(task: MonitorTask, dates: List<String>) {
        sent += task.id to dates
    }
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

/**
 * Answers every check with [status] and [dates], deriving new dates the same way the real portal client
 * does: only a readable page can report them, and only against what the caller already knew.
 */
class FakeChecker(
    private val clock: Clock,
    var status: Status = Status.NONE,
    var dates: List<String> = emptyList(),
) : TerminChecker {
    val checked = mutableListOf<Long>()

    override suspend fun check(task: MonitorTask, knownDates: Set<String>): CheckRecord {
        checked += task.id
        val fresh = if (status == Status.AVAILABLE) (dates - knownDates).sorted() else emptyList()
        return CheckRecord(task.id, clock.now(), status, if (status.isParsed) dates else emptyList(), fresh)
    }
}

fun task(id: Long, intervalMinutes: Int) = MonitorTask(id, 120686, emptyList(), intervalMinutes)
