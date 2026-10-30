package de.jied.terminfinder.feature

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import de.jied.terminfinder.FakeLocationCatalog
import de.jied.terminfinder.FakeScheduler
import de.jied.terminfinder.FakeTasks
import de.jied.terminfinder.domain.MonitorTask
import de.jied.terminfinder.feature.edit.EditEffectHandler
import de.jied.terminfinder.feature.edit.EditMessage
import de.jied.terminfinder.feature.edit.EditField
import de.jied.terminfinder.feature.edit.EditIntent
import de.jied.terminfinder.feature.edit.EditViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditViewModelTest : MainDispatcherTest() {
    private val tasks = FakeTasks(MonitorTask(1, 120686, listOf(122210, 122217), 4))
    private val scheduler = FakeScheduler()
    private val offices = FakeLocationCatalog()
    private val viewModel by lazy { EditViewModel(1, EditEffectHandler(tasks, scheduler, offices), dispatcher) }

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

        assertEquals(EditMessage.Done, viewModel.messages.first())
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
        viewModel.dispatch(EditIntent.LocationsChanged("122210,"))
        viewModel.dispatch(EditIntent.Save)

        assertEquals(EditField.LOCATIONS, viewModel.state.value.error)
        assertEquals(listOf(MonitorTask(1, 120686, listOf(122210, 122217), 4)), tasks.tasks.value)
    }

    @Test
    fun typingANameFiltersInsteadOfOverwritingTheOffices() {
        viewModel.dispatch(EditIntent.LocationsChanged("mitte"))

        assertEquals("mitte", viewModel.state.value.locationField)
        assertEquals(listOf(122217), viewModel.state.value.visibleLocations.map { it.id })
        // the ids stay the value behind the filter, and stay checked
        assertEquals("122210, 122217", viewModel.state.value.locations)
        assertEquals(setOf(122210, 122217), viewModel.state.value.selectedLocations)
    }

    @Test
    fun filterThenToggleThenClearLeavesACleanIdList() = runTest {
        viewModel.dispatch(EditIntent.LocationsChanged("mitte"))
        val mitte = viewModel.state.value.visibleLocations.single()

        viewModel.dispatch(EditIntent.LocationToggled(mitte))
        // the filter survives the tick, so more offices can be picked from it
        assertEquals("mitte", viewModel.state.value.locationField)
        assertEquals(listOf(122217), viewModel.state.value.visibleLocations.map { it.id })

        viewModel.dispatch(EditIntent.LocationsChanged(""))

        assertEquals("122210", viewModel.state.value.locationField)
        assertEquals(listOf(122210, 122217), viewModel.state.value.visibleLocations.map { it.id })

        viewModel.dispatch(EditIntent.Save)

        assertEquals(EditMessage.Done, viewModel.messages.first())
        assertEquals(listOf(MonitorTask(1, 120686, listOf(122210), 4)), tasks.tasks.value)
    }

    @Test
    fun clearingTheIdListMeansEveryOffice() = runTest {
        viewModel.dispatch(EditIntent.LocationsChanged(""))
        viewModel.dispatch(EditIntent.Save)

        assertEquals(EditMessage.Done, viewModel.messages.first())
        assertEquals(emptyList(), tasks.tasks.value.single().locations)
    }

    @Test
    fun theOfficesOfTheEditedTaskAreOfferedAndPrefilledAsSelected() {
        assertEquals(listOf(122210, 122217), viewModel.state.value.locationOptions.map { it.id })
        assertEquals(setOf(122210, 122217), viewModel.state.value.selectedLocations)
        assertEquals(false, viewModel.state.value.locationsLoading)
    }

    @Test
    fun togglingAnOfficeOffRoundTripsThroughTheIdField() = runTest {
        val mitte = viewModel.state.value.locationOptions.single { it.id == 122217 }

        viewModel.dispatch(EditIntent.LocationToggled(mitte))
        assertEquals("122210", viewModel.state.value.locations)

        viewModel.dispatch(EditIntent.Save)

        assertEquals(EditMessage.Done, viewModel.messages.first())
        assertEquals(listOf(MonitorTask(1, 120686, listOf(122210), 4)), tasks.tasks.value)
    }

    @Test
    fun anUnreachableOfficeListStillLetsTheTaskBeSaved() = runTest {
        val offline = EditViewModel(
            1,
            EditEffectHandler(tasks, scheduler, FakeLocationCatalog(failure = IllegalStateException("offline"))),
            dispatcher,
        )

        assertTrue(offline.state.value.locationsFailed)
        assertTrue(offline.state.value.loaded)

        offline.dispatch(EditIntent.LocationsChanged("122210"))
        offline.dispatch(EditIntent.Save)

        assertEquals(EditMessage.Done, offline.messages.first())
        assertEquals(listOf(122210), tasks.tasks.value.single().locations)
    }

    @Test
    fun missingTaskLeavesTheScreen() = runTest {
        val viewModel = EditViewModel(99, EditEffectHandler(tasks, scheduler, offices), dispatcher)

        assertEquals(EditMessage.Done, viewModel.messages.first())
    }
}
