package termin.android.ui

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import termin.android.resources.Res
import termin.android.resources.task_title
import kotlin.time.Instant

private val timeFormat = LocalDateTime.Format {
    day()
    char('.')
    monthNumber()
    char(' ')
    hour()
    char(':')
    minute()
}

fun Instant.display(): String = timeFormat.format(toLocalDateTime(TimeZone.currentSystemDefault()))

@Composable
fun taskTitle(serviceId: Int): String = stringResource(Res.string.task_title, serviceId)
