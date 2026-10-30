package de.jied.terminfinder.feature.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import de.jied.terminfinder.resources.Res
import de.jied.terminfinder.resources.ic_add
import de.jied.terminfinder.resources.ic_settings
import de.jied.terminfinder.resources.settings_title
import de.jied.terminfinder.resources.tasks_add
import de.jied.terminfinder.resources.tasks_empty
import de.jied.terminfinder.resources.tasks_title
import de.jied.terminfinder.ui.display
import de.jied.terminfinder.ui.label
import de.jied.terminfinder.ui.taskTitle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    state: TasksState,
    onIntent: (TasksIntent) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.tasks_title)) },
                actions = {
                    IconButton(onClick = { onIntent(TasksIntent.Settings) }) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_settings),
                            contentDescription = stringResource(Res.string.settings_title),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text(stringResource(Res.string.tasks_add)) },
                icon = { Icon(painterResource(Res.drawable.ic_add), contentDescription = null) },
                onClick = { onIntent(TasksIntent.Add) },
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { padding ->
        if (state.items.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(Res.string.tasks_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = FAB_CLEARANCE),
            ) {
                items(state.items, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = {
                            Text(
                                text = taskTitle(item.serviceId, item.serviceName),
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = { Text(item.status.label()) },
                        trailingContent = item.checkedAt?.let { { Text(it.display(), style = MaterialTheme.typography.labelMedium) } },
                        modifier = Modifier.clickable { onIntent(TasksIntent.Open(item.id)) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

internal val FAB_CLEARANCE = 88.dp
