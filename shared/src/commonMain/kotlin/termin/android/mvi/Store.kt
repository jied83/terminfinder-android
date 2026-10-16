package termin.android.mvi

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface Store<I, S, E> {
    val state: StateFlow<S>
    val effects: Flow<E>
    fun dispatch(intent: I)
}
