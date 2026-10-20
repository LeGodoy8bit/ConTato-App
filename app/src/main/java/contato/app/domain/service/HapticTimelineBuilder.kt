package contato.app.domain.service

import contato.app.core.config.HapticConfig
import contato.app.domain.model.HapticTimeline
import contato.app.domain.model.HapticTimelineStep

/**
 * Calcula em que momento cada letra começa a vibrar.
 *
 * Usa exatamente as mesmas regras do HapticWaveformBuilder (duração de cada ponto
 * + POINT_PAUSE; CHARACTER_PAUSE entre letras; SPACE_PAUSE perto de espaços).
 * É o que permite ao celular do Surdocego avisar o Comunicador "estou na letra 3 de 12"
 * no momento certo, sem medir nada no motor de vibração.
 */
class HapticTimelineBuilder(
    private val hapticMessageService: HapticMessageService = HapticMessageService(),
) {

    fun buildTimeline(rawText: String): HapticTimeline {
        val normalized = BrailleTextNormalizer.normalizeForBraille(rawText)
        val patterns = hapticMessageService.createMessage(rawText).messagePattern

        val steps = mutableListOf<HapticTimelineStep>()
        var elapsed = 0L

        patterns.forEachIndexed { index, pattern ->
            steps += HapticTimelineStep(index, normalized[index], elapsed)
            elapsed += pattern.events.sumOf { it.duration + HapticConfig.POINT_PAUSE }

            val isLast = index == patterns.lastIndex
            if (!isLast) {
                val nextIsSpace = patterns[index + 1].events.isEmpty()
                elapsed += if (pattern.events.isEmpty() || nextIsSpace) {
                    HapticConfig.SPACE_PAUSE
                } else {
                    HapticConfig.CHARACTER_PAUSE
                }
            }
        }

        return HapticTimeline(normalized, steps, elapsed)
    }
}
