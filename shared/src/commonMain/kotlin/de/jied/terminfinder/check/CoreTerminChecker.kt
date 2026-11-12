package de.jied.terminfinder.check

import de.jied.terminfinder.domain.CheckRecord
import de.jied.terminfinder.domain.DateMatch
import de.jied.terminfinder.domain.MonitorTask
import de.jied.terminfinder.domain.TerminChecker
import de.jied.terminfinder.logDebug
import terminwatcher.core.api.Status
import terminwatcher.core.api.TerminClient
import kotlin.time.Clock

class CoreTerminChecker(
    private val client: TerminClient,
    private val clock: Clock,
) : TerminChecker {

    override suspend fun check(task: MonitorTask, knownDates: Set<String>): CheckRecord {
        logDebug("api check task=${task.id} service=${task.serviceId} locations=${task.locations} dates=${task.dates}")
        val started = clock.now()
        // core's own `matches` covers only new days, so availability is narrowed here instead.
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
        val selection = task.dates?.takeIf { it.isValid }
            ?: return CheckRecord(task.id, clock.now(), result.status, result.dates, result.freshDates, result.serviceName)

        // The record keeps only wanted dates, so they also become the known-dates baseline: an unwanted day
        // stays "fresh" on every check but never survives this filter.
        val wanted = DateMatch.wanted(result.days, selection, client.today())
        val status = if (result.status == Status.AVAILABLE && wanted.isEmpty()) Status.NONE else result.status
        logDebug("api check task=${task.id} wanted=$wanted status=$status")
        return CheckRecord(
            taskId = task.id,
            at = clock.now(),
            status = status,
            dates = wanted,
            newDates = result.freshDates.filter { it in wanted },
            serviceName = result.serviceName,
        )
    }
}
