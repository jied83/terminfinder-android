package de.jied.terminfinder.feature.setup

import de.jied.terminfinder.mvi.MessageController

class SetupMessages(
    private val onDone: () -> Unit,
) : MessageController<SetupMessage> {
    override fun handle(message: SetupMessage) = when (message) {
        SetupMessage.Done -> onDone()
    }
}
