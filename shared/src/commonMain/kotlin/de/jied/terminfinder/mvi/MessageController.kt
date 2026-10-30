package de.jied.terminfinder.mvi

interface MessageController<M> {
    fun handle(message: M)
}
