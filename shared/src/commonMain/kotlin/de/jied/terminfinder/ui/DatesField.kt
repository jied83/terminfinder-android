package de.jied.terminfinder.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.jied.terminfinder.domain.DateSelection
import de.jied.terminfinder.resources.Res
import de.jied.terminfinder.resources.dates_add
import de.jied.terminfinder.resources.dates_any
import de.jied.terminfinder.resources.dates_cancel
import de.jied.terminfinder.resources.dates_days
import de.jied.terminfinder.resources.dates_error
import de.jied.terminfinder.resources.dates_label
import de.jied.terminfinder.resources.dates_ok
import de.jied.terminfinder.resources.dates_range
import de.jied.terminfinder.resources.dates_range_value
import de.jied.terminfinder.resources.dates_remove
import de.jied.terminfinder.resources.ic_delete
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import terminwatcher.core.api.BERLIN
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatesField(
    selection: DateSelection?,
    isError: Boolean,
    onChange: (DateSelection?) -> Unit,
) {
    var pickingDay by rememberSaveable { mutableStateOf(false) }
    var pickingRange by rememberSaveable { mutableStateOf(false) }
    val notBefore = remember { FutureDates(Clock.System.now().toLocalDateTime(BERLIN).date) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.dates_label), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = selection == null,
                onClick = { onChange(null) },
                label = { Text(stringResource(Res.string.dates_any)) },
            )
            FilterChip(
                selected = selection != null && !selection.isRange,
                onClick = {
                    if (selection == null || selection.isRange) onChange(DateSelection())
                    pickingDay = true
                },
                label = { Text(stringResource(Res.string.dates_days)) },
            )
            FilterChip(
                selected = selection?.isRange == true,
                onClick = { pickingRange = true },
                label = { Text(stringResource(Res.string.dates_range)) },
            )
        }
        if (selection != null && !selection.isRange) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                selection.days.forEach { day ->
                    val label = displayDate(day)
                    InputChip(
                        selected = false,
                        onClick = { onChange(selection.copy(days = selection.days - day)) },
                        label = { Text(label) },
                        trailingIcon = {
                            Icon(
                                painterResource(Res.drawable.ic_delete),
                                contentDescription = stringResource(Res.string.dates_remove, label),
                                modifier = Modifier.size(InputChipDefaults.IconSize),
                            )
                        },
                    )
                }
                AssistChip(onClick = { pickingDay = true }, label = { Text(stringResource(Res.string.dates_add)) })
            }
        }
        if (selection != null && selection.isRange) {
            AssistChip(
                onClick = { pickingRange = true },
                label = {
                    Text(
                        stringResource(
                            Res.string.dates_range_value,
                            selection.from?.let(::displayDate).orEmpty(),
                            selection.to?.let(::displayDate).orEmpty(),
                        ),
                    )
                },
            )
        }
        if (isError) {
            Text(
                text = stringResource(Res.string.dates_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }

    if (pickingDay) {
        val state = rememberDatePickerState(selectableDates = notBefore)
        DatePickerDialog(
            onDismissRequest = { pickingDay = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        state.selectedDateMillis?.let { millis ->
                            val days = selection?.takeUnless { it.isRange }?.days.orEmpty()
                            onChange(DateSelection(days = (days + isoFromPicker(millis)).distinct().sorted()))
                        }
                        pickingDay = false
                    },
                    enabled = state.selectedDateMillis != null,
                ) { Text(stringResource(Res.string.dates_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingDay = false }) { Text(stringResource(Res.string.dates_cancel)) }
            },
        ) { DatePicker(state = state) }
    }

    if (pickingRange) {
        val state = rememberDateRangePickerState(selectableDates = notBefore)
        DatePickerDialog(
            onDismissRequest = { pickingRange = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = state.selectedStartDateMillis
                        val end = state.selectedEndDateMillis
                        if (start != null && end != null) {
                            onChange(DateSelection(from = isoFromPicker(start), to = isoFromPicker(end)))
                        }
                        pickingRange = false
                    },
                    enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null,
                ) { Text(stringResource(Res.string.dates_ok)) }
            },
            dismissButton = {
                TextButton(onClick = { pickingRange = false }) { Text(stringResource(Res.string.dates_cancel)) }
            },
        ) { DateRangePicker(state = state, modifier = Modifier.weight(1f)) }
    }
}

private fun isoFromPicker(utcMillis: Long): String =
    Instant.fromEpochMilliseconds(utcMillis).toLocalDateTime(TimeZone.UTC).date.toString()

@OptIn(ExperimentalMaterial3Api::class)
private class FutureDates(private val today: LocalDate) : SelectableDates {
    private val todayMillis = today.atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

    override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayMillis

    override fun isSelectableYear(year: Int): Boolean = year >= today.year
}
