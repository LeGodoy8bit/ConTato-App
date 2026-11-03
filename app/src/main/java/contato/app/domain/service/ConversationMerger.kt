package contato.app.domain.service

import contato.app.domain.model.Conversation
import contato.app.domain.model.Message
import contato.app.domain.model.MessageStatus
import java.text.Normalizer

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

    /** "Letícia", " leticia " e "LETÍCIA" são o mesmo aparelho. */
    fun peerKey(name: String): String =
        Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .trim()
            .replace(Regex("\\s+"), " ")
            .lowercase()

    /**
     * O outro celular avisou o nome dele e a conversa ativa foi renomeada. Se já existia outra
     * conversa com esse nome (ex.: antes de entrar na conta), junta as duas numa só — senão
     * aparecem duas "Letícia" na lista. Fica o id da conversa mais antiga (a que já está na nuvem).
     *
     * @return a lista nova e o id da conversa ativa depois da junção.
     */
    fun renameActive(list: List<Conversation>, activeId: String, newName: String): Pair<List<Conversation>, String> {
        val active = list.firstOrNull { it.id == activeId } ?: return list to activeId
        val key = peerKey(newName)
        val other = list.firstOrNull { it.id != activeId && peerKey(it.peerName) == key }
        if (other == null) {
            return list.map { if (it.id == activeId) it.copy(peerName = newName) else it } to activeId
        }
        val joined = other.copy(
            peerName = newName,
            messages = mergeMessages(other.messages, active.messages),
            updatedAt = maxOf(other.updatedAt, active.updatedAt),
            lastReadAt = maxOf(other.lastReadAt, active.lastReadAt),
        )
        val rest = list.filter { it.id != activeId && it.id != other.id }
        return (listOf(joined) + rest).sortedByDescending { it.updatedAt } to other.id
    }

    private fun normalize(messages: List<Message>): List<Message> =
        messages.sortedBy { it.timestamp }.takeLast(Conversation.MAX_MESSAGES)

    /** FAILED vale menos que qualquer confirmação: se um lado viu que vibrou, vale o que vibrou. */
    private fun rank(status: MessageStatus): Int = if (status == MessageStatus.FAILED) 0 else status.ordinal + 1
}
