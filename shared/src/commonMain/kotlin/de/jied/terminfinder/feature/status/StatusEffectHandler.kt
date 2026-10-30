package de.jied.terminfinder.feature.status

import de.jied.terminfinder.domain.MonitorScheduler
import de.jied.terminfinder.domain.TaskRepository
import de.jied.terminfinder.mvi.EffectHandler

class StatusEffectHandler(
    private val tasks: TaskRepository,
    private val scheduler: MonitorScheduler,
) : EffectHandler<StatusEffect, StatusIntent> {

    override suspend fun handle(effect: StatusEffect): StatusIntent = when (effect) {
        is StatusEffect.Delete -> {
            tasks.remove(effect.taskId)
            scheduler.reschedule()
            StatusIntent.Removed
        }
    }
}
