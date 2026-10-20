package contato.app.domain.service

import contato.app.core.config.HapticConfig
import contato.app.domain.model.HapticMessage
import contato.app.domain.model.HapticPattern

class HapticMessageService(
    private val hapticPatternService: HapticPatternService = HapticPatternService()
) {

    /**
     * Converte um texto em HapticMessage.
     *
     * O texto passa antes pelo BrailleTextNormalizer: acentos, "ç", números e
     * pontuação não derrubam mais o app (antes, "Olá" lançava exceção no BrailleMapper).
     */
    fun createMessage(message: String): HapticMessage {

        val normalized = BrailleTextNormalizer.normalizeForBraille(message)

        require(normalized.length <= HapticConfig.MAX_MESSAGE_LENGTH) {
            "Mensagem excede o limite de ${HapticConfig.MAX_MESSAGE_LENGTH} caracteres"
        }

        val messagePattern = normalized.map { char ->
            if (char == ' ') {
                createSpacePattern()
            } else {
                val brailleCharacter = BrailleMapper.map(char)
                hapticPatternService.createPattern(brailleCharacter)
            }
        }

        return HapticMessage(messagePattern)
    }

    private fun createSpacePattern(): HapticPattern {
        return HapticPattern(events = emptyList())
    }
}