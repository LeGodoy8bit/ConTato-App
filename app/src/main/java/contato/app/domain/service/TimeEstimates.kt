package contato.app.domain.service

import kotlin.math.roundToLong

/**
 * Tempos estimados mostrados ao Comunicador (Kotlin puro, testável):
 *  - quanto tempo a pessoa surdocega leva para SENTIR a mensagem (pela mesma conta da vibração);
 *  - quanto costuma levar a RESPOSTA (aprendido com as respostas anteriores da conversa).
 */
object TimeEstimates {

    /** Sem histórico ainda: uma resposta curta montada na cela leva por volta de 1 minuto. */
    const val DEFAULT_REPLY_MS = 60_000L
    private const val MIN_REPLY_MS = 15_000L
    private const val MAX_REPLY_MS = 5 * 60_000L

    private val builder = HapticTimelineBuilder()

    /** A conta da vibração recusa texto acima do limite: aí não estimamos (null). */
    private fun timeline(text: String) = runCatching { builder.buildTimeline(text) }.getOrNull()

    /** Duração da vibração da mensagem inteira, na velocidade padrão. */
    fun feelMs(text: String): Long =
        if (BrailleTextNormalizer.normalizeForBraille(text).isEmpty()) 0L else timeline(text)?.totalMs ?: 0L

    /** Quanto falta a partir da letra [index], já corrigido pela velocidade real ([scale]). */
    fun remainingFeelMs(text: String, index: Int, scale: Float): Long {
        val t = timeline(text) ?: return 0L
        val start = t.steps.getOrNull(index)?.startMs ?: return 0L
        return ((t.totalMs - start) * scale).roundToLong().coerceAtLeast(0L)
    }

    /**
     * Velocidade real da vibração no outro celular (a pessoa pode ter deixado mais lenta nos ajustes):
     * tempo que passou de verdade ÷ tempo previsto entre duas letras. null se ainda é cedo para medir.
     */
    fun measureScale(text: String, fromIndex: Int, toIndex: Int, elapsedMs: Long): Float? {
        val steps = timeline(text)?.steps ?: return null
        val a = steps.getOrNull(fromIndex)?.startMs ?: return null
        val b = steps.getOrNull(toIndex)?.startMs ?: return null
        val expected = b - a
        if (expected < 1_500L || elapsedMs <= 0L) return null
        return (elapsedMs.toFloat() / expected).coerceIn(0.3f, 4f)
    }

    /** Mediana das últimas respostas (até 5), dentro de limites razoáveis. */
    fun typicalReplyMs(samples: List<Long>): Long {
        val recent = samples.takeLast(5).sorted()
        if (recent.isEmpty()) return DEFAULT_REPLY_MS
        val median = if (recent.size % 2 == 1) recent[recent.size / 2]
        else (recent[recent.size / 2 - 1] + recent[recent.size / 2]) / 2
        return median.coerceIn(MIN_REPLY_MS, MAX_REPLY_MS)
    }

    /** "~12 s", "~1 min", "~2 min e meio" → arredondado para ficar fácil de ler. */
    fun friendly(ms: Long): String {
        val s = (ms / 1000.0).roundToLong().coerceAtLeast(1)
        return when {
            s < 60 -> "~${((s + 4) / 5 * 5).coerceAtLeast(5)} s"
            s < 90 -> "~1 min"
            else -> {
                val halfMinutes = (s + 15) / 30
                val min = halfMinutes / 2
                if (halfMinutes % 2 == 0L) "~$min min" else "~$min min e meio"
            }
        }
    }
}
