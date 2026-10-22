package contato.app.domain.service

import contato.app.domain.model.Conversation
import contato.app.domain.model.Message
import contato.app.domain.model.MessageStatus

/**
 * Junta as conversas do aparelho com as da nuvem (ao entrar na conta ou abrir o app).
 *
 * Regras:
 *  - mesma conversa = mesmo id OU mesmo nome de aparelho (ignorando maiúsculas);
 *    nesse caso fica o id da nuvem, para não duplicar no Firestore;
 *  - mesma mensagem = mesmo id; fica a versão com o status mais avançado;
 *  - mensagens em ordem de horário, no máximo [Conversation.MAX_MESSAGES] por conversa.
 */
object ConversationMerger {

    fun merge(local: List<Conversation>, remote: List<Conversation>): List<Conversation> {
        val result = LinkedHashMap<String, Conversation>() // chave: nome do aparelho normalizado
        // A nuvem entra primeiro: o id dela vence.
        for (conversation in remote + local) {
            val key = peerKey(conversation.peerName)
            val existing = result[key] ?: result.values.firstOrNull { it.id == conversation.id }
            if (existing != null) result.remove(peerKey(existing.peerName))
            val merged = if (existing == null) {
                conversation.copy(messages = normalize(conversation.messages))
            } else {
                existing.copy(
                    messages = mergeMessages(existing.messages, conversation.messages),
                    updatedAt = maxOf(existing.updatedAt, conversation.updatedAt),
                    lastReadAt = maxOf(existing.lastReadAt, conversation.lastReadAt),
                )
            }
            result[peerKey(merged.peerName)] = merged
        }
        return result.values.sortedByDescending { it.updatedAt }
    }

    fun mergeMessages(a: List<Message>, b: List<Message>): List<Message> =
        normalize((a + b).groupBy { it.id }.values.map { copies -> copies.maxByOrNull { rank(it.status) }!! })

    fun peerKey(name: String): String = name.trim().lowercase()

    private fun normalize(messages: List<Message>): List<Message> =
        messages.sortedBy { it.timestamp }.takeLast(Conversation.MAX_MESSAGES)

    /** FAILED vale menos que qualquer confirmação: se um lado viu que vibrou, vale o que vibrou. */
    private fun rank(status: MessageStatus): Int = if (status == MessageStatus.FAILED) 0 else status.ordinal + 1
}
