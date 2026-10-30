package de.jied.terminfinder.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import de.jied.terminfinder.resources.Res
import de.jied.terminfinder.resources.settings_notify_new_dates
import de.jied.terminfinder.resources.settings_notify_new_dates_hint
import de.jied.terminfinder.resources.settings_title
import de.jied.terminfinder.ui.BackTopAppBar

@Composable
fun SettingsScreen(
    state: SettingsState,
    onIntent: (SettingsIntent) -> Unit,
    onBack: () -> Unit,
) {
    val toggle = { onIntent(SettingsIntent.NotifyNewDatesChanged(!state.notifyNewDates)) }

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
