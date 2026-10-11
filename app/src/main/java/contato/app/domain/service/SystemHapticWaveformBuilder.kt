package contato.app.domain.service

import contato.app.core.config.HapticConfig
import contato.app.domain.model.HapticPulse
import contato.app.domain.model.HapticWaveform

/**
 * Converte uma lista de [HapticPulse] (padrão de vibração de sistema)
 * em um [HapticWaveform] (timings/amplitudes prontos para o Vibrator).
 *
 * Equivalente ao HapticWaveformBuilder do pipeline Braille, mas
 * trabalhando com pulsos de sistema em vez de pontos de letra —
 * por isso é uma classe separada, com seu próprio espaçamento
 * (SYSTEM_PULSE_GAP), independente do POINT_PAUSE usado nas letras.
 */
class SystemHapticWaveformBuilder {

    /**
     * Monta a receita de vibração para uma sequência de pulsos de sistema.
     *
     * @param pulses pulsos que compõem o padrão do evento de sistema.
     * @return waveform pronto para ser reproduzido pelo HapticController.
     */
    fun buildSystemWaveform(pulses: List<HapticPulse>): HapticWaveform {

        val timings = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()

        for ((index, pulse) in pulses.withIndex()) {
            timings.add(pulse.durationMs)
            amplitudes.add(pulse.amplitude)

            // Silêncio entre pulsos, exceto após o último,
            // para não sobrar uma pausa "pendurada" no final.
            val isLastPulse = index == pulses.lastIndex
            if (!isLastPulse) {
                timings.add(HapticConfig.SYSTEM_PULSE_GAP)
                amplitudes.add(0)
            }
        }

        return HapticWaveform(
            timings = timings.toLongArray(),
            amplitudes = amplitudes.toIntArray()
        )
    }
}