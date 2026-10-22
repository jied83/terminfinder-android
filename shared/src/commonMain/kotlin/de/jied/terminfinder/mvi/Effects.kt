package de.jied.terminfinder.mvi

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState

@Composable
fun <E> Store<*, *, E>.CollectEffects(handler: (E) -> Unit) {
    val currentHandler = rememberUpdatedState(handler)
    LaunchedEffect(this) {
        effects.collect { currentHandler.value(it) }
    }
}
