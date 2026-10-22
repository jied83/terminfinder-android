package de.jied.terminfinder.domain

import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

object Ticks {
    const val MIN_MINUTES = 2

    fun isValidInterval(minutes: Int): Boolean = minutes >= MIN_MINUTES && minutes % MIN_MINUTES == 0

    fun period(intervals: List<Int>): Int? = intervals.reduceOrNull(::gcd)?.coerceAtLeast(MIN_MINUTES)

    fun isDue(last: Instant?, intervalMinutes: Int, periodMinutes: Int, now: Instant): Boolean =
        last == null || now >= last + intervalMinutes.minutes - periodMinutes.minutes / 2

    private tailrec fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
}
