package contato.app.core.metrics

import java.util.Locale

/**
 * Uma medição pronta. Cada aparelho preenche só a sua coluna:
 * o Surdocego grava T1 e o Comunicador grava T2. O script de análise junta os dois
 * arquivos pela dupla (execucao, tamanho_msg).
 */
data class LatencySample(
    /** Número da repetição (1..30) nas mensagens do teste automático; null em mensagens digitadas. */
    val execucao: Int?,
    val tamanho: Int,
    val t1Ms: Double?,
    val t2Ms: Double?,
    val aparelho: String,
    val versaoApp: String,
) {
    fun toCsvLine(): String = listOf(
        execucao?.toString().orEmpty(),
        tamanho.toString(),
        t1Ms?.let(::formatMs).orEmpty(),
        t2Ms?.let(::formatMs).orEmpty(),
        csvField(aparelho),
        csvField(versaoApp),
    ).joinToString(",")

    companion object {
        const val CSV_HEADER = "execucao,tamanho_msg,T1_ms,T2_ms,aparelho,versao_app"

        /** Ponto como separador decimal, independentemente do idioma do celular. */
        private fun formatMs(ms: Double) = String.format(Locale.US, "%.3f", ms)

        private fun csvField(value: String): String =
            if (value.any { it == ',' || it == '"' || it == '\n' }) "\"" + value.replace("\"", "\"\"") + "\"" else value
    }
}

/**
 * Liga os instantes de início e fim de cada medição e entrega o resultado ao [sink]
 * (log e CSV, ver DebugLatency). Kotlin puro: testável com JUnit.
 *
 * Tudo é chamado na thread principal (callbacks do Nearby e da sessão), então não há
 * concorrência entre as chamadas.
 */
class LatencyRecorder(
    private val aparelho: String,
    private val versaoApp: String,
    private val sink: (LatencySample) -> Unit,
) : LatencyProbe {

    private data class Pending(val id: String, val length: Int, val atNanos: Long)

    /** Surdocego: só uma mensagem vibra por vez, e a nova substitui a anterior. */
    private var pendingArrival: Pending? = null

    /** Comunicador: mensagens enviadas que ainda esperam a confirmação R. */
    private val pendingSends = HashMap<String, Pending>()

    override fun textArrived(id: String, length: Int, atNanos: Long) {
        pendingArrival = Pending(id, length, atNanos)
    }

    /**
     * Fecha T1 só se houver uma chegada pendente: vibrações que não vêm de uma
     * mensagem recebida (repetir, treino, teste de vibração) são ignoradas.
     */
    override fun beforeVibrate(atNanos: Long) {
        val arrival = pendingArrival ?: return
        pendingArrival = null
        sink(sampleOf(arrival, t1Ms = toMs(atNanos - arrival.atNanos), t2Ms = null))
    }

    override fun textSent(id: String, length: Int, atNanos: Long) {
        pendingSends[id] = Pending(id, length, atNanos)
    }

    /** Uma confirmação repetida ou de mensagem desconhecida é ignorada. */
    override fun receiptArrived(id: String, atNanos: Long) {
        val sent = pendingSends.remove(id) ?: return
        sink(sampleOf(sent, t1Ms = null, t2Ms = toMs(atNanos - sent.atNanos)))
    }

    private fun sampleOf(pending: Pending, t1Ms: Double?, t2Ms: Double?) = LatencySample(
        execucao = LatencyTestPlan.parseExecucao(pending.id),
        tamanho = pending.length,
        t1Ms = t1Ms,
        t2Ms = t2Ms,
        aparelho = aparelho,
        versaoApp = versaoApp,
    )

    private fun toMs(nanos: Long): Double = nanos / 1_000_000.0
}
