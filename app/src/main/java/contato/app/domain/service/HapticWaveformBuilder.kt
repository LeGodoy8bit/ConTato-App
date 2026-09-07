package contato.app.domain.service

import contato.app.core.config.HapticConfig
import contato.app.domain.model.HapticMessage
import contato.app.domain.model.HapticPattern
import contato.app.domain.model.HapticWaveform

class HapticWaveformBuilder {

    // Monta a receita de vibração para UM pattern (uma letra isolada)
    fun buildCharacterWaveform(pattern: HapticPattern): HapticWaveform {

        val timings = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()

        addPatternEvents(pattern, timings, amplitudes)

        return HapticWaveform(
            timings = timings.toLongArray(),
            amplitudes = amplitudes.toIntArray()
        )
    }

    // Monta a receita de vibração para uma MENSAGEM inteira (várias letras/espaços)
    fun buildMessageWaveform(message: HapticMessage): HapticWaveform {

        val timings = mutableListOf<Long>()
        val amplitudes = mutableListOf<Int>()

        for ((index, pattern) in message.messagePattern.withIndex()) {

            addPatternEvents(pattern, timings, amplitudes)

            val isLastPattern = index == message.messagePattern.lastIndex

            if (!isLastPattern) {
                val currentIsSpace = pattern.events.isEmpty()
                val nextIsSpace = message.messagePattern[index + 1].events.isEmpty()

                val pause = if (currentIsSpace || nextIsSpace) {
                    HapticConfig.SPACE_PAUSE
                } else {
                    HapticConfig.CHARACTER_PAUSE
                }

                timings.add(pause)
                amplitudes.add(0)
            }
        }

        return HapticWaveform(
            timings = timings.toLongArray(),
            amplitudes = amplitudes.toIntArray()
        )
    }

    // Função auxiliar: adiciona os eventos de UM pattern (os pontos da letra) nas listas
    private fun addPatternEvents(
        pattern: HapticPattern,
        timings: MutableList<Long>,
        amplitudes: MutableList<Int>
    ) {
        for (event in pattern.events) {
            timings.add(event.duration)
            amplitudes.add(event.amplitude)

            timings.add(HapticConfig.POINT_PAUSE)
            amplitudes.add(0)
        }
    }
}
