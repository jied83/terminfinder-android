package de.jied.terminfinder.feature

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import de.jied.terminfinder.FakeLog
import de.jied.terminfinder.FakeTasks
import de.jied.terminfinder.domain.CheckRecord
import de.jied.terminfinder.feature.tasks.TaskItem
import de.jied.terminfinder.feature.tasks.TasksMessage
import de.jied.terminfinder.feature.tasks.TasksIntent
import de.jied.terminfinder.feature.tasks.TasksViewModel
import de.jied.terminfinder.task
import terminwatcher.core.api.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class TasksViewModelTest : MainDispatcherTest() {

    @Test
    fun listsTasksWithLatestStatus() = runTest {
        val at = Instant.fromEpochSeconds(100)
        val log = FakeLog().apply { append(CheckRecord(1, at, Status.AVAILABLE, listOf("2026-10-20"))) }
        val named = task(1, 2).copy(serviceName = "Personalausweis beantragen")
        val viewModel = TasksViewModel(FakeTasks(named, task(2, 4)), log)

        assertEquals(
            listOf(
                TaskItem(1, 120686, "Personalausweis beantragen", Status.AVAILABLE, at),
                TaskItem(2, 120686, null, null, null),
            ),
            viewModel.state.value.items,
        )
    }

    @Test
    fun intentsBecomeNavigationEffects() = runTest {
        val viewModel = TasksViewModel(FakeTasks(), FakeLog())

        viewModel.dispatch(TasksIntent.Open(7))
        assertEquals(TasksMessage.ToStatus(7), viewModel.messages.first())
        viewModel.dispatch(TasksIntent.Add)
        assertEquals(TasksMessage.ToSetup, viewModel.messages.first())
        viewModel.dispatch(TasksIntent.Settings)
        assertEquals(TasksMessage.ToSettings, viewModel.messages.first())
    }
}
