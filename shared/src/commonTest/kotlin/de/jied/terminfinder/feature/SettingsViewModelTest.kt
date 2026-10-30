package de.jied.terminfinder.feature

import de.jied.terminfinder.FakeSettings
import de.jied.terminfinder.feature.settings.SettingsEffectHandler
import de.jied.terminfinder.feature.settings.SettingsIntent
import de.jied.terminfinder.feature.settings.SettingsViewModel
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SettingsViewModelTest : MainDispatcherTest() {

    private fun viewModelFor(settings: FakeSettings) =
        SettingsViewModel(settings, SettingsEffectHandler(settings), dispatcher)

    @Test
    fun showsTheStoredToggle() = runTest {
        val viewModel = viewModelFor(FakeSettings(notifyNewDates = false))

        assertFalse(viewModel.state.value.notifyNewDates)
    }

    @Test
    fun togglingPersistsAndIsReflectedBack() = runTest {
        val settings = FakeSettings(notifyNewDates = true)
        val viewModel = viewModelFor(settings)

        viewModel.dispatch(SettingsIntent.NotifyNewDatesChanged(false))

        assertFalse(settings.settings.value.notifyNewDates)
        assertFalse(viewModel.state.value.notifyNewDates)

        viewModel.dispatch(SettingsIntent.NotifyNewDatesChanged(true))

        assertTrue(settings.settings.value.notifyNewDates)
        assertEquals(true, viewModel.state.value.notifyNewDates)
    }
}
