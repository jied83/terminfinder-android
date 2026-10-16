package termin.android.feature.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import termin.android.domain.Ticks
import org.jetbrains.compose.resources.stringResource
import termin.android.resources.Res
import termin.android.resources.setup_add
import termin.android.resources.setup_interval_error
import termin.android.resources.setup_interval_label
import termin.android.resources.setup_locations_error
import termin.android.resources.setup_locations_label
import termin.android.resources.setup_next
import termin.android.resources.setup_service_error
import termin.android.resources.setup_service_label
import termin.android.resources.setup_title
import termin.android.mvi.CollectEffects
import termin.android.mvi.Store
import termin.android.ui.BackTopAppBar

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
            StepField(
                value = state.service,
                label = stringResource(Res.string.setup_service_label),
                error = stringResource(Res.string.setup_service_error),
                isError = state.error == SetupStep.SERVICE,
                keyboardType = KeyboardType.Number,
                onChange = { store.dispatch(SetupIntent.ServiceChanged(it)) },
            )
            if (state.step >= SetupStep.LOCATIONS) {
                StepField(
                    value = state.locations,
                    label = stringResource(Res.string.setup_locations_label),
                    error = stringResource(Res.string.setup_locations_error),
                    isError = state.error == SetupStep.LOCATIONS,
                    keyboardType = KeyboardType.Text,
                    onChange = { store.dispatch(SetupIntent.LocationsChanged(it)) },
                )
            }
            if (state.step >= SetupStep.INTERVAL) {
                StepField(
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

@Composable
private fun StepField(
    value: String,
    label: String,
    error: String,
    isError: Boolean,
    keyboardType: KeyboardType,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        isError = isError,
        supportingText = if (isError) ({ Text(error) }) else null,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth(),
    )
}
