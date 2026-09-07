package contato.app.core.vibration

import android.content.Context
import contato.app.domain.service.HapticMessageService

/**
 * Ponto único de entrada para transformar um texto em vibração.
 *
 * Esconde a coordenação entre HapticMessageService (domínio: texto -> HapticMessage)
 * e HapticController (infraestrutura: HapticMessage -> vibração real). Sem esta
 * fachada, cada tela que precisa vibrar uma mensagem (MainActivity, NearbyDebugActivity,
 * e futuramente a tela de recebimento via Nearby) teria que repetir essa coordenação.
 */
class HapticMessagePlaybackService(context: Context) {

    private val hapticMessageService = HapticMessageService()
    private val hapticController = HapticController(context)

    fun play(text: String) {
        val message = hapticMessageService.createMessage(text)
        hapticController.playMessage(message)
    }

    fun stop() {
        hapticController.stop()
    }
}