package de.jied.terminfinder.feature.status

import de.jied.terminfinder.domain.CheckRecord
import terminwatcher.core.api.Status

data class StatusState(
    val serviceId: Int = 0,
    val serviceName: String? = null,
    val bookingUrl: String = "",
    val history: List<CheckRecord> = emptyList(),
    val confirmingDelete: Boolean = false,
) {
    val latest: CheckRecord? get() = history.firstOrNull()
    val canBook: Boolean get() = latest?.status == Status.AVAILABLE
}

sealed interface StatusIntent {
    data object Book : StatusIntent
    data object Edit : StatusIntent
    data object Delete : StatusIntent
    data object ConfirmDelete : StatusIntent
    data object DismissDelete : StatusIntent

    data object Removed : StatusIntent
}

sealed interface StatusEffect {
    data class Delete(val taskId: Long) : StatusEffect
}

sealed interface StatusMessage {
    data class OpenUrl(val url: String) : StatusMessage
    data class ToEdit(val id: Long) : StatusMessage
    data object Deleted : StatusMessage
}
