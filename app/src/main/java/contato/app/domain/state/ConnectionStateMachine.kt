package contato.app.domain.state

/**
 * Controla as transições válidas entre estados de conexão.
 *
 * Existe para impedir que a UI ou o serviço de conexão apresentem um
 * estado inconsistente (ex.: "Enviando" enquanto, na prática, o app
 * está desconectado). Qualquer tentativa de transição fora das regras
 * definidas em [validTransitions] é ignorada silenciosamente e não
 * altera o estado atual.
 */

class ConnectionStateMachine(
    initialState: ConnectionState = ConnectionState.DISCONNECTED
) {

    var currentState: ConnectionState = initialState
        private set

    private var onStateChanged: ((ConnectionState) -> Unit)? = null

    private val validTransitions: Map<ConnectionState, Set<ConnectionState>> = mapOf(
        ConnectionState.DISCONNECTED to setOf(
            ConnectionState.SCANNING,
            ConnectionState.CONNECTING
        ),
        ConnectionState.SCANNING to setOf(
            ConnectionState.DEVICE_FOUND,
            ConnectionState.DISCONNECTED
        ),
        ConnectionState.DEVICE_FOUND to setOf(
            ConnectionState.CONNECTING,
            ConnectionState.DISCONNECTED
        ),
        ConnectionState.CONNECTING to setOf(
            ConnectionState.CONNECTED,
            ConnectionState.CONNECTION_ERROR,
            ConnectionState.DISCONNECTED
        ),
        ConnectionState.CONNECTED to setOf(
            ConnectionState.DISCONNECTED,
            ConnectionState.RECONNECTING
        ),
        ConnectionState.CONNECTION_ERROR to setOf(
            ConnectionState.RECONNECTING,
            ConnectionState.DISCONNECTED
        ),
        ConnectionState.RECONNECTING to setOf(
            ConnectionState.CONNECTING,
            ConnectionState.CONNECTED,
            ConnectionState.DISCONNECTED
        )
    )

    /** Define o callback chamado sempre que uma transição válida ocorre. */
    fun setOnStateChangedListener(listener: (ConnectionState) -> Unit) {
        onStateChanged = listener
    }

    /**
     * Tenta mover para [newState]. Retorna true se a transição era válida
     * e foi aplicada; retorna false se a transição foi ignorada.
     */
    fun transitionTo(newState: ConnectionState): Boolean {
        val allowedNextStates = validTransitions[currentState] ?: emptySet()

        if (newState !in allowedNextStates) {
            return false
        }

        currentState = newState
        onStateChanged?.invoke(newState)
        return true
    }
}