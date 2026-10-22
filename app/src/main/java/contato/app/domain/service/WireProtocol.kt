package contato.app.domain.service

import contato.app.domain.model.WireMessage

/**
 * Codifica e decodifica [WireMessage] em texto simples, para enviar como bytes UTF-8.
 *
 * Formato: CT1 <US> tipo <US> id <US> conteúdo
 * <US> é o caractere "separador de unidade" (U+001F): não aparece em texto digitado.
 *
 * Compatibilidade: qualquer texto que não siga o formato (ex.: a tela de debug,
 * que manda texto puro) é tratado como uma mensagem de texto sem id.
 *
 * Kotlin puro: testável com JUnit.
 */
object WireProtocol {

    private const val VERSION = "CT1"
    private const val SEPARATOR = '\u001F'

    private const val TYPE_TEXT = "T"
    private const val TYPE_RECEIVED = "R"
    private const val TYPE_PROGRESS = "P"
    private const val TYPE_PLAYED = "D"
    private const val TYPE_PROFILE = "N"

    fun encode(message: WireMessage): String = when (message) {
        is WireMessage.Text -> join(TYPE_TEXT, message.id, message.text)
        is WireMessage.Received -> join(TYPE_RECEIVED, message.id, "")
        is WireMessage.Progress -> join(TYPE_PROGRESS, message.id, message.index.toString())
        is WireMessage.Played -> join(TYPE_PLAYED, message.id, "")
        is WireMessage.Profile -> join(TYPE_PROFILE, "", message.name.replace(SEPARATOR, ' ').take(64))
    }

    /** Devolve null só para mensagens no formato novo, mas com tipo desconhecido ou corrompidas. */
    fun decode(raw: String): WireMessage? {
        val parts = raw.split(SEPARATOR, limit = 4)
        if (parts.size != 4 || parts[0] != VERSION) {
            return WireMessage.Text(id = "", text = raw)
        }
        val type = parts[1]
        val id = parts[2]
        val payload = parts[3]
        return when (type) {
            TYPE_TEXT -> WireMessage.Text(id, payload)
            TYPE_RECEIVED -> WireMessage.Received(id)
            TYPE_PROGRESS -> payload.toIntOrNull()?.let { WireMessage.Progress(id, it) }
            TYPE_PLAYED -> WireMessage.Played(id)
            TYPE_PROFILE -> payload.trim().takeIf { it.isNotEmpty() }?.let { WireMessage.Profile(it) }
            else -> null
        }
    }

    private fun join(type: String, id: String, payload: String): String =
        listOf(VERSION, type, id, payload).joinToString(SEPARATOR.toString())
}
