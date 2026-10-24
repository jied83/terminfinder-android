package de.jied.terminfinder.feature.setup

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.jied.terminfinder.domain.ServiceInfo
import de.jied.terminfinder.resources.Res
import de.jied.terminfinder.resources.setup_service_suggestion_id
import org.jetbrains.compose.resources.stringResource

@Composable
fun ServiceSuggestions(services: List<ServiceInfo>, onPick: (ServiceInfo) -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        LazyColumn(modifier = Modifier.heightIn(max = 260.dp)) {
            items(services, key = { it.id }) { service ->
                ListItem(
                    headlineContent = { Text(service.name) },
                    supportingContent = { Text(stringResource(Res.string.setup_service_suggestion_id, service.id)) },
                    modifier = Modifier.clickable { onPick(service) },
                )
                HorizontalDivider()
            }
        }
    }
}
