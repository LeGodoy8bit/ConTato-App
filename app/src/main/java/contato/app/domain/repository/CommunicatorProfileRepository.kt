package contato.app.domain.repository

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
}
