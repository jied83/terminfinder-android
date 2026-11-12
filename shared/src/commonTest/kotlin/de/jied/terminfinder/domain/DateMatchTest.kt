package de.jied.terminfinder.domain

import kotlinx.datetime.LocalDate
import terminwatcher.core.api.BookableDay
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DateMatchTest {
    private val today = LocalDate(2026, 10, 1)
    private val days = listOf("2026-10-05", "2026-10-10", "2026-10-11", "2026-10-20").map(::day)

    @Test
    fun aSingleDayMatchesOnlyItself() {
        assertEquals(listOf("2026-10-10"), DateMatch.wanted(days, DateSelection(days = listOf("2026-10-10")), today))
    }

    @Test
    fun aListMatchesEachOfItsDaysAndSkipsTheMissingOnes() {
        val selection = DateSelection(days = listOf("2026-10-20", "2026-10-05", "2026-10-07"))
        assertEquals(listOf("2026-10-05", "2026-10-20"), DateMatch.wanted(days, selection, today))
    }

    @Test
    fun aRangeIsInclusiveAtBothEnds() {
        val selection = DateSelection(from = "2026-10-10", to = "2026-10-20")
        assertEquals(listOf("2026-10-10", "2026-10-11", "2026-10-20"), DateMatch.wanted(days, selection, today))
    }

    @Test
    fun daysBeforeTodayNeverMatch() {
        val selection = DateSelection(from = "2026-10-01", to = "2026-10-31")
        assertEquals(listOf("2026-10-20"), DateMatch.wanted(days, selection, today = LocalDate(2026, 10, 12)))
    }

    @Test
    fun aDayWithoutAParsedDateNeverMatches() {
        val unparsed = BookableDay(date = null, label = "10", url = "")
        assertEquals(emptyList(), DateMatch.wanted(listOf(unparsed), DateSelection(from = "2026-10-01", to = "2026-10-31"), today))
    }

    @Test
    fun overlappingDaysAreReportedOnce() {
        val repeated = days + day("2026-10-10")
        assertEquals(listOf("2026-10-10"), DateMatch.wanted(repeated, DateSelection(days = listOf("2026-10-10")), today))
    }

    @Test
    fun validity() {
        assertTrue(DateSelection(days = listOf("2026-10-10")).isValid)
        assertTrue(DateSelection(from = "2026-10-10", to = "2026-10-10").isValid)
        assertFalse(DateSelection().isValid)
        assertFalse(DateSelection(days = listOf("10.10.2026")).isValid)
        assertFalse(DateSelection(from = "2026-10-20", to = "2026-10-10").isValid)
        assertFalse(DateSelection(from = "2026-10-10").isValid)
        assertFalse(DateSelection(days = listOf("2026-10-10"), from = "2026-10-10", to = "2026-10-11").isValid)
    }

    private fun day(iso: String) = BookableDay(LocalDate.parse(iso), iso, "")
}
