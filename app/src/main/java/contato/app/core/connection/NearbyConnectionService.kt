package contato.app.core.connection

import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.Strategy
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import contato.app.domain.state.ConnectionState
import contato.app.domain.state.ConnectionStateMachine

interface NearbyListener {
    fun onMessageReceived(message: String)
    fun onConnectionStatusChanged(status: String)
    fun onConnectionStateChanged(state: ConnectionState)
}

class NearbyConnectionService(
    private val context: Context,
    private val listener: NearbyListener
) {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)

    private val serviceId = "contato.app.BRAILLE"
    private val strategy = Strategy.P2P_POINT_TO_POINT

    private var connectedEndpointId: String? = null

    // Máquina de estados formal, seguindo os estados definidos na especificação.
    private val stateMachine = ConnectionStateMachine()

    init {
        stateMachine.setOnStateChangedListener { newState ->
            listener.onConnectionStateChanged(newState)
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            stateMachine.transitionTo(ConnectionState.CONNECTING)
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    connectedEndpointId = endpointId
                    stateMachine.transitionTo(ConnectionState.CONNECTED)
                    listener.onConnectionStatusChanged("Conectado com: $endpointId")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    stateMachine.transitionTo(ConnectionState.CONNECTION_ERROR)
                    listener.onConnectionStatusChanged("Conexão Rejeitada")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    stateMachine.transitionTo(ConnectionState.CONNECTION_ERROR)
                    listener.onConnectionStatusChanged("Erro na Conexão")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpointId = null
            stateMachine.transitionTo(ConnectionState.DISCONNECTED)
            listener.onConnectionStatusChanged("Desconectado")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {

        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            stateMachine.transitionTo(ConnectionState.DEVICE_FOUND)

            connectionsClient.requestConnection(
                "Usuário Surdocego",
                endpointId,
                connectionLifecycleCallback
            )
        }

        override fun onEndpointLost(endpointId: String) {
            // Dispositivo saiu de alcance ou parou de anunciar
        }
    }

    private val payloadCallback = object : PayloadCallback() {

        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val receivedBytes = payload.asBytes()
                val message = receivedBytes?.let { String(it, Charsets.UTF_8) }
                message?.let { listener.onMessageReceived(it) }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // Relevante para acompanhar progresso de payloads grandes (não usado ainda)
        }
    }

    fun startAdvertising(userName: String) {
        if (isSearchingOrConnected()) return

        stateMachine.transitionTo(ConnectionState.SCANNING)

        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(strategy).build()

        connectionsClient.startAdvertising(userName, serviceId, connectionLifecycleCallback, advertisingOptions)
            .addOnSuccessListener {
                listener.onConnectionStatusChanged("Anunciando com sucesso!")
            }
            .addOnFailureListener { e ->
                stateMachine.transitionTo(ConnectionState.CONNECTION_ERROR)
                listener.onConnectionStatusChanged("ERRO ao anunciar: ${e.message}")
            }
    }

    fun startDiscovery() {
        if (isSearchingOrConnected()) return

        stateMachine.transitionTo(ConnectionState.SCANNING)

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(strategy).build()

        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                listener.onConnectionStatusChanged("Procurando com sucesso!")
            }
            .addOnFailureListener { e ->
                stateMachine.transitionTo(ConnectionState.CONNECTION_ERROR)
                listener.onConnectionStatusChanged("ERRO ao procurar: ${e.message}")
            }
    }

    /**
     * Evita chamadas duplicadas de startAdvertising/startDiscovery, que o Nearby
     * rejeita com STATUS_ALREADY_ADVERTISING (8001) ou STATUS_ALREADY_DISCOVERING
     * (8002). Sem esta guarda, uma segunda chamada durante uma busca/anúncio já
     * em andamento fazia a máquina de estados transicionar incorretamente para
     * CONNECTION_ERROR, mesmo com a operação original continuando ativa.
     */
    private fun isSearchingOrConnected(): Boolean {
        val currentState = stateMachine.currentState
        return currentState == ConnectionState.SCANNING ||
                currentState == ConnectionState.DEVICE_FOUND ||
                currentState == ConnectionState.CONNECTING ||
                currentState == ConnectionState.CONNECTED
    }

    fun sendMessage(message: String) {
        connectedEndpointId?.let { id ->
            val bytes = message.toByteArray(Charsets.UTF_8)
            val payload = Payload.fromBytes(bytes)
            connectionsClient.sendPayload(id, payload)
        }
    }

    fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpointId = null
        stateMachine.transitionTo(ConnectionState.DISCONNECTED)
    }
    fun getCurrentState(): ConnectionState = stateMachine.currentState

}