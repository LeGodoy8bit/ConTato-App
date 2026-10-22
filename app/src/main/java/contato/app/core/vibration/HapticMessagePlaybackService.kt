package contato.app.core.vibration

import android.content.Context
import contato.app.domain.model.HapticPulse
import contato.app.domain.model.HapticTuning
import contato.app.domain.service.HapticMessageService

/**
 * Ponto único de entrada para transformar um texto em vibração.
 *
 * Esconde a coordenação entre HapticMessageService (domínio: texto -> HapticMessage)
 * e HapticController (infraestrutura: HapticMessage -> vibração real), para que
 * nenhuma tela precise repetir essa coordenação.
 */
class HapticMessagePlaybackService(context: Context) {

    private val hapticMessageService = HapticMessageService()
    private val hapticController = HapticController(context)

    /** Força e ritmo das letras (Ajustes do Surdocego). */
    var tuning: HapticTuning
        get() = hapticController.tuning
        set(value) { hapticController.tuning = value }

    /** Repassa um padrão de vibração de sistema para o HapticController. */
    fun playSystemFeedback(pulses: List<HapticPulse>) {
        hapticController.playSystemFeedback(pulses)
    }

    /**
     * Vibra o texto em Braille.
     * @return duração total em milissegundos (quem não precisa pode ignorar).
     */
    fun play(text: String): Long {
        val message = hapticMessageService.createMessage(text)
        return hapticController.playMessage(message)
    }

    fun stop() {
        hapticController.stop()
    }
}
