package de.jied.terminfinder.feature

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import de.jied.terminfinder.FakeCatalog
import de.jied.terminfinder.FakeScheduler
import de.jied.terminfinder.FakeTasks
import de.jied.terminfinder.domain.MonitorTask
import de.jied.terminfinder.domain.ServiceInfo
import de.jied.terminfinder.feature.setup.SetupEffect
import de.jied.terminfinder.feature.setup.SetupIntent
import de.jied.terminfinder.feature.setup.SetupStep
import de.jied.terminfinder.feature.setup.SetupViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration

class SetupViewModelTest : MainDispatcherTest() {
    private val tasks = FakeTasks()
    private val scheduler = FakeScheduler()
    private val catalog = FakeCatalog(
        listOf(
            ServiceInfo(120686, "Wohnsitz - Alleinige Wohnung oder Hauptwohnung anmelden"),
            ServiceInfo(120335, "Abmeldung einer Wohnung"),
        ),
    )

    // No debounce: with the unconfined dispatcher the search then runs inside dispatch().
    private val viewModel by lazy { SetupViewModel(tasks, scheduler, catalog, Duration.ZERO) }

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

    @Test
    fun textSearchesTheCatalogAndPickingAnEntryFillsInItsId() {
        viewModel.dispatch(SetupIntent.ServiceChanged("wohnung anmelden"))

        val suggestions = viewModel.state.value.suggestions
        assertEquals(listOf(120686), suggestions.map { it.id })
        assertTrue(viewModel.state.value.serviceId == null)

        viewModel.dispatch(SetupIntent.ServiceSelected(suggestions.first()))

        assertEquals("120686", viewModel.state.value.service)
        assertEquals(120686, viewModel.state.value.serviceId)
        assertEquals("Wohnsitz - Alleinige Wohnung oder Hauptwohnung anmelden", viewModel.state.value.serviceName)
        assertEquals(emptyList(), viewModel.state.value.suggestions)
    }

    @Test
    fun thePickedNameTravelsToTheTask() = runTest {
        viewModel.dispatch(SetupIntent.ServiceChanged("abmeldung"))
        viewModel.dispatch(SetupIntent.ServiceSelected(viewModel.state.value.suggestions.first()))
        viewModel.dispatch(SetupIntent.Next)
        viewModel.dispatch(SetupIntent.LocationsChanged(""))
        viewModel.dispatch(SetupIntent.Next)
        viewModel.dispatch(SetupIntent.IntervalChanged("4"))
        viewModel.dispatch(SetupIntent.Submit)

        assertEquals(SetupEffect.Done, viewModel.effects.first())
        assertEquals(
            listOf(MonitorTask(1, 120335, emptyList(), 4, "Abmeldung einer Wohnung")),
            tasks.tasks.value,
        )
    }

    @Test
    fun editingAfterAPickDropsTheNameAgain() {
        viewModel.dispatch(SetupIntent.ServiceChanged("abmeldung"))
        viewModel.dispatch(SetupIntent.ServiceSelected(viewModel.state.value.suggestions.first()))
        viewModel.dispatch(SetupIntent.ServiceChanged("1203"))

        assertNull(viewModel.state.value.serviceName)
        assertEquals(emptyList(), viewModel.state.value.suggestions)
    }

    @Test
    fun aTypedIdIsNamedFromTheCatalogOnlyWhenItWasAlreadyLoaded() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))

        assertEquals(0, catalog.loads)
        assertNull(viewModel.state.value.serviceName)

        viewModel.dispatch(SetupIntent.ServiceChanged("wohnung"))
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))

        assertEquals("Wohnsitz - Alleinige Wohnung oder Hauptwohnung anmelden", viewModel.state.value.serviceName)
    }

    @Test
    fun anUnreachableCatalogIsReported() {
        val offline = SetupViewModel(tasks, scheduler, FakeCatalog(failure = IllegalStateException("offline")), Duration.ZERO)

        offline.dispatch(SetupIntent.ServiceChanged("wohnung"))

        assertTrue(offline.state.value.catalogFailed)
        assertEquals(false, offline.state.value.searching)
    }

    private fun fillUpToInterval() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        viewModel.dispatch(SetupIntent.Next)
        viewModel.dispatch(SetupIntent.LocationsChanged("122210, 122217"))
        viewModel.dispatch(SetupIntent.Next)
        assertEquals(SetupStep.INTERVAL, viewModel.state.value.step)
    }
}
