package de.jied.terminfinder.domain

import kotlinx.coroutines.test.runTest
import de.jied.terminfinder.FakeChecker
import de.jied.terminfinder.FakeClock
import de.jied.terminfinder.FakeLog
import de.jied.terminfinder.FakeNotifier
import de.jied.terminfinder.FakeSettings
import de.jied.terminfinder.FakeTasks
import de.jied.terminfinder.task
import terminwatcher.core.api.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class DueMonitorRunnerTest {
    private val clock = FakeClock()
    private val log = FakeLog()
    private val checker = FakeChecker(clock)
    private val settings = FakeSettings()
    private val notifier = FakeNotifier()

    private fun runner(tasks: FakeTasks) =
        DueMonitorRunner(tasks, log, checker, clock, settings, notifier)

    @Test
    fun runsEachTaskAtItsOwnInterval() = runTest {
        val runner = runner(FakeTasks(task(1, 2), task(2, 4)))

        repeat(4) {
            runner.runDue()
            clock.advance(2.minutes)
        }

        assertEquals(listOf(1L, 2L, 1L, 1L, 2L, 1L), checker.checked)
        assertEquals(6, log.records.value.size)
    }

    @Test
    fun storesTheServiceNameReportedByTheCheckAndKeepsItWhenAbsent() = runTest {
        val tasks = FakeTasks(task(1, 2))
        val runner = runner(tasks)
        checker.serviceName = "Personalausweis beantragen"

        runner.runDue()

        assertEquals("Personalausweis beantragen", tasks.tasks.value.single().serviceName)

        checker.serviceName = null
        clock.advance(2.minutes)
        runner.runDue()

        assertEquals("Personalausweis beantragen", tasks.tasks.value.single().serviceName)
    }

    @Test
    fun doesNothingWithoutTasks() = runTest {
        runner(FakeTasks()).runDue()

        assertEquals(emptyList(), checker.checked)
    }

    @Test
    fun notifiesOncePerNewDate() = runTest {
        val runner = runner(FakeTasks(task(1, 2)))
        checker.status = Status.AVAILABLE
        checker.dates = listOf("2026-10-21")

        runner.runDue()
        clock.advance(2.minutes)
        runner.runDue()
        clock.advance(2.minutes)
        checker.dates = listOf("2026-10-21", "2026-10-23")
        runner.runDue()

        assertEquals(
            listOf(1L to listOf("2026-10-21"), 1L to listOf("2026-10-23")),
            notifier.sent,
        )
    }

    @Test
    fun staysSilentWhenTheSettingIsOff() = runTest {
        settings.setNotifyNewDates(false)
        checker.status = Status.AVAILABLE
        checker.dates = listOf("2026-10-21")

        runner(FakeTasks(task(1, 2))).runDue()

        assertEquals(emptyList(), notifier.sent)
        assertEquals(listOf("2026-10-21"), log.records.value.single().newDates)
    }

    @Test
    fun blockedChecksDoNotResetTheBaseline() = runTest {
        val runner = runner(FakeTasks(task(1, 2)))
        checker.status = Status.AVAILABLE
        checker.dates = listOf("2026-10-21")
        runner.runDue()

        // The portal rate-limits for longer than the log keeps records, then comes back unchanged.
        checker.status = Status.BLOCKED
        repeat(40) {
            clock.advance(2.minutes)
            runner.runDue()
        }
        checker.status = Status.AVAILABLE
        clock.advance(2.minutes)
        runner.runDue()

        assertEquals(listOf(1L to listOf("2026-10-21")), notifier.sent)
    }

    @Test
    fun noAppointmentsClearTheBaselineSoTheyAlertAgain() = runTest {
        val runner = runner(FakeTasks(task(1, 2)))
        checker.status = Status.AVAILABLE
        checker.dates = listOf("2026-10-21")
        runner.runDue()

        checker.status = Status.NONE
        checker.dates = emptyList()
        clock.advance(2.minutes)
        runner.runDue()

        checker.status = Status.AVAILABLE
        checker.dates = listOf("2026-10-21")
        clock.advance(2.minutes)
        runner.runDue()

        assertEquals(
            listOf(1L to listOf("2026-10-21"), 1L to listOf("2026-10-21")),
            notifier.sent,
        )
    }
}
