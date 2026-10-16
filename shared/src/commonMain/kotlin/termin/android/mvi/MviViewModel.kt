package termin.android.mvi

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

abstract class MviViewModel<I, S, E>(initial: S) : ViewModel(), Store<I, S, E> {
    private val mutableState = MutableStateFlow(initial)
    private val effectChannel = Channel<E>(Channel.BUFFERED)

    override val state: StateFlow<S> = mutableState.asStateFlow()
    override val effects: Flow<E> = effectChannel.receiveAsFlow()

    protected fun reduce(transform: S.() -> S) = mutableState.update(transform)

    protected fun emit(effect: E) {
        effectChannel.trySend(effect)
    }
}
