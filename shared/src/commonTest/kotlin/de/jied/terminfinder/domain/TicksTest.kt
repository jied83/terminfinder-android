package de.jied.terminfinder.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TicksTest {
    private val start = Instant.fromEpochSeconds(1_800_000_000)

    @Test
    fun periodIsGcdOfIntervals() {
        assertEquals(2, Ticks.period(listOf(2, 4)))
        assertEquals(2, Ticks.period(listOf(4, 6)))
        assertEquals(4, Ticks.period(listOf(4, 8)))
        assertEquals(10, Ticks.period(listOf(10)))
    }

    @Test
    fun periodIsNeverBelowMinimum() {
        assertEquals(Ticks.MIN_MINUTES, Ticks.period(listOf(1)))
    }

    @Test
    fun noTasksMeansNoPeriod() {
        assertNull(Ticks.period(emptyList()))
    }

    @Test
    fun intervalMustBeMultipleOfMinimum() {
        assertTrue(Ticks.isValidInterval(2))
        assertTrue(Ticks.isValidInterval(4))
        assertFalse(Ticks.isValidInterval(0))
        assertFalse(Ticks.isValidInterval(3))
    }

    @Test
    fun neverCheckedTaskIsDue() {
        assertTrue(Ticks.isDue(null, 4, 2, start))
    }

    @Test
    fun fourMinuteTaskRunsEverySecondTwoMinuteTick() {
        val last = start + 10.seconds
        assertFalse(Ticks.isDue(last, 4, 2, start + 2.minutes))
        assertTrue(Ticks.isDue(last, 4, 2, start + 4.minutes))
    }

    @Test
    fun lateAlarmDoesNotSkipTick() {
        assertTrue(Ticks.isDue(start + 30.seconds, 2, 2, start + 2.minutes))
    }
}
