package de.jied.terminfinder.feature

import de.jied.terminfinder.feature.status.StatusMessage
import de.jied.terminfinder.feature.status.StatusMessages
import kotlin.test.Test
import kotlin.test.assertEquals

class StatusMessagesTest {

    @Test
    fun routesEachMessageToItsHandler() {
        val calls = mutableListOf<String>()
        val controller = StatusMessages(
            onOpenUrl = { calls += "url:$it" },
            onEdit = { calls += "edit:$it" },
            onDeleted = { calls += "deleted" },
        )

        controller.handle(StatusMessage.OpenUrl("https://service.berlin.de"))
        controller.handle(StatusMessage.ToEdit(7))
        controller.handle(StatusMessage.Deleted)

        assertEquals(listOf("url:https://service.berlin.de", "edit:7", "deleted"), calls)
    }
}
