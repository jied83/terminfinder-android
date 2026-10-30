package de.jied.terminfinder.mvi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Store<I, S, M> {
    val state: StateFlow<S>
    val messages: Flow<M>
    fun dispatch(intent: I)
}
