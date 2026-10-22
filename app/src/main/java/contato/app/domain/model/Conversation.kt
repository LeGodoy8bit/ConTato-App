package contato.app.domain.model

/**
 * Uma conversa com um aparelho (o "par" do Nearby), como no app de mensagens:
 * cada celular com quem você conversou vira uma linha da lista.
 *
 * [id] é gerado uma vez (UUID) e não muda, mesmo se o nome do aparelho mudar depois;
 * é ele que identifica a conversa na nuvem.
 */
data class Conversation(
    val id: String,
    val peerName: String,
    val messages: List<Message>,
    val updatedAt: Long,
    /** Até quando o dono já leu (mensagens recebidas depois disso contam como não lidas). */
    val lastReadAt: Long = 0L,
) {
    val lastMessage: Message? get() = messages.maxByOrNull { it.timestamp }

    /** Mensagens que chegaram do outro aparelho e ainda não foram vistas. */
    fun unreadCount(localMode: UserMode): Int =
        messages.count { it.sender != localMode && it.timestamp > lastReadAt }

    companion object {
        /** Quantas mensagens cada conversa guarda (no aparelho e na nuvem). */
        const val MAX_MESSAGES = 200
    }
}
