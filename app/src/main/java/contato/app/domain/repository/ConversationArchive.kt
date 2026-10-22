package contato.app.domain.repository

import contato.app.domain.model.Conversation

/**
 * Guarda as conversas no próprio aparelho (opcional, controlado pelo usuário).
 * A nuvem fica em [CloudConversationRepository]; as duas são juntadas pelo ConversationMerger.
 */
interface ConversationArchive {
    fun loadAll(): List<Conversation>
    fun saveAll(conversations: List<Conversation>)
    fun clear()
}
