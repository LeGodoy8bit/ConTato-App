package contato.app.domain.service

import contato.app.domain.model.SystemFeedbackEvent
import contato.app.domain.state.ConnectionState

/**
 * Traduz um [ConnectionState] (estado formal de conexão) no
 * [SystemFeedbackEvent] correspondente, quando aplicável.
 *
 * Existe para que o NearbyConnectionService (infraestrutura) e o
 * ConnectionStateMachine (domínio) não precisem conhecer nada sobre
 * vibração — a tradução para feedback háptico é responsabilidade
 * exclusiva desta classe.
 *
 * Retorna null para estados que não possuem um evento de feedback
 * próprio (ex.: DISCONNECTED, que representa ausência de conexão,
 * não uma ocorrência a ser sentida).
 */
object ConnectionFeedbackMapper {

    /**
     * @param state estado atual da conexão.
     * @return evento de feedback correspondente, ou null se este
     *         estado não deve gerar nenhuma vibração de sistema.
     */
    fun mapConnectionStateToFeedback(state: ConnectionState): SystemFeedbackEvent? {
        return when (state) {
            ConnectionState.SCANNING -> SystemFeedbackEvent.SEARCHING
            ConnectionState.DEVICE_FOUND -> SystemFeedbackEvent.DEVICE_FOUND
            ConnectionState.CONNECTING -> SystemFeedbackEvent.CONNECTING
            ConnectionState.CONNECTED -> SystemFeedbackEvent.CONNECTED
            ConnectionState.CONNECTION_ERROR -> SystemFeedbackEvent.CONNECTION_FAILED
            ConnectionState.DISCONNECTED,
            ConnectionState.RECONNECTING -> null
        }
    }
}