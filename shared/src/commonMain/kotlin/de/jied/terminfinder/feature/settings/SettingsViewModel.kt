package de.jied.terminfinder.feature.settings

import androidx.lifecycle.viewModelScope
import de.jied.terminfinder.domain.SettingsRepository
import de.jied.terminfinder.mvi.EffectHandler
import de.jied.terminfinder.mvi.MviViewModel
import de.jied.terminfinder.mvi.noEffect
import de.jied.terminfinder.mvi.perform
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SettingsViewModel(
    settings: SettingsRepository,
    effects: EffectHandler<SettingsEffect, Nothing>,
    background: CoroutineDispatcher = Dispatchers.Default,
) : MviViewModel<SettingsIntent, SettingsState, SettingsEffect, Nothing>(SettingsState(), effects, background) {

    init {
        settings.observe()
            .onEach { current -> reduce { copy(notifyNewDates = current.notifyNewDates).noEffect } }
            .launchIn(viewModelScope)
    }

    override fun dispatch(intent: SettingsIntent) = when (intent) {
        is SettingsIntent.NotifyNewDatesChanged ->
            reduce { perform(SettingsEffect.SetNotifyNewDates(intent.enabled)) }
    }
}
