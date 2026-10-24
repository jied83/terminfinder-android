package de.jied.terminfinder.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.jied.terminfinder.domain.Ticks
import org.jetbrains.compose.resources.stringResource
import de.jied.terminfinder.resources.Res
import de.jied.terminfinder.resources.setup_add
import de.jied.terminfinder.resources.setup_interval_error
import de.jied.terminfinder.resources.setup_interval_label
import de.jied.terminfinder.resources.setup_locations_error
import de.jied.terminfinder.resources.setup_locations_label
import de.jied.terminfinder.resources.setup_next
import de.jied.terminfinder.resources.setup_service_catalog_error
import de.jied.terminfinder.resources.setup_service_error
import de.jied.terminfinder.resources.setup_service_label
import de.jied.terminfinder.resources.setup_title
import de.jied.terminfinder.mvi.CollectEffects
import de.jied.terminfinder.mvi.Store
import de.jied.terminfinder.ui.BackTopAppBar
import de.jied.terminfinder.ui.FormField

@Composable
fun SetupScreen(
    store: Store<SetupIntent, SetupState, SetupEffect>,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val state by store.state.collectAsStateWithLifecycle()
    store.CollectEffects { effect ->
        when (effect) {
            SetupEffect.Done -> onDone()
        }
    }

    Scaffold(topBar = { BackTopAppBar(stringResource(Res.string.setup_title), onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormField(
                value = state.service,
                label = stringResource(Res.string.setup_service_label),
                error = stringResource(Res.string.setup_service_error),
                isError = state.error == SetupStep.SERVICE,
                keyboardType = KeyboardType.Text,
                onChange = { store.dispatch(SetupIntent.ServiceChanged(it)) },
                helper = state.serviceName,
            )
            if (state.searching) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            if (state.catalogFailed) {
                Text(
                    text = stringResource(Res.string.setup_service_catalog_error),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (state.suggestions.isNotEmpty()) {
                ServiceSuggestions(state.suggestions) { store.dispatch(SetupIntent.ServiceSelected(it)) }
            }
            if (state.step >= SetupStep.LOCATIONS) {
                FormField(
                    value = state.locations,
                    label = stringResource(Res.string.setup_locations_label),
                    error = stringResource(Res.string.setup_locations_error),
                    isError = state.error == SetupStep.LOCATIONS,
                    keyboardType = KeyboardType.Text,
                    onChange = { store.dispatch(SetupIntent.LocationsChanged(it)) },
                )
            }
            if (state.step >= SetupStep.INTERVAL) {
                FormField(
                    value = state.interval,
                    label = stringResource(Res.string.setup_interval_label),
                    error = stringResource(Res.string.setup_interval_error, Ticks.MIN_MINUTES),
                    isError = state.error == SetupStep.INTERVAL,
                    keyboardType = KeyboardType.Number,
                    onChange = { store.dispatch(SetupIntent.IntervalChanged(it)) },
                )
                Button(
                    onClick = { store.dispatch(SetupIntent.Submit) },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.setup_add)) }
            } else {
                FilledTonalButton(
                    onClick = { store.dispatch(SetupIntent.Next) },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.setup_next)) }
            }
        }
    }
}
