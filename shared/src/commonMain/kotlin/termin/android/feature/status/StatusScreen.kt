package termin.android.feature.status

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import termin.android.feature.tasks.FAB_CLEARANCE
import termin.android.mvi.CollectEffects
import termin.android.mvi.Store
import termin.android.resources.Res
import termin.android.resources.ic_open_in_new
import termin.android.resources.status_book
import termin.android.ui.BackTopAppBar
import termin.android.ui.cardColors
import termin.android.ui.display
import termin.android.ui.label
import termin.android.ui.taskTitle

@Composable
fun StatusScreen(
    store: Store<StatusIntent, StatusState, StatusEffect>,
    onBack: () -> Unit,
) {
    val state by store.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    store.CollectEffects { effect ->
        when (effect) {
            is StatusEffect.OpenUrl -> uriHandler.openUri(effect.url)
        }
    }

    Scaffold(
        topBar = { BackTopAppBar(taskTitle(state.serviceId), onBack) },
        floatingActionButton = {
            if (state.canBook) {
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(Res.string.status_book)) },
                    icon = { Icon(painterResource(Res.drawable.ic_open_in_new), contentDescription = null) },
                    onClick = { store.dispatch(StatusIntent.Book) },
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
