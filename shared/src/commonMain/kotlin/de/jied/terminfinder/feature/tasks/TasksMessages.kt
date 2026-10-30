package de.jied.terminfinder.feature.tasks

import de.jied.terminfinder.mvi.MessageController

class TasksMessages(
    private val onOpen: (Long) -> Unit,
    private val onAdd: () -> Unit,
    private val onSettings: () -> Unit,
) : MessageController<TasksMessage> {
    override fun handle(message: TasksMessage) = when (message) {
        is TasksMessage.ToStatus -> onOpen(message.id)
        TasksMessage.ToSetup -> onAdd()
        TasksMessage.ToSettings -> onSettings()
    }
}
