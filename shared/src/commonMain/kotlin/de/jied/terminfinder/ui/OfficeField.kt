package de.jied.terminfinder.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.resources.Res
import de.jied.terminfinder.resources.setup_locations_catalog_error
import de.jied.terminfinder.resources.setup_locations_error
import de.jied.terminfinder.resources.setup_locations_label
import de.jied.terminfinder.resources.setup_locations_no_match
import de.jied.terminfinder.resources.setup_locations_picker_label
import de.jied.terminfinder.resources.setup_locations_selected
import de.jied.terminfinder.resources.setup_locations_selected_all
import org.jetbrains.compose.resources.stringResource


@Composable
fun OfficeField(
    value: String,
    entries: List<CatalogEntry>,
    selected: Set<Int>,
    isError: Boolean,
    loading: Boolean,
    failed: Boolean,
    filtering: Boolean,
    onChange: (String) -> Unit,
    onToggle: (CatalogEntry) -> Unit,
) {
    val selectionSummary = when {
        !filtering -> null
        selected.isEmpty() -> stringResource(Res.string.setup_locations_selected_all)
        else -> stringResource(Res.string.setup_locations_selected, selected.joinToString(", "))
    }

    FormField(
        value = value,
        label = stringResource(Res.string.setup_locations_label),
        error = stringResource(Res.string.setup_locations_error),
        isError = isError,
        keyboardType = KeyboardType.Text,
        onChange = onChange,
        helper = selectionSummary,
    )
    if (loading) {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
    }
    if (failed) {
        Text(
            text = stringResource(Res.string.setup_locations_catalog_error),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
    }
    if (entries.isNotEmpty()) {
        Text(
            text = stringResource(Res.string.setup_locations_picker_label),
            style = MaterialTheme.typography.labelLarge,
        )
        CatalogPicker(entries = entries, onPick = onToggle, selected = selected)
    } else if (filtering && !loading && !failed) {
        Text(
            text = stringResource(Res.string.setup_locations_no_match),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
