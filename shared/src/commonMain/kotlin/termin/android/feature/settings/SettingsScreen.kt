package termin.android.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import termin.android.mvi.Store
import termin.android.resources.Res
import termin.android.resources.settings_notify_new_dates
import termin.android.resources.settings_notify_new_dates_hint
import termin.android.resources.settings_title
import termin.android.ui.BackTopAppBar

@Composable
fun SettingsScreen(
    store: Store<SettingsIntent, SettingsState, Nothing>,
    onBack: () -> Unit,
) {
    val state by store.state.collectAsStateWithLifecycle()
    val toggle = { store.dispatch(SettingsIntent.NotifyNewDatesChanged(!state.notifyNewDates)) }

    Scaffold(topBar = { BackTopAppBar(stringResource(Res.string.settings_title), onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                headlineContent = { Text(stringResource(Res.string.settings_notify_new_dates)) },
                supportingContent = { Text(stringResource(Res.string.settings_notify_new_dates_hint)) },
                trailingContent = {
                    Switch(checked = state.notifyNewDates, onCheckedChange = { toggle() })
                },
                modifier = Modifier.clickable { toggle() },
            )
        }
    }
}
