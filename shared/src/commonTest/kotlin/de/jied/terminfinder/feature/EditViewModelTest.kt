package de.jied.terminfinder.feature

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import de.jied.terminfinder.FakeScheduler
import de.jied.terminfinder.FakeTasks
import de.jied.terminfinder.domain.MonitorTask
import de.jied.terminfinder.feature.edit.EditEffect
import de.jied.terminfinder.feature.edit.EditField
import de.jied.terminfinder.feature.edit.EditIntent
import de.jied.terminfinder.feature.edit.EditViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditViewModelTest : MainDispatcherTest() {
    private val tasks = FakeTasks(MonitorTask(1, 120686, listOf(122210, 122217), 4))
    private val scheduler = FakeScheduler()
    private val viewModel by lazy { EditViewModel(1, tasks, scheduler) }

    @Test
    fun prefillsLocationsAndInterval() {
        assertTrue(viewModel.state.value.loaded)
        assertEquals(120686, viewModel.state.value.serviceId)
        assertEquals("122210, 122217", viewModel.state.value.locations)
        assertEquals("4", viewModel.state.value.interval)
    }

    @Test
    fun savingKeepsServiceAndStoresNewValues() = runTest {
        viewModel.dispatch(EditIntent.LocationsChanged("122210"))
        viewModel.dispatch(EditIntent.IntervalChanged("6"))
        viewModel.dispatch(EditIntent.Save)

        assertEquals(EditEffect.Done, viewModel.effects.first())
        assertEquals(listOf(MonitorTask(1, 120686, listOf(122210), 6)), tasks.tasks.value)
        assertEquals(1, scheduler.calls)
    }

    @Test
    fun invalidIntervalBlocksSave() {
        viewModel.dispatch(EditIntent.IntervalChanged("3"))
        viewModel.dispatch(EditIntent.Save)

        assertEquals(EditField.INTERVAL, viewModel.state.value.error)
        assertEquals(listOf(MonitorTask(1, 120686, listOf(122210, 122217), 4)), tasks.tasks.value)
        assertEquals(0, scheduler.calls)
    }

    @Test
    fun invalidLocationsBlockSave() {
        viewModel.dispatch(EditIntent.LocationsChanged("122210, abc"))
        viewModel.dispatch(EditIntent.Save)

        assertEquals(EditField.LOCATIONS, viewModel.state.value.error)
        assertEquals(listOf(MonitorTask(1, 120686, listOf(122210, 122217), 4)), tasks.tasks.value)
    }

    @Test
    fun missingTaskLeavesTheScreen() = runTest {
        val viewModel = EditViewModel(99, tasks, scheduler)

        assertEquals(EditEffect.Done, viewModel.effects.first())
    }
}
