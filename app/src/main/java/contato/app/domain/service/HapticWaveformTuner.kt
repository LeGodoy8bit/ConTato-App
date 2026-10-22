package contato.app.domain.service

import contato.app.domain.model.HapticTuning
import contato.app.domain.model.HapticWaveform
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Aplica as preferências de vibração (força e ritmo) numa receita já montada.
 *
 * Fica fora do HapticWaveformBuilder de propósito: o Braille continua igual para todo mundo
 * e o ajuste é só uma "camada" por cima. Pausas (amplitude 0) continuam pausas, e um ponto
 * fraco nunca vira 0 (senão o ponto liso sumiria com a intensidade baixa).
 */
object HapticWaveformTuner {

    fun tune(waveform: HapticWaveform, tuning: HapticTuning): HapticWaveform {
        if (tuning == HapticTuning.DEFAULT) return waveform
        return HapticWaveform(
            timings = LongArray(waveform.timings.size) { i -> scaleTime(waveform.timings[i], tuning) },
            amplitudes = IntArray(waveform.amplitudes.size) { i ->
                val a = waveform.amplitudes[i]
                if (a == 0) 0 else (a * tuning.intensity).roundToInt().coerceIn(1, 255)
            },
        )
    }

    /** Mesma conta usada nos tempos de cada letra (progresso enviado ao Comunicador). */
    fun scaleTime(ms: Long, tuning: HapticTuning): Long =
        if (ms <= 0L) ms else (ms * tuning.timeScale).roundToLong().coerceAtLeast(1L)
}
