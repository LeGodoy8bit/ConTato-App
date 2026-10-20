package contato.app.domain.model

/** Momento em que uma letra começa a vibrar, contado do início da mensagem. */
data class HapticTimelineStep(
    val index: Int,
    val character: Char,
    val startMs: Long,
)

/**
 * Linha do tempo de uma mensagem vibrada.
 *
 * @param text texto normalizado (o que realmente vibra: A-Z e espaço).
 * @param steps uma entrada por caractere, inclusive espaços.
 * @param totalMs duração total, igual à soma do HapticWaveform da mensagem.
 */
data class HapticTimeline(
    val text: String,
    val steps: List<HapticTimelineStep>,
    val totalMs: Long,
)
