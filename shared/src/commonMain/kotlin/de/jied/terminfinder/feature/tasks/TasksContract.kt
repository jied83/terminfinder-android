package de.jied.terminfinder.feature.tasks

import terminwatcher.core.api.Status
import kotlin.time.Instant

data class TasksState(val items: List<TaskItem> = emptyList())

data class TaskItem(
    val id: Long,
    val serviceId: Int,
    val serviceName: String?,
    val status: Status?,
    val checkedAt: Instant?,
)

sealed interface TasksIntent {
    data class Open(val id: Long) : TasksIntent
    data object Add : TasksIntent
    data object Settings : TasksIntent
}

sealed interface TasksEffect {
    data class ToStatus(val id: Long) : TasksEffect
    data object ToSetup : TasksEffect
    data object ToSettings : TasksEffect
}
