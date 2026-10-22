package de.jied.terminfinder.feature

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import de.jied.terminfinder.FakeScheduler
import de.jied.terminfinder.FakeTasks
import de.jied.terminfinder.domain.MonitorTask
import de.jied.terminfinder.feature.setup.SetupEffect
import de.jied.terminfinder.feature.setup.SetupIntent
import de.jied.terminfinder.feature.setup.SetupStep
import de.jied.terminfinder.feature.setup.SetupViewModel
import kotlin.test.Test
import kotlin.test.assertEquals

class SetupViewModelTest : MainDispatcherTest() {
    private val tasks = FakeTasks()
    private val scheduler = FakeScheduler()
    private val viewModel by lazy { SetupViewModel(tasks, scheduler) }

    @Test
    fun invalidStepDoesNotAdvance() {
        viewModel.dispatch(SetupIntent.ServiceChanged("abc"))
        viewModel.dispatch(SetupIntent.Next)

        assertEquals(SetupStep.SERVICE, viewModel.state.value.step)
        assertEquals(SetupStep.SERVICE, viewModel.state.value.error)
    }

    @Test
    fun intervalMustBeMultipleOfTwo() {
        fillUpToInterval()
        viewModel.dispatch(SetupIntent.IntervalChanged("3"))
        viewModel.dispatch(SetupIntent.Submit)

        assertEquals(SetupStep.INTERVAL, viewModel.state.value.error)
        assertEquals(emptyList(), tasks.tasks.value)
    }

    @Test
    fun submitSavesTaskAndReschedules() = runTest {
        fillUpToInterval()
        viewModel.dispatch(SetupIntent.IntervalChanged("4"))
        viewModel.dispatch(SetupIntent.Submit)

        assertEquals(SetupEffect.Done, viewModel.effects.first())
        assertEquals(listOf(MonitorTask(1, 120686, listOf(122210, 122217), 4)), tasks.tasks.value)
        assertEquals(1, scheduler.calls)
    }

    private fun fillUpToInterval() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        viewModel.dispatch(SetupIntent.Next)
        viewModel.dispatch(SetupIntent.LocationsChanged("122210, 122217"))
        viewModel.dispatch(SetupIntent.Next)
        assertEquals(SetupStep.INTERVAL, viewModel.state.value.step)
    }
}
