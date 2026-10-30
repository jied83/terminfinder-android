package de.jied.terminfinder.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class MviViewModel<I, S, E, M>(
    initial: S,
    private val handler: EffectHandler<E, I>,
    private val background: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel(), Store<I, S, M> {

    private val mutableState = MutableStateFlow(initial)
    private val messageChannel = Channel<M>(Channel.BUFFERED)
    private val running = mutableMapOf<Any, Job>()

    override val state: StateFlow<S> = mutableState.asStateFlow()
    override val messages: Flow<M> = messageChannel.receiveAsFlow()

    protected fun reduce(transform: S.() -> Transition<S, E, M>) {
        while (true) {
            val current = mutableState.value
            val (next, effects, messages) = current.transform()
            if (mutableState.compareAndSet(current, next)) {
                messages.forEach(::deliver)
                effects.forEach(::perform)
                return
            }
        }
    }

    private fun deliver(message: M) {
        messageChannel.trySend(message)
    }

    protected open fun keyOf(effect: E): Any = (effect as Any)::class

    private fun perform(effect: E) {
        val key = keyOf(effect)
        running[key]?.cancel()
        running[key] = viewModelScope.launch {
            val intent = withContext(background) { handler.handle(effect) }
            if (intent != null) dispatch(intent)
        }
    }
}
