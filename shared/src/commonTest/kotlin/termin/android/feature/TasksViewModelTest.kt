package termin.android.feature

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import termin.android.FakeLog
import termin.android.FakeTasks
import termin.android.domain.CheckRecord
import termin.android.feature.tasks.TaskItem
import termin.android.feature.tasks.TasksEffect
import termin.android.feature.tasks.TasksIntent
import termin.android.feature.tasks.TasksViewModel
import termin.android.task
import terminwatcher.core.api.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TasksViewModelTest : MainDispatcherTest() {

    @Test
    fun listsTasksWithLatestStatus() = runTest {
        val at = Instant.fromEpochSeconds(100)
        val log = FakeLog().apply { append(CheckRecord(1, at, Status.AVAILABLE, listOf("2026-10-20"))) }
        val viewModel = TasksViewModel(FakeTasks(task(1, 2), task(2, 4)), log)

        assertEquals(
            listOf(
                TaskItem(1, 120686, Status.AVAILABLE, at),
                TaskItem(2, 120686, null, null),
            ),
            viewModel.state.value.items,
        )
    }

    @Test
    fun intentsBecomeNavigationEffects() = runTest {
        val viewModel = TasksViewModel(FakeTasks(), FakeLog())

        viewModel.dispatch(TasksIntent.Open(7))
        assertEquals(TasksEffect.ToStatus(7), viewModel.effects.first())
        viewModel.dispatch(TasksIntent.Add)
        assertEquals(TasksEffect.ToSetup, viewModel.effects.first())
        viewModel.dispatch(TasksIntent.Settings)
        assertEquals(TasksEffect.ToSettings, viewModel.effects.first())
    }
}
