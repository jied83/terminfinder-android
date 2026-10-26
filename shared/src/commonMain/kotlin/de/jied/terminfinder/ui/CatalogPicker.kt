package de.jied.terminfinder.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.jied.terminfinder.domain.CatalogEntry
import de.jied.terminfinder.resources.Res
import de.jied.terminfinder.resources.setup_service_suggestion_id
import org.jetbrains.compose.resources.stringResource

@Composable
fun CatalogPicker(
    entries: List<CatalogEntry>,
    onPick: (CatalogEntry) -> Unit,
    selected: Set<Int>? = null,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
            items(entries, key = { it.id }) { entry ->
                ListItem(
                    headlineContent = { Text(entry.name) },
                    supportingContent = {
                        Text(stringResource(Res.string.setup_service_suggestion_id, entry.id))
                    },
                    trailingContent = selected?.let { ids ->
                        {
                            Checkbox(
                                checked = entry.id in ids,
                                onCheckedChange = { onPick(entry) },
                            )
                        }
                    },
                    modifier = Modifier.clickable { onPick(entry) },
                )
                HorizontalDivider()
            }
        }
    }
}
