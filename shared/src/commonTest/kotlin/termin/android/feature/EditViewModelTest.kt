package termin.android.feature

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import termin.android.FakeScheduler
import termin.android.FakeTasks
import termin.android.domain.MonitorTask
import termin.android.feature.edit.EditEffect
import termin.android.feature.edit.EditField
import termin.android.feature.edit.EditIntent
import termin.android.feature.edit.EditViewModel
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
