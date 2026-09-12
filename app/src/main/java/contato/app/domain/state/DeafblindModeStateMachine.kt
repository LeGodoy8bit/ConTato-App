package contato.app.domain.state

/**
 * Controla o ciclo de estados específico do usuário Surdocego, conforme a
 * especificação do TCC (seções 3.1 a 3.11).
 *
 * O ciclo principal é contínuo: WAITING_MESSAGE -> RECEIVING_MESSAGE ->
 * (repetir ou responder) -> ... -> RETURNING_TO_WAIT -> WAITING_MESSAGE.
 * A perda de conexão pode interromper esse ciclo a qualquer momento
 * operacional, retornando a SEARCHING.
 */
class DeafblindModeStateMachine(
    initialState: DeafblindModeState = DeafblindModeState.INITIALIZING
) {

    var currentState: DeafblindModeState = initialState
        private set

    private var onStateChanged: ((DeafblindModeState) -> Unit)? = null

    private val validTransitions: Map<DeafblindModeState, Set<DeafblindModeState>> = mapOf(
        DeafblindModeState.INITIALIZING to setOf(
            DeafblindModeState.SEARCHING
        ),
        DeafblindModeState.SEARCHING to setOf(
            DeafblindModeState.CONNECTED
        ),
        DeafblindModeState.CONNECTED to setOf(
            DeafblindModeState.WAITING_MESSAGE,
            DeafblindModeState.SEARCHING
        ),
        DeafblindModeState.WAITING_MESSAGE to setOf(
            DeafblindModeState.RECEIVING_MESSAGE,
            DeafblindModeState.SEARCHING
        ),
        DeafblindModeState.RECEIVING_MESSAGE to setOf(
            DeafblindModeState.REPEATING_MESSAGE,
            DeafblindModeState.COMPOSING_BRAILLE,
            DeafblindModeState.WAITING_MESSAGE,
            DeafblindModeState.SEARCHING
        ),
        DeafblindModeState.REPEATING_MESSAGE to setOf(
            DeafblindModeState.WAITING_MESSAGE,
            DeafblindModeState.SEARCHING
        ),
        DeafblindModeState.COMPOSING_BRAILLE to setOf(
            DeafblindModeState.SENDING_MESSAGE,
            DeafblindModeState.WAITING_MESSAGE, // permite cancelar a composição
            DeafblindModeState.SEARCHING
        ),
        DeafblindModeState.SENDING_MESSAGE to setOf(
            DeafblindModeState.RETURNING_TO_WAIT,
            DeafblindModeState.SEARCHING // falha de conexão durante o envio
        ),
        DeafblindModeState.RETURNING_TO_WAIT to setOf(
            DeafblindModeState.WAITING_MESSAGE
        )
    )

    fun setOnStateChangedListener(listener: (DeafblindModeState) -> Unit) {
        onStateChanged = listener
    }

    fun transitionTo(newState: DeafblindModeState): Boolean {
        val allowedNextStates = validTransitions[currentState] ?: emptySet()

        if (newState !in allowedNextStates) {
            return false
        }

        currentState = newState
        onStateChanged?.invoke(newState)
        return true
    }
}