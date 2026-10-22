package termin.android.feature.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import termin.android.domain.SettingsRepository
import termin.android.mvi.MviViewModel

class SettingsViewModel(
    private val settings: SettingsRepository,
) : MviViewModel<SettingsIntent, SettingsState, Nothing>(SettingsState()) {

    init {
        settings.observe()
            .onEach { current -> reduce { copy(notifyNewDates = current.notifyNewDates) } }
            .launchIn(viewModelScope)
    }

    override fun dispatch(intent: SettingsIntent) = when (intent) {
        is SettingsIntent.NotifyNewDatesChanged -> setNotifyNewDates(intent.enabled)
    }

    private fun setNotifyNewDates(enabled: Boolean) {
        viewModelScope.launch { settings.setNotifyNewDates(enabled) }
    }
}
