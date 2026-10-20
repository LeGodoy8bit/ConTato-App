package contato.app.domain.repository

import contato.app.domain.model.Message

/**
 * Guarda as conversas entre uma sessão e outra (opcional, controlado pelo usuário).
 * A implementação local fica em data/; uma versão em nuvem (Firebase) pode
 * implementar o mesmo contrato depois, sem mudar as telas.
 */
interface ConversationArchive {
    fun load(): List<Message>
    fun save(messages: List<Message>)
    fun clear()
}
