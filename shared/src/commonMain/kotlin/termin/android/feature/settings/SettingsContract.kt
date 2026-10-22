package termin.android.feature.settings

data class SettingsState(val notifyNewDates: Boolean = true)

sealed interface SettingsIntent {
    data class NotifyNewDatesChanged(val enabled: Boolean) : SettingsIntent
}
