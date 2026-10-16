package termin.android.domain

import kotlinx.coroutines.test.runTest
import termin.android.FakeChecker
import termin.android.FakeClock
import termin.android.FakeLog
import termin.android.FakeTasks
import termin.android.task
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class DueMonitorRunnerTest {
    private val clock = FakeClock()
    private val log = FakeLog()
    private val checker = FakeChecker(clock)

    @Test
    fun runsEachTaskAtItsOwnInterval() = runTest {
        val runner = DueMonitorRunner(FakeTasks(task(1, 2), task(2, 4)), log, checker, clock)

        repeat(4) {
            runner.runDue()
            clock.advance(2.minutes)
        }

        assertEquals(listOf(1L, 2L, 1L, 1L, 2L, 1L), checker.checked)
        assertEquals(6, log.records.value.size)
    }

    @Test
    fun doesNothingWithoutTasks() = runTest {
        DueMonitorRunner(FakeTasks(), log, checker, clock).runDue()

        assertEquals(emptyList(), checker.checked)
    }
}
