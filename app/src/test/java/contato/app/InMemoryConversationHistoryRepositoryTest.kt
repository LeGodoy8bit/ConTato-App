package contato.app

import contato.app.data.InMemoryConversationHistoryRepository
import contato.app.domain.model.Message
import contato.app.domain.model.MessageStatus
import contato.app.domain.model.UserMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryConversationHistoryRepositoryTest {

    private fun criarMensagem(id: String, texto: String) = Message(
        id = id,
        content = texto,
        sender = UserMode.COMMUNICATOR,
        timestamp = System.currentTimeMillis(),
        status = MessageStatus.SENDING
    )

    @Test
    fun `deve comecar sem nenhuma mensagem`() {
        val repository = InMemoryConversationHistoryRepository()
        assertTrue(repository.getMessages().isEmpty())
    }

    @Test
    fun `deve adicionar mensagem e retornar na lista`() {
        val repository = InMemoryConversationHistoryRepository()
        val mensagem = criarMensagem("1", "Oi")

        repository.addMessage(mensagem)

        assertEquals(1, repository.getMessages().size)
        assertEquals("Oi", repository.getMessages()[0].content)
    }

    @Test
    fun `deve manter ordem de insercao das mensagens`() {
        val repository = InMemoryConversationHistoryRepository()

        repository.addMessage(criarMensagem("1", "Primeira"))
        repository.addMessage(criarMensagem("2", "Segunda"))

        val mensagens = repository.getMessages()
        assertEquals("Primeira", mensagens[0].content)
        assertEquals("Segunda", mensagens[1].content)
    }

    @Test
    fun `deve notificar listener quando mensagem for adicionada`() {
        val repository = InMemoryConversationHistoryRepository()
        var mensagemRecebida: Message? = null

        repository.setOnMessageAddedListener { mensagem ->
            mensagemRecebida = mensagem
        }

        repository.addMessage(criarMensagem("1", "Teste"))

        assertEquals("Teste", mensagemRecebida?.content)
    }
}