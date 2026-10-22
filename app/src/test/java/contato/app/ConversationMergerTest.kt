package contato.app

import contato.app.domain.model.Conversation
import contato.app.domain.model.Message
import contato.app.domain.model.MessageStatus
import contato.app.domain.model.UserMode
import contato.app.domain.service.ConversationMerger
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationMergerTest {

    private fun msg(id: String, t: Long, status: MessageStatus = MessageStatus.PLAYED, sender: UserMode = UserMode.COMMUNICATOR) =
        Message(id = id, content = "OI $id", sender = sender, timestamp = t, status = status)

    @Test
    fun mesmaConversaPorNomeFicaComIdDaNuvem() {
        val local = Conversation("local-1", "Celular tátil (Pixel)", listOf(msg("a", 1), msg("b", 3)), updatedAt = 3)
        val remote = Conversation("cloud-1", "celular tátil (pixel)", listOf(msg("a", 1), msg("c", 2)), updatedAt = 2)
        val merged = ConversationMerger.merge(listOf(local), listOf(remote))
        assertEquals(1, merged.size)
        assertEquals("cloud-1", merged[0].id)
        assertEquals(listOf("a", "c", "b"), merged[0].messages.map { it.id })
        assertEquals(3L, merged[0].updatedAt)
    }

    @Test
    fun statusMaisAvancadoVenceEFalhaPerde() {
        val a = listOf(msg("x", 1, MessageStatus.FAILED))
        val b = listOf(msg("x", 1, MessageStatus.DELIVERED))
        assertEquals(MessageStatus.DELIVERED, ConversationMerger.mergeMessages(a, b).single().status)
    }

    @Test
    fun conversasDiferentesFicamSeparadasEOrdenadas() {
        val c1 = Conversation("1", "A", emptyList(), updatedAt = 10)
        val c2 = Conversation("2", "B", emptyList(), updatedAt = 20)
        assertEquals(listOf("2", "1"), ConversationMerger.merge(listOf(c1), listOf(c2)).map { it.id })
    }

    @Test
    fun naoLidasContamSoAsRecebidasDepoisDaLeitura() {
        val c = Conversation("1", "A", listOf(
            msg("1", 5, sender = UserMode.DEAFBLIND),
            msg("2", 15, sender = UserMode.DEAFBLIND),
            msg("3", 20, sender = UserMode.COMMUNICATOR),
        ), updatedAt = 20, lastReadAt = 10)
        assertEquals(1, c.unreadCount(UserMode.COMMUNICATOR))
    }
}
