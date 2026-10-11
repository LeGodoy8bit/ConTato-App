package contato.app.domain.state

/**
 * Controla as transições válidas do ciclo de uma conversa (envio e recebimento
 * de mensagens), conforme a especificação do TCC. Segue o mesmo princípio da
 * ConnectionStateMachine: nenhum estado inconsistente pode ser alcançado.
 *
 * Regras notáveis:
 * - COMPOSING e PROCESSING podem ser cancelados, voltando a IDLE.
 * - Uma vez em SENDING (transmissão já iniciada), não há retorno direto a
 *   IDLE — só SENT (sucesso) ou ERROR (falha), refletindo que a transmissão
 *   BLE/Nearby não deve ser interrompida no meio.
 * - Uma mensagem pode chegar (RECEIVING) tanto em repouso (IDLE) quanto
 *   enquanto se aguarda resposta (WAITING_RESPONSE).
 */
class ConversationStateMachine(
    initialState: ConversationState = ConversationState.IDLE
) {

    var currentState: ConversationState = initialState
        private set

    private var onStateChanged: ((ConversationState) -> Unit)? = null

    private val validTransitions: Map<ConversationState, Set<ConversationState>> = mapOf(
        ConversationState.IDLE to setOf(
            ConversationState.COMPOSING,
            ConversationState.RECEIVING
        ),
        ConversationState.COMPOSING to setOf(
            ConversationState.PROCESSING,
            ConversationState.IDLE
        ),
        ConversationState.PROCESSING to setOf(
            ConversationState.SENDING,
            ConversationState.COMPOSING,
            ConversationState.ERROR
        ),
        ConversationState.SENDING to setOf(
            ConversationState.SENT,
            ConversationState.ERROR
        ),
        ConversationState.SENT to setOf(
            ConversationState.WAITING_RESPONSE
        ),
        ConversationState.WAITING_RESPONSE to setOf(
            ConversationState.RECEIVING,
            ConversationState.IDLE
        ),
        ConversationState.RECEIVING to setOf(
            ConversationState.RECEIVED
        ),
        ConversationState.RECEIVED to setOf(
            ConversationState.IDLE
        ),
        ConversationState.ERROR to setOf(
            ConversationState.COMPOSING,
            ConversationState.IDLE
        )
    )

    fun setOnStateChangedListener(listener: (ConversationState) -> Unit) {
        onStateChanged = listener
    }

    fun transitionTo(newState: ConversationState): Boolean {
        val allowedNextStates = validTransitions[currentState] ?: emptySet()

        if (newState !in allowedNextStates) {
            return false
        }

        currentState = newState
        onStateChanged?.invoke(newState)
        return true
    }
}