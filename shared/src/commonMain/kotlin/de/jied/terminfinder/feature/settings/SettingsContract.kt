package de.jied.terminfinder.feature.settings

data class SettingsState(val notifyNewDates: Boolean = true)

sealed interface SettingsIntent {
    data class NotifyNewDatesChanged(val enabled: Boolean) : SettingsIntent
}

sealed interface SettingsEffect {
    data class SetNotifyNewDates(val enabled: Boolean) : SettingsEffect
}
