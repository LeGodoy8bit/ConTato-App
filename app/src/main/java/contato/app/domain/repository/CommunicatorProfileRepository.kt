package contato.app.domain.repository

import contato.app.domain.model.MessageStyle

/** Preferências do Comunicador neste aparelho. */
interface CommunicatorProfileRepository {

    /** Nome mostrado ao outro celular durante a conexão. */
    fun getDisplayName(): String
    fun saveDisplayName(name: String)

    /** Se as conversas devem ficar guardadas neste aparelho. */
    fun isHistoryEnabled(): Boolean
    fun setHistoryEnabled(enabled: Boolean)

    /** Se o app procura o outro celular sozinho (ao abrir e quando a conexão cai). */
    fun isAutoSearchEnabled(): Boolean
    fun setAutoSearchEnabled(enabled: Boolean)

    /** Cor dos balões, tamanho do texto e horários na conversa. */
    fun getMessageStyle(): MessageStyle
    fun saveMessageStyle(style: MessageStyle)

    /** Se a IA sugere uma versão mais curta da mensagem antes de enviar. */
    fun isAiEnabled(): Boolean
    fun setAiEnabled(enabled: Boolean)
}
