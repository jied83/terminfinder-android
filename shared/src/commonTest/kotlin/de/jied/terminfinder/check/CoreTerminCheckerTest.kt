package de.jied.terminfinder.check

import de.jied.terminfinder.FakeClock
import de.jied.terminfinder.domain.DateSelection
import de.jied.terminfinder.domain.MonitorTask
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import terminwatcher.core.api.BookableDay
import terminwatcher.core.api.CheckResult
import terminwatcher.core.api.DateFilter
import terminwatcher.core.api.ServiceQuery
import terminwatcher.core.api.Status
import terminwatcher.core.api.TerminClient
import terminwatcher.core.api.freshDates
import kotlin.test.Test
import kotlin.test.assertEquals

class CoreTerminCheckerTest {
    private val client = PortalStub(listOf("2026-10-10", "2026-10-11", "2026-10-20"))
    private val checker = CoreTerminChecker(client, FakeClock())

    @Test
    fun withoutASelectionEveryDateCounts() = runTest {
        val record = checker.check(task(dates = null), knownDates = setOf("2026-10-10"))

        assertEquals(Status.AVAILABLE, record.status)
        assertEquals(listOf("2026-10-10", "2026-10-11", "2026-10-20"), record.dates)
        assertEquals(listOf("2026-10-11", "2026-10-20"), record.newDates)
    }

    @Test
    fun aSelectionKeepsOnlyTheWantedDates() = runTest {
        val record = checker.check(task(DateSelection(days = listOf("2026-10-11", "2026-10-12"))), emptySet())

        assertEquals(Status.AVAILABLE, record.status)
        assertEquals(listOf("2026-10-11"), record.dates)
        assertEquals(listOf("2026-10-11"), record.newDates)
    }

    @Test
    fun freeDatesOutsideTheSelectionReadAsNone() = runTest {
        val record = checker.check(task(DateSelection(from = "2026-11-01", to = "2026-11-30")), emptySet())

        assertEquals(Status.NONE, record.status)
        assertEquals(emptyList(), record.dates)
        assertEquals(emptyList(), record.newDates)
    }

    @Test
    fun aWantedDateAlreadyKnownIsNotNewAgain() = runTest {
        val selection = DateSelection(from = "2026-10-01", to = "2026-10-15")
        // the baseline is what the previous filtered check recorded
        val record = checker.check(task(selection), knownDates = setOf("2026-10-10"))

        assertEquals(listOf("2026-10-10", "2026-10-11"), record.dates)
        assertEquals(listOf("2026-10-11"), record.newDates)
    }

    @Test
    fun aBlockedPortalStaysBlocked() = runTest {
        client.status = Status.BLOCKED
        val record = checker.check(task(DateSelection(days = listOf("2026-10-11"))), emptySet())

        assertEquals(Status.BLOCKED, record.status)
        assertEquals(emptyList(), record.newDates)
    }

    private fun task(dates: DateSelection?) = MonitorTask(1, 120686, emptyList(), 4, dates = dates)

    /** Answers like the real client: fresh dates only from a readable page with free days. */
    private class PortalStub(private val dates: List<String>) : TerminClient {
        var status = Status.AVAILABLE

        override suspend fun check(query: ServiceQuery, knownDates: Set<String>, filter: DateFilter?): CheckResult {
            val available = status == Status.AVAILABLE
            return CheckResult(
                query = query,
                status = status,
                dates = if (available) dates else emptyList(),
                freshDates = if (available) freshDates(knownDates, dates) else emptyList(),
                days = if (available) dates.map { BookableDay(LocalDate.parse(it), it, "") } else emptyList(),
            )
        }

        override fun today(): LocalDate = LocalDate(2026, 10, 1)
    }
}
