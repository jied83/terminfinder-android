package de.jied.terminfinder.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun <M> Store<*, *, M>.CollectMessages(controller: MessageController<M>) {
    val current = rememberUpdatedState(controller)
    LaunchedEffect(this) {
        messages.collect { current.value.handle(it) }
    }
}
