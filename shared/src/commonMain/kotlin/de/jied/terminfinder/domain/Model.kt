package de.jied.terminfinder.domain

import kotlinx.datetime.LocalDate
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
    val dates: DateSelection? = null,
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
    val dates: DateSelection? = null,
)

@Serializable
data class DateSelection(
    val days: List<String> = emptyList(),
    val from: String? = null,
    val to: String? = null,
) {
    val isRange: Boolean get() = from != null || to != null

    val isValid: Boolean
        get() = if (isRange) {
            val start = from?.let(::parseDate)
            val end = to?.let(::parseDate)
            days.isEmpty() && start != null && end != null && start <= end
        } else {
            days.isNotEmpty() && days.all { parseDate(it) != null }
        }

    private fun parseDate(iso: String): LocalDate? = runCatching { LocalDate.parse(iso) }.getOrNull()
}
