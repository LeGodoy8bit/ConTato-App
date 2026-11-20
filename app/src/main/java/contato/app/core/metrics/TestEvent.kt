package contato.app.core.metrics

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Registro de eventos dos testes técnicos (C4), usado só no build debug.
 *
 * Cada linha diz o que aconteceu (envio, confirmação, letra confirmada, queda da conexão...)
 * e quando. Os dois aparelhos gravam arquivos separados; o script
 * scripts/analisar_eventos.py junta os dois pelo id da mensagem.
 *
 * Em release o app usa [None], que não faz nada.
 */
interface TestEventLog {

    /**
     * @param evento nome curto em maiúsculas (ex.: "ENVIO", "CONEXAO").
     * @param msgId id da mensagem, quando o evento for de uma mensagem.
     * @param detalhe pares chave=valor separados por ";". Se houver texto, "texto=" vem por último.
     */
    fun log(evento: String, msgId: String = "", detalhe: String = "")

    object None : TestEventLog {
        override fun log(evento: String, msgId: String, detalhe: String) = Unit
    }
}

/**
 * Uma linha do arquivo ct_eventos_<papel>.csv. Kotlin puro: testável com JUnit.
 *
 * - epochMs: relógio de parede do aparelho (para achar o trecho pela hora anotada).
 * - uptimeMs: SystemClock.elapsedRealtime, que não muda se o relógio for ajustado.
 *   Durações (reconexão, tempo de conexão) usam só esta coluna, sempre do mesmo aparelho.
 */
data class TestEvent(
    val epochMs: Long,
    val uptimeMs: Long,
    val papel: String,
    val evento: String,
    val msgId: String,
    val detalhe: String,
    val aparelho: String,
    val versaoApp: String,
) {
    fun toCsvLine(): String = listOf(
        formatDataHora(epochMs),
        epochMs.toString(),
        uptimeMs.toString(),
        papel,
        csvField(evento),
        csvField(msgId),
        csvField(detalhe),
        csvField(aparelho),
        csvField(versaoApp),
    ).joinToString(",")

    companion object {
        const val CSV_HEADER = "data_hora,epoch_ms,uptime_ms,papel,evento,msg_id,detalhe,aparelho,versao_app"

        fun formatDataHora(epochMs: Long): String =
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(epochMs))

        fun csvField(value: String): String =
            if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
                "\"" + value.replace("\"", "\"\"") + "\""
            } else {
                value
            }
    }
}
