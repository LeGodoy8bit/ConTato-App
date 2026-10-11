package contato.app.domain.state

/**
 * Controla as transições válidas entre estados de conexão.
 *
 * Existe para impedir que a UI ou o serviço de conexão apresentem um
 * estado inconsistente (ex.: "Enviando" enquanto, na prática, o app
 * está desconectado). Qualquer tentativa de transição fora das regras
 * definidas em [validTransitions] é ignorada silenciosamente e não
 * altera o estado atual.
 *
 * IMPORTANTE — assimetria entre papéis: esta máquina é compartilhada
 * pelos dois papéis do Nearby Connections (quem anuncia e quem busca),
 * que percorrem caminhos ligeiramente diferentes:
 *
 * - Quem BUSCA (ex.: Surdocego, via startDiscovery): passa por
 *   SCANNING -> DEVICE_FOUND -> CONNECTING -> CONNECTED, pois recebe
 *   o callback onEndpointFound antes de solicitar a conexão.
 * - Quem ANUNCIA (ex.: Comunicador, via startAdvertising): vai direto
 *   de SCANNING -> CONNECTING -> CONNECTED, pois recebe apenas
 *   onConnectionInitiated, sem nunca passar por DEVICE_FOUND (esse
 *   conceito só existe do lado de quem descobre ativamente o outro).
 *
 * Por isso DEVICE_FOUND é tratado como um estado intermediário
 * OPCIONAL: CONNECTING é alcançável tanto a partir de SCANNING quanto
 * de DEVICE_FOUND, cobrindo os dois papéis sem duplicar a máquina de
 * estados nem criar um conjunto de estados separado para cada um.
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
            ConnectionState.DEVICE_FOUND,  // caminho de quem busca
            ConnectionState.CONNECTING,    // caminho de quem anuncia
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