package termin.android.feature.status

import termin.android.domain.CheckRecord
import terminwatcher.core.api.Status

data class StatusState(
    val serviceId: Int = 0,
    val bookingUrl: String = "",
    val history: List<CheckRecord> = emptyList(),
) {
    val latest: CheckRecord? get() = history.firstOrNull()
    val canBook: Boolean get() = latest?.status == Status.AVAILABLE
}

sealed interface StatusIntent {
    data object Book : StatusIntent
}

sealed interface StatusEffect {
    data class OpenUrl(val url: String) : StatusEffect
}
