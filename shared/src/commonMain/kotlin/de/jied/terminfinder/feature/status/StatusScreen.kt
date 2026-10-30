package de.jied.terminfinder.feature.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import de.jied.terminfinder.feature.tasks.FAB_CLEARANCE
import de.jied.terminfinder.resources.Res
import de.jied.terminfinder.resources.ic_delete
import de.jied.terminfinder.resources.ic_edit
import de.jied.terminfinder.resources.ic_open_in_new
import de.jied.terminfinder.resources.status_book
import de.jied.terminfinder.resources.status_delete
import de.jied.terminfinder.resources.status_delete_cancel
import de.jied.terminfinder.resources.status_delete_confirm
import de.jied.terminfinder.resources.status_delete_message
import de.jied.terminfinder.resources.status_delete_title
import de.jied.terminfinder.resources.status_edit
import de.jied.terminfinder.ui.BackTopAppBar
import de.jied.terminfinder.ui.cardColors
import de.jied.terminfinder.ui.display
import de.jied.terminfinder.ui.label
import de.jied.terminfinder.ui.taskTitle

@Composable
fun StatusScreen(
    state: StatusState,
    onIntent: (StatusIntent) -> Unit,
    onBack: () -> Unit,
) {
    if (state.confirmingDelete) {
        AlertDialog(
            onDismissRequest = { onIntent(StatusIntent.DismissDelete) },
            title = { Text(stringResource(Res.string.status_delete_title)) },
            text = { Text(stringResource(Res.string.status_delete_message)) },
            confirmButton = {
                TextButton(onClick = { onIntent(StatusIntent.ConfirmDelete) }) {
                    Text(stringResource(Res.string.status_delete_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { onIntent(StatusIntent.DismissDelete) }) {
                    Text(stringResource(Res.string.status_delete_cancel))
                }
            },
        )
    }

    Scaffold(
        topBar = {
            BackTopAppBar(taskTitle(state.serviceId, state.serviceName), onBack) {
                IconButton(onClick = { onIntent(StatusIntent.Edit) }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_edit),
                        contentDescription = stringResource(Res.string.status_edit),
                    )
                }
                IconButton(onClick = { onIntent(StatusIntent.Delete) }) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_delete),
                        contentDescription = stringResource(Res.string.status_delete),
                    )
                }
            }
        },
        floatingActionButton = {
            if (state.canBook) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(Res.string.status_book)) },
                    icon = { Icon(painterResource(Res.drawable.ic_open_in_new), contentDescription = null) },
                    onClick = { onIntent(StatusIntent.Book) },
                )
            }
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = FAB_CLEARANCE),
        ) {
            item {
                Card(
                    colors = state.latest?.status.cardColors(),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(state.latest?.status.label(), style = MaterialTheme.typography.headlineSmall)
                        state.latest?.let { Text(it.at.display(), style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
            items(state.history) { record ->
                ListItem(
                    overlineContent = { Text(record.at.display()) },
                    headlineContent = { Text(record.status.label()) },
                    supportingContent = record.dates.takeIf { it.isNotEmpty() }?.let { { Text(it.joinToString()) } },
                )
            }
        }
    }
}
