package de.jied.terminfinder.feature.edit

import de.jied.terminfinder.domain.LocationCatalog
import de.jied.terminfinder.domain.MonitorScheduler
import de.jied.terminfinder.domain.TaskRepository
import de.jied.terminfinder.logDebug
import de.jied.terminfinder.mvi.EffectHandler
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first

class EditEffectHandler(
    private val tasks: TaskRepository,
    private val scheduler: MonitorScheduler,
    private val locations: LocationCatalog,
) : EffectHandler<EditEffect, EditIntent> {

    override suspend fun handle(effect: EditEffect): EditIntent = when (effect) {
        is EditEffect.Load -> {
            val task = tasks.observeAll().first().firstOrNull { it.id == effect.taskId }
            if (task == null) EditIntent.Missing else EditIntent.Loaded(task)
        }
        is EditEffect.LoadOffices -> try {
            EditIntent.OfficesLoaded(locations.load(effect.serviceId))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            logDebug("office list for service ${effect.serviceId} failed: $failure")
            EditIntent.OfficesFailed
        }
        is EditEffect.Save -> {
            tasks.update(effect.taskId, effect.locations, effect.interval)
            scheduler.reschedule()
            EditIntent.Saved
        }
    }
}
