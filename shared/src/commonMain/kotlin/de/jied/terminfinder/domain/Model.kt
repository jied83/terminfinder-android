package de.jied.terminfinder.domain

import kotlinx.serialization.Serializable
import terminwatcher.core.api.ServiceQuery
import terminwatcher.core.api.Status
import kotlin.time.Instant

@Serializable
data class MonitorTask(
    val id: Long,
    val serviceId: Int,
    val locations: List<Int>,
    val intervalMinutes: Int,
    val serviceName: String? = null,
) {
    val query: ServiceQuery get() = ServiceQuery(serviceId, locations, serviceName)
}

@Serializable
data class CheckRecord(
    val taskId: Long,
    val at: Instant,
    val status: Status,
    val dates: List<String>,
    val newDates: List<String> = emptyList(),
    val serviceName: String? = null,
)

val Status.isParsed: Boolean get() = this == Status.AVAILABLE || this == Status.NONE

data class TaskDraft(
    val serviceId: Int,
    val locations: List<Int>,
    val intervalMinutes: Int,
    val serviceName: String? = null,
)
