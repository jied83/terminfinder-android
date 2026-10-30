package de.jied.terminfinder.mvi

fun interface EffectHandler<E, out I> {
    suspend fun handle(effect: E): I?
}
