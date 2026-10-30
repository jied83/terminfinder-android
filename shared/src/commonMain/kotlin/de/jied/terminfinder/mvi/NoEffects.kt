package de.jied.terminfinder.mvi

object NoEffects : EffectHandler<Nothing, Nothing> {
    override suspend fun handle(effect: Nothing): Nothing = effect
}
