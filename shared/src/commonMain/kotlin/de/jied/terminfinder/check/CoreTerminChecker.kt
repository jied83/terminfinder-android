package de.jied.terminfinder.check

import de.jied.terminfinder.domain.CheckRecord
import de.jied.terminfinder.domain.MonitorTask
import de.jied.terminfinder.domain.TerminChecker
import de.jied.terminfinder.logDebug
import terminwatcher.core.api.TerminClient
import kotlin.time.Clock

class CoreTerminChecker(
    private val client: TerminClient,
    private val clock: Clock,
) : TerminChecker {

    override suspend fun check(task: MonitorTask, knownDates: Set<String>): CheckRecord {
        logDebug("api check task=${task.id} service=${task.serviceId} locations=${task.locations}")
        val started = clock.now()
        val result = try {
            client.check(task.query, knownDates, filter = null)
        } catch (e: Throwable) {
            logDebug("api check task=${task.id} failed after ${clock.now() - started}: $e")
            throw e
        }
        logDebug(
            "api check task=${task.id} status=${result.status} dates=${result.dates} " +
                "fresh=${result.freshDates} in ${clock.now() - started}",
        )
        return CheckRecord(task.id, clock.now(), result.status, result.dates, result.freshDates, result.serviceName)
    }
}
