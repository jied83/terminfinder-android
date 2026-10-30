package de.jied.terminfinder.feature

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import de.jied.terminfinder.FakeCatalog
import de.jied.terminfinder.FakeLocationCatalog
import de.jied.terminfinder.FakeScheduler
import de.jied.terminfinder.FakeTasks
import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.domain.MonitorTask
import de.jied.terminfinder.feature.setup.SetupEffectHandler
import de.jied.terminfinder.feature.setup.SetupMessage
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
    private val offices = FakeLocationCatalog()
    private val catalog = FakeCatalog(
        listOf(
            CatalogEntry(120686, "Wohnsitz - Alleinige Wohnung oder Hauptwohnung anmelden"),
            CatalogEntry(120335, "Abmeldung einer Wohnung"),
        ),
    )

    // No debounce: with the unconfined dispatcher the search then runs inside dispatch().
    private val viewModel by lazy { SetupViewModel(SetupEffectHandler(tasks, scheduler, catalog, offices, Duration.ZERO), dispatcher) }

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

        assertEquals(SetupMessage.Done, viewModel.messages.first())
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

        assertEquals(SetupMessage.Done, viewModel.messages.first())
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
        val offline = SetupViewModel(
            SetupEffectHandler(
                tasks,
                scheduler,
                FakeCatalog(failure = IllegalStateException("offline")),
                offices,
                Duration.ZERO,
            ),
            dispatcher,
        )

        offline.dispatch(SetupIntent.ServiceChanged("wohnung"))

        assertTrue(offline.state.value.catalogFailed)
        assertEquals(false, offline.state.value.searching)
    }

    @Test
    fun theOfficeListLoadsOnceTheServiceStepIsPassed() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        assertEquals(0, offices.loads)

        viewModel.dispatch(SetupIntent.Next)

        assertEquals(1, offices.loads)
        assertEquals(listOf(122210, 122217), viewModel.state.value.locationOptions.map { it.id })
        assertEquals(false, viewModel.state.value.locationsLoading)

        viewModel.dispatch(SetupIntent.Next)

        assertEquals(1, offices.loads)
    }

    @Test
    fun togglingAnOfficeEditsTheIdFieldBothWays() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        viewModel.dispatch(SetupIntent.Next)
        val (pankow, mitte) = viewModel.state.value.locationOptions

        viewModel.dispatch(SetupIntent.LocationToggled(pankow))
        assertEquals("122210", viewModel.state.value.locations)

        viewModel.dispatch(SetupIntent.LocationToggled(mitte))
        assertEquals("122210, 122217", viewModel.state.value.locations)
        assertEquals(setOf(122210, 122217), viewModel.state.value.selectedLocations)

        viewModel.dispatch(SetupIntent.LocationToggled(pankow))
        assertEquals("122217", viewModel.state.value.locations)

        viewModel.dispatch(SetupIntent.LocationToggled(mitte))
        assertEquals("", viewModel.state.value.locations)
        assertEquals(emptyList(), viewModel.state.value.locationIds)
    }

    @Test
    fun goingBackToAnotherServiceReplacesItsOffices() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        viewModel.dispatch(SetupIntent.Next)
        assertEquals(listOf(122210, 122217), viewModel.state.value.locationOptions.map { it.id })

        viewModel.dispatch(SetupIntent.ServiceChanged("120335"))
        viewModel.dispatch(SetupIntent.Next)

        assertEquals(2, offices.loads)
        assertEquals(listOf(122231), viewModel.state.value.locationOptions.map { it.id })
        // offices picked for the previous service must not travel to the new one
        assertEquals("", viewModel.state.value.locations)
    }

    @Test
    fun anUnreachableOfficeListLeavesTheFieldUsable() = runTest {
        val offlineOffices = FakeLocationCatalog(failure = IllegalStateException("offline"))
        val offline = SetupViewModel(
            SetupEffectHandler(tasks, scheduler, catalog, offlineOffices, Duration.ZERO),
            dispatcher,
        )

        offline.dispatch(SetupIntent.ServiceChanged("120686"))
        offline.dispatch(SetupIntent.Next)

        assertTrue(offline.state.value.locationsFailed)
        assertEquals(false, offline.state.value.locationsLoading)
        assertEquals(1, offlineOffices.loads)

        offline.dispatch(SetupIntent.LocationsChanged("122210"))
        offline.dispatch(SetupIntent.IntervalChanged("4"))
        offline.dispatch(SetupIntent.Next)
        offline.dispatch(SetupIntent.Submit)

        assertEquals(SetupMessage.Done, offline.messages.first())
        assertEquals(listOf(122210), tasks.tasks.value.single().locations)
        assertEquals(1, offlineOffices.loads)
    }

    @Test
    fun typingANameFiltersTheOfficesAndTickingSeveralKeepsTheFilter() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        viewModel.dispatch(SetupIntent.Next)

        viewModel.dispatch(SetupIntent.LocationsChanged("burgeramt"))
        // both offices share the word; CatalogSearch orders them, so compare without an order
        assertEquals(setOf(122210, 122217), viewModel.state.value.visibleLocations.map { it.id }.toSet())

        viewModel.dispatch(SetupIntent.LocationsChanged("mitte"))
        val mitte = viewModel.state.value.visibleLocations.single()
        assertEquals(122217, mitte.id)

        viewModel.dispatch(SetupIntent.LocationToggled(mitte))

        assertEquals("mitte", viewModel.state.value.locationField)
        assertEquals("122217", viewModel.state.value.locations)
        assertEquals(setOf(122217), viewModel.state.value.selectedLocations)
    }

    @Test
    fun clearingTheFilterShowsTheIdsAndTheWholeListAgain() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        viewModel.dispatch(SetupIntent.Next)
        viewModel.dispatch(SetupIntent.LocationsChanged("122210"))
        viewModel.dispatch(SetupIntent.LocationsChanged("mitte"))

        assertEquals("mitte", viewModel.state.value.locationField)

        viewModel.dispatch(SetupIntent.LocationsChanged(""))

        assertEquals("122210", viewModel.state.value.locationField)
        assertNull(viewModel.state.value.locationQuery)
        assertEquals(listOf(122210, 122217), viewModel.state.value.visibleLocations.map { it.id })
    }

    @Test
    fun enteringIdsByHandNeverSlipsIntoFiltering() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        viewModel.dispatch(SetupIntent.Next)

        // keystroke by keystroke, including the comma that does not parse on its own
        for (typed in listOf("1", "12", "122210", "122210,", "122210, ", "122210, 122217")) {
            viewModel.dispatch(SetupIntent.LocationsChanged(typed))
            assertNull(viewModel.state.value.locationQuery)
            assertEquals(typed, viewModel.state.value.locationField)
        }
        assertEquals(listOf(122210, 122217), viewModel.state.value.locationIds)
    }

    @Test
    fun changingTheServiceDropsTheOfficesItWasShowing() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        viewModel.dispatch(SetupIntent.Next)
        viewModel.dispatch(SetupIntent.LocationToggled(viewModel.state.value.locationOptions.first()))
        viewModel.dispatch(SetupIntent.LocationsChanged("mitte"))
        assertEquals("122210", viewModel.state.value.locations)

        viewModel.dispatch(SetupIntent.ServiceChanged("120335"))

        assertEquals(emptyList(), viewModel.state.value.locationOptions)
        assertEquals("", viewModel.state.value.locations)
        assertNull(viewModel.state.value.locationQuery)
        assertEquals(emptySet(), viewModel.state.value.selectedLocations)
    }

    @Test
    fun clearingTheServiceDropsTheOfficesToo() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        viewModel.dispatch(SetupIntent.Next)
        assertTrue(viewModel.state.value.locationOptions.isNotEmpty())

        viewModel.dispatch(SetupIntent.ServiceChanged(""))

        assertNull(viewModel.state.value.serviceId)
        assertEquals(emptyList(), viewModel.state.value.locationOptions)
        assertEquals("", viewModel.state.value.locations)
    }

    @Test
    fun retypingTheSameServiceKeepsTheOfficesAndDoesNotRefetch() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        viewModel.dispatch(SetupIntent.Next)
        viewModel.dispatch(SetupIntent.LocationToggled(viewModel.state.value.locationOptions.first()))

        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))

        assertEquals(1, offices.loads)
        assertEquals(listOf(122210, 122217), viewModel.state.value.locationOptions.map { it.id })
        assertEquals("122210", viewModel.state.value.locations)
    }

    @Test
    fun pickingAnotherServiceFromTheListAlsoDropsTheOffices() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        viewModel.dispatch(SetupIntent.Next)
        viewModel.dispatch(SetupIntent.LocationToggled(viewModel.state.value.locationOptions.first()))

        viewModel.dispatch(SetupIntent.ServiceChanged("abmeldung"))
        viewModel.dispatch(SetupIntent.ServiceSelected(viewModel.state.value.suggestions.first()))

        assertEquals(120335, viewModel.state.value.serviceId)
        assertEquals(emptyList(), viewModel.state.value.locationOptions)
        assertEquals("", viewModel.state.value.locations)
    }

    private fun fillUpToInterval() {
        viewModel.dispatch(SetupIntent.ServiceChanged("120686"))
        viewModel.dispatch(SetupIntent.Next)
        viewModel.dispatch(SetupIntent.LocationsChanged("122210, 122217"))
        viewModel.dispatch(SetupIntent.Next)
        assertEquals(SetupStep.INTERVAL, viewModel.state.value.step)
    }
}
