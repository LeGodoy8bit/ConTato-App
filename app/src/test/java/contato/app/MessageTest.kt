package contato.app

import contato.app.domain.model.Message
import contato.app.domain.model.MessageStatus
import contato.app.domain.model.MessageType
import contato.app.domain.model.UserMode
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageTest {

    @Test
    fun `deve criar mensagem com valores corretos`() {
        val message = Message(
            id = "1",
            content = "Ola",
            sender = UserMode.COMMUNICATOR,
            timestamp = 123456L,
            status = MessageStatus.SENDING
        )

        assertEquals("1", message.id)
        assertEquals("Ola", message.content)
        assertEquals(UserMode.COMMUNICATOR, message.sender)
        assertEquals(MessageStatus.SENDING, message.status)
        assertEquals(MessageType.TEXT, message.type) // valor padrão
    }
}