package contato.app.data

import contato.app.domain.model.Message
import contato.app.domain.repository.ConversationHistoryRepository

/**
 * Implementação simples do histórico de conversa, guardando as
 * mensagens apenas em memória (uma lista comum, dentro do próprio
 * objeto). Isso significa que o histórico se perde quando o app é
 * fechado — decisão consciente de MVP (YAGNI): persistência real
 * (banco de dados local) pode ser adicionada depois, se for pedida,
 * trocando só esta classe, sem mexer em quem a usa.
 */
class InMemoryConversationHistoryRepository : ConversationHistoryRepository {

    // mutableListOf cria uma lista que pode ser alterada (adicionar itens).
    // É "private" porque só esta classe pode modificar a lista diretamente;
    // quem usa de fora só recebe uma cópia somente-leitura via getMessages().
    private val messages = mutableListOf<Message>()

    private var onMessageAdded: ((Message) -> Unit)? = null

    override fun addMessage(message: Message) {
        messages.add(message)
        onMessageAdded?.invoke(message)
    }

    override fun getMessages(): List<Message> {
        // toList() cria uma cópia da lista, para que quem receber não
        // consiga alterar a lista interna diretamente por fora da classe.
        return messages.toList()
    }

    override fun setOnMessageAddedListener(listener: (Message) -> Unit) {
        onMessageAdded = listener
    }
}