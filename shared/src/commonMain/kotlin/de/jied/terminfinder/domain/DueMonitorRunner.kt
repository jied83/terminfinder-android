package de.jied.terminfinder.domain

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import de.jied.terminfinder.logDebug
import kotlin.time.Clock

class DueMonitorRunner(
    private val tasks: TaskRepository,
    private val log: CheckLog,
    private val checker: TerminChecker,
    private val clock: Clock,
    private val settings: SettingsRepository,
    private val notifier: NewDatesNotifier,
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
        val known = log.knownDates()
        val notify = settings.observe().first().notifyNewDates
        due.forEach { task ->
            val record = checker.check(task, known[task.id].orEmpty())
            log.append(record)
            val name = record.serviceName?.trim()
            if (!name.isNullOrEmpty() && name != task.serviceName) {
                logDebug("runDue: task=${task.id} learned service name '$name'")
                tasks.setServiceName(task.id, name)
            }
            if (notify && record.newDates.isNotEmpty()) {
                logDebug("runDue: task=${task.id} new dates=${record.newDates}, notifying")
                notifier.notify(task, record.newDates)
            }
        }
    }
}
