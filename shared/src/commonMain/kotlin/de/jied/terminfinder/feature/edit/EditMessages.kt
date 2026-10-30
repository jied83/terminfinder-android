package de.jied.terminfinder.feature.edit

import de.jied.terminfinder.mvi.MessageController

class EditMessages(
    private val onDone: () -> Unit,
) : MessageController<EditMessage> {
    override fun handle(message: EditMessage) = when (message) {
        EditMessage.Done -> onDone()
    }
}
