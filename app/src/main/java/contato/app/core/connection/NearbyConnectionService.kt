package contato.app.core.connection

import android.content.Context
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import contato.app.domain.state.ConnectionState
import contato.app.domain.state.ConnectionStateMachine

interface NearbyListener {
    fun onMessageReceived(message: String)
    fun onConnectionStatusChanged(status: String)
    fun onConnectionStateChanged(state: ConnectionState)
}

class NearbyConnectionService(
    context: Context,
    private val listener: NearbyListener,
) {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)

    private val serviceId = "contato.app.BRAILLE"
    private val strategy = Strategy.P2P_POINT_TO_POINT

    private var connectedEndpointId: String? = null

    // Nome que o outro aparelho anunciou (ex.: o nome do Comunicador).
    // Guardado no início da conexão e confirmado quando ela é aceita.
    private val pendingNames = mutableMapOf<String, String>()
    private var connectedEndpointName: String? = null

    // Nome que este aparelho apresenta ao pedir conexão (lado que busca).
    private var localDiscoveryName = DEFAULT_DISCOVERY_NAME

    // Máquina de estados formal, seguindo os estados definidos na especificação.
    private val stateMachine = ConnectionStateMachine()

    init {
        stateMachine.setOnStateChangedListener { newState ->
            listener.onConnectionStateChanged(newState)
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            pendingNames[endpointId] = info.endpointName
            stateMachine.transitionTo(ConnectionState.CONNECTING)
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    connectedEndpointId = endpointId
                    connectedEndpointName = pendingNames.remove(endpointId)
                    connectionsClient.stopDiscovery() // recomendado pelo Nearby depois de conectar
                    stateMachine.transitionTo(ConnectionState.CONNECTED)
                    listener.onConnectionStatusChanged("Conectado com: ${connectedEndpointName ?: endpointId}")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    pendingNames.remove(endpointId)
                    stateMachine.transitionTo(ConnectionState.CONNECTION_ERROR)
                    listener.onConnectionStatusChanged("Conexão rejeitada")
                }
                else -> {
                    // Antes só STATUS_ERROR era tratado: outros códigos deixavam o app
                    // parado em CONNECTING para sempre.
                    pendingNames.remove(endpointId)
                    stateMachine.transitionTo(ConnectionState.CONNECTION_ERROR)
                    listener.onConnectionStatusChanged("Erro na conexão (${result.status.statusCode})")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            // Para anúncio e busca antigos: sem isto, a reconexão automática esbarrava em
            // STATUS_ALREADY_ADVERTISING/DISCOVERING (8001/8002) e demorava mais.
            connectionsClient.stopAdvertising()
            connectionsClient.stopDiscovery()
            connectedEndpointId = null
            connectedEndpointName = null
            stateMachine.transitionTo(ConnectionState.DISCONNECTED)
            listener.onConnectionStatusChanged("Desconectado")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {

        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            stateMachine.transitionTo(ConnectionState.DEVICE_FOUND)

            connectionsClient.requestConnection(
                localDiscoveryName,
                endpointId,
                connectionLifecycleCallback,
            ).addOnFailureListener { e ->
                val code = (e as? ApiException)?.statusCode
                if (code == ConnectionsStatusCodes.STATUS_ALREADY_CONNECTED_TO_ENDPOINT) return@addOnFailureListener
                // Sem isto, uma falha no pedido deixava o app parado em DEVICE_FOUND.
                listener.onConnectionStatusChanged("Falha ao pedir conexão: ${e.message}")
                stop()
            }
        }

        override fun onEndpointLost(endpointId: String) {
            // Dispositivo saiu de alcance ou parou de anunciar
        }
    }

    private val payloadCallback = object : PayloadCallback() {

        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val message = payload.asBytes()?.let { String(it, Charsets.UTF_8) }
                message?.let { listener.onMessageReceived(it) }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            // A confirmação de entrega agora vem do próprio app (WireMessage.Received).
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
                listener.onConnectionStatusChanged("ERRO ao anunciar: ${e.message}")
                stop()
            }
    }

    /** @param userName como este aparelho aparece para o Comunicador (ex.: "Celular tátil"). */
    fun startDiscovery(userName: String = DEFAULT_DISCOVERY_NAME) {
        if (isSearchingOrConnected()) return
        localDiscoveryName = userName

        stateMachine.transitionTo(ConnectionState.SCANNING)

        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(strategy).build()

        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                listener.onConnectionStatusChanged("Procurando com sucesso!")
            }
            .addOnFailureListener { e ->
                listener.onConnectionStatusChanged("ERRO ao procurar: ${e.message}")
                stop()
            }
    }

    /**
     * Evita chamadas duplicadas de startAdvertising/startDiscovery, que o Nearby
     * rejeita com STATUS_ALREADY_ADVERTISING (8001) ou STATUS_ALREADY_DISCOVERING (8002).
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
            connectionsClient.sendPayload(id, Payload.fromBytes(message.toByteArray(Charsets.UTF_8)))
        }
    }

    /** Encerra tudo e volta para DISCONNECTED (de onde é possível buscar de novo). */
    fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpointId = null
        connectedEndpointName = null
        pendingNames.clear()
        stateMachine.transitionTo(ConnectionState.DISCONNECTED)
    }

    fun getCurrentState(): ConnectionState = stateMachine.currentState

    /** Nome do aparelho conectado, quando houver. */
    fun getConnectedEndpointName(): String? = connectedEndpointName

    companion object {
        const val DEFAULT_DISCOVERY_NAME = "Celular tátil"
    }
}
