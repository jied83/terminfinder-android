package termin.android.feature

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import termin.android.FakeLog
import termin.android.FakeTasks
import termin.android.domain.CheckRecord
import termin.android.feature.status.StatusEffect
import termin.android.feature.status.StatusIntent
import termin.android.feature.status.StatusViewModel
import termin.android.task
import terminwatcher.core.api.Status
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class StatusViewModelTest : MainDispatcherTest() {
    private val log = FakeLog()
    private val viewModel by lazy { StatusViewModel(1, FakeTasks(task(1, 2)), log) }

    @Test
    fun bookingOpensServicePageWhenLatestHasSlots() = runTest {
        log.append(CheckRecord(1, Instant.fromEpochSeconds(1), Status.NONE, emptyList()))
        log.append(CheckRecord(1, Instant.fromEpochSeconds(2), Status.AVAILABLE, listOf("2026-10-20")))

        assertTrue(viewModel.state.value.canBook)
        assertEquals(2, viewModel.state.value.history.size)
        viewModel.dispatch(StatusIntent.Book)
        assertEquals(StatusEffect.OpenUrl("https://service.berlin.de/dienstleistung/120686/"), viewModel.effects.first())
    }

    @Test
    fun noBookingWhenLatestHasNoSlots() {
        log.records.value = listOf(
            CheckRecord(1, Instant.fromEpochSeconds(2), Status.NONE, emptyList()),
            CheckRecord(1, Instant.fromEpochSeconds(1), Status.AVAILABLE, listOf("2026-10-20")),
        )

        assertFalse(viewModel.state.value.canBook)
    }
}
