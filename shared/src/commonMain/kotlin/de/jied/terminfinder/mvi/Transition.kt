package de.jied.terminfinder.mvi

data class Transition<S, out E, out M>(
    val state: S,
    val effects: List<E> = emptyList(),
    val messages: List<M> = emptyList(),
)

val <S> S.noEffect: Transition<S, Nothing, Nothing> get() = Transition(this)

fun <S, E> S.perform(vararg effects: E): Transition<S, E, Nothing> =
    Transition(this, effects = effects.toList())

fun <S, M> S.send(vararg messages: M): Transition<S, Nothing, M> =
    Transition(this, messages = messages.toList())
