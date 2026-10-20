package termin.android.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import termin.android.logDebug
import kotlin.time.Clock

class DueMonitorRunner(
    private val tasks: TaskRepository,
    private val log: CheckLog,
    private val checker: TerminChecker,
    private val clock: Clock,
) : MonitorRunner {
    private val mutex = Mutex()

    override suspend fun runDue() = mutex.withLock {
        val all = tasks.observeAll().first()
        val period = Ticks.period(all.map { it.intervalMinutes })
        if (period == null) {
            logDebug("runDue: no tasks configured, nothing to check")
            return@withLock
        }
        val latest = log.observeLatest().first()
        val now = clock.now()
        val due = all.filter { Ticks.isDue(latest[it.id]?.at, it.intervalMinutes, period, now) }
        logDebug("runDue: tasks=${all.size} period=${period}m due=${due.map { it.id }}")
        due.forEach { log.append(checker.check(it)) }
    }
}
