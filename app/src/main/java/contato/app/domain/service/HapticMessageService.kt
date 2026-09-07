package contato.app.domain.service

import contato.app.core.config.HapticConfig
import contato.app.domain.model.HapticEvent
import contato.app.domain.model.HapticEventType
import contato.app.domain.model.HapticMessage
import contato.app.domain.model.HapticPattern

class HapticMessageService(
    private val hapticPatternService: HapticPatternService = HapticPatternService()
) {

    fun createMessage(message: String): HapticMessage {

        require(message.length <= HapticConfig.MAX_MESSAGE_LENGTH) {
            "Mensagem excede o limite de ${HapticConfig.MAX_MESSAGE_LENGTH} caracteres"
        }

        val messagePattern = message.uppercase().map { char -> //adicionado logica para reconhecer espaço
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

