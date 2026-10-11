package contato.app.domain.model

data class HapticPulse(

    /** Um único pulso de vibração de sistema (não confundir com HapticEvent, que representa pontos Braille). */
    val durationMs: Long,
    val amplitude: Int

)
