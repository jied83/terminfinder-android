package de.jied.terminfinder.ui

import androidx.compose.runtime.Composable
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource
import de.jied.terminfinder.resources.Res
import de.jied.terminfinder.resources.task_title
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
fun taskTitle(serviceId: Int, serviceName: String?): String =
    serviceName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.task_title, serviceId)
