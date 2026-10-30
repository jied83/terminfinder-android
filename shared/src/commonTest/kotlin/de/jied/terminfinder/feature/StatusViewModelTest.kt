package de.jied.terminfinder.feature

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import de.jied.terminfinder.FakeLog
import de.jied.terminfinder.FakeScheduler
import de.jied.terminfinder.FakeTasks
import de.jied.terminfinder.domain.CheckRecord
import de.jied.terminfinder.feature.status.StatusEffectHandler
import de.jied.terminfinder.feature.status.StatusMessage
import de.jied.terminfinder.feature.status.StatusIntent
import de.jied.terminfinder.feature.status.StatusViewModel
import de.jied.terminfinder.task
import terminwatcher.core.api.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class StatusViewModelTest : MainDispatcherTest() {
    private val log = FakeLog()
    private val tasks = FakeTasks(task(1, 2), task(2, 6))
    private val scheduler = FakeScheduler()
    private val viewModel by lazy { StatusViewModel(1, tasks, log, StatusEffectHandler(tasks, scheduler), dispatcher) }

    @Test
    fun bookingOpensTheCalendarWhenLatestHasSlots() = runTest {
        log.append(CheckRecord(1, Instant.fromEpochSeconds(1), Status.NONE, emptyList()))
        log.append(CheckRecord(1, Instant.fromEpochSeconds(2), Status.AVAILABLE, listOf("2026-10-20")))

        assertTrue(viewModel.state.value.canBook)
        assertEquals(2, viewModel.state.value.history.size)
        viewModel.dispatch(StatusIntent.Book)
        assertEquals(
            StatusMessage.OpenUrl("https://service.berlin.de/terminvereinbarung/termin/all/120686/"),
            viewModel.messages.first(),
        )
    }

    @Test
    fun exposesTheServiceNameLearnedByACheck() = runTest {
        assertEquals(null, viewModel.state.value.serviceName)

        tasks.setServiceName(1, "Personalausweis beantragen")

        assertEquals("Personalausweis beantragen", viewModel.state.value.serviceName)
        assertEquals(120686, viewModel.state.value.serviceId)
    }

    @Test
    fun bookingKeepsTheTasksOfficeFilter() = runTest {
        tasks.update(1, listOf(122210, 122217), 2)
        log.append(CheckRecord(1, Instant.fromEpochSeconds(1), Status.AVAILABLE, listOf("2026-10-20")))

        viewModel.dispatch(StatusIntent.Book)

        assertEquals(
            StatusMessage.OpenUrl(
                "https://service.berlin.de/terminvereinbarung/termin/tag.php" +
                    "?termin=1&anliegen[]=120686&dienstleister[]=122210&dienstleister[]=122217",
            ),
            viewModel.messages.first(),
        )
    }

    @Test
    fun noBookingWhenLatestHasNoSlots() {
        log.records.value = listOf(
            CheckRecord(1, Instant.fromEpochSeconds(2), Status.NONE, emptyList()),
            CheckRecord(1, Instant.fromEpochSeconds(1), Status.AVAILABLE, listOf("2026-10-20")),
        )

        assertFalse(viewModel.state.value.canBook)
    }

    @Test
    fun editIntentCarriesTaskId() = runTest {
        viewModel.dispatch(StatusIntent.Edit)

        assertEquals(StatusMessage.ToEdit(1), viewModel.messages.first())
    }

    @Test
    fun deleteAsksForConfirmationFirst() {
        viewModel.dispatch(StatusIntent.Delete)

        assertTrue(viewModel.state.value.confirmingDelete)
        assertEquals(listOf(task(1, 2), task(2, 6)), tasks.tasks.value)
    }

    @Test
    fun dismissingConfirmationKeepsTask() {
        viewModel.dispatch(StatusIntent.Delete)
        viewModel.dispatch(StatusIntent.DismissDelete)
        viewModel.dispatch(StatusIntent.ConfirmDelete)

        assertFalse(viewModel.state.value.confirmingDelete)
        assertEquals(listOf(task(1, 2), task(2, 6)), tasks.tasks.value)
        assertEquals(0, scheduler.calls)
    }

    @Test
    fun confirmedDeleteRemovesTaskAndReschedules() = runTest {
        viewModel.dispatch(StatusIntent.Delete)
        viewModel.dispatch(StatusIntent.ConfirmDelete)

        assertEquals(StatusMessage.Deleted, viewModel.messages.first())
        assertEquals(listOf(task(2, 6)), tasks.tasks.value)
        assertEquals(1, scheduler.calls)
    }
}
