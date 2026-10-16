package termin.android.ui

import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import termin.android.resources.Res
import termin.android.resources.status_available
import termin.android.resources.status_blocked
import termin.android.resources.status_never
import termin.android.resources.status_none
import termin.android.resources.status_unknown
import terminwatcher.core.api.Status

@Composable
fun Status?.label(): String = stringResource(
    when (this) {
        Status.AVAILABLE -> Res.string.status_available
        Status.NONE -> Res.string.status_none
        Status.BLOCKED -> Res.string.status_blocked
        Status.UNKNOWN -> Res.string.status_unknown
        null -> Res.string.status_never
    },
)

@Composable
fun Status?.cardColors(): CardColors {
    val scheme = MaterialTheme.colorScheme
    return when (this) {
        Status.AVAILABLE -> CardDefaults.cardColors(scheme.primaryContainer, scheme.onPrimaryContainer)
        Status.BLOCKED -> CardDefaults.cardColors(scheme.errorContainer, scheme.onErrorContainer)
        else -> CardDefaults.cardColors(scheme.surfaceContainerHighest, scheme.onSurfaceVariant)
    }
}
