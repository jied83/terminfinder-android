package termin.android.feature

import kotlinx.coroutines.test.runTest
import termin.android.FakeSettings
import termin.android.feature.settings.SettingsIntent
import termin.android.feature.settings.SettingsViewModel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest : MainDispatcherTest() {

    @Test
    fun showsTheStoredToggle() = runTest {
        val viewModel = SettingsViewModel(FakeSettings(notifyNewDates = false))

        assertFalse(viewModel.state.value.notifyNewDates)
    }

    @Test
    fun togglingPersistsAndIsReflectedBack() = runTest {
        val settings = FakeSettings(notifyNewDates = true)
        val viewModel = SettingsViewModel(settings)

        viewModel.dispatch(SettingsIntent.NotifyNewDatesChanged(false))

        assertFalse(settings.settings.value.notifyNewDates)
        assertFalse(viewModel.state.value.notifyNewDates)

        viewModel.dispatch(SettingsIntent.NotifyNewDatesChanged(true))

        assertTrue(settings.settings.value.notifyNewDates)
        assertEquals(true, viewModel.state.value.notifyNewDates)
    }
}
