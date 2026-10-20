package termin.android.feature.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import termin.android.domain.Ticks
import termin.android.mvi.CollectEffects
import termin.android.mvi.Store
import termin.android.resources.Res
import termin.android.resources.edit_save
import termin.android.resources.edit_title
import termin.android.resources.setup_interval_error
import termin.android.resources.setup_interval_label
import termin.android.resources.setup_locations_error
import termin.android.resources.setup_locations_label
import termin.android.ui.BackTopAppBar
import termin.android.ui.FormField
import termin.android.ui.taskTitle

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

    val title = if (state.loaded) taskTitle(state.serviceId) else stringResource(Res.string.edit_title)

    Scaffold(topBar = { BackTopAppBar(title, onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxWidth().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormField(
                value = state.locations,
                label = stringResource(Res.string.setup_locations_label),
                error = stringResource(Res.string.setup_locations_error),
                isError = state.error == EditField.LOCATIONS,
                keyboardType = KeyboardType.Text,
                onChange = { store.dispatch(EditIntent.LocationsChanged(it)) },
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
