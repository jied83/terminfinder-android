package de.jied.terminfinder.feature.status

import de.jied.terminfinder.mvi.MessageController

class StatusMessages(
    private val onOpenUrl: (String) -> Unit,
    private val onEdit: (Long) -> Unit,
    private val onDeleted: () -> Unit,
) : MessageController<StatusMessage> {
    override fun handle(message: StatusMessage) = when (message) {
        is StatusMessage.OpenUrl -> onOpenUrl(message.url)
        is StatusMessage.ToEdit -> onEdit(message.id)
        StatusMessage.Deleted -> onDeleted()
    }
}
