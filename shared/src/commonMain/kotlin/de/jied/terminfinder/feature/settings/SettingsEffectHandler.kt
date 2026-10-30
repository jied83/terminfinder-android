package de.jied.terminfinder.feature.settings

import de.jied.terminfinder.domain.SettingsRepository
import de.jied.terminfinder.mvi.EffectHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SettingsEffectHandler(
    private val settings: SettingsRepository,
) : EffectHandler<SettingsEffect, Nothing> {

    override suspend fun handle(effect: SettingsEffect): Nothing? = when (effect) {
        is SettingsEffect.SetNotifyNewDates -> {
            settings.setNotifyNewDates(effect.enabled)
            null
        }
    }
}
