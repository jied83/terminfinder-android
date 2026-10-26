package de.jied.terminfinder.feature.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
import org.jetbrains.compose.resources.stringResource
import de.jied.terminfinder.domain.Ticks
import de.jied.terminfinder.mvi.CollectEffects
import de.jied.terminfinder.mvi.Store
import de.jied.terminfinder.resources.Res
import de.jied.terminfinder.resources.edit_save
import de.jied.terminfinder.resources.edit_title
import de.jied.terminfinder.resources.setup_interval_error
import de.jied.terminfinder.resources.setup_interval_label
import de.jied.terminfinder.ui.BackTopAppBar
import de.jied.terminfinder.ui.FormField
import de.jied.terminfinder.ui.OfficeField
import de.jied.terminfinder.ui.taskTitle

@Composable
fun EditScreen(
    store: Store<EditIntent, EditState, EditEffect>,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val state by store.state.collectAsStateWithLifecycle()
    store.CollectEffects { effect ->
        when (effect) {
            EditEffect.Done -> onDone()
        }
    }

    val title = if (state.loaded) taskTitle(state.serviceId, state.serviceName) else stringResource(Res.string.edit_title)

    Scaffold(topBar = { BackTopAppBar(title, onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OfficeField(
                value = state.locationField,
                entries = state.visibleLocations,
                selected = state.selectedLocations,
                isError = state.error == EditField.LOCATIONS,
                loading = state.locationsLoading,
                failed = state.locationsFailed,
                filtering = state.locationQuery != null,
                onChange = { store.dispatch(EditIntent.LocationsChanged(it)) },
                onToggle = { store.dispatch(EditIntent.LocationToggled(it)) },
            )
            FormField(
                value = state.interval,
                label = stringResource(Res.string.setup_interval_label),
                error = stringResource(Res.string.setup_interval_error, Ticks.MIN_MINUTES),
                isError = state.error == EditField.INTERVAL,
                keyboardType = KeyboardType.Number,
                onChange = { store.dispatch(EditIntent.IntervalChanged(it)) },
            )
            Button(
                onClick = { store.dispatch(EditIntent.Save) },
                enabled = state.loaded && !state.saving,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.edit_save)) }
        }
    }
}
