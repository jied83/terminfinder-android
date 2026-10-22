package termin.android.feature.status

import termin.android.domain.CheckRecord
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
}

sealed interface StatusEffect {
    data class OpenUrl(val url: String) : StatusEffect
    data class ToEdit(val id: Long) : StatusEffect
    data object Deleted : StatusEffect
}
