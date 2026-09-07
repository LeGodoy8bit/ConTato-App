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

interface NearbyListener {
    fun onMessageReceived(message: String)
    fun onConnectionStatusChanged(status: String)
}

/**
 * Encapsula a comunicação P2P entre o celular do Guia (interlocutor) e o
 * celular da pessoa Surdocega, usando Nearby Connections (Google Play Services).
 */

class NearbyConnectionService(
    private val context: Context,
    private val listener: NearbyListener
) {

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)

    // Identifica o app para não conectar acidentalmente com outros apps Nearby por perto
    private val serviceId = "contato.app.BRAILLE"

    // P2P_POINT_TO_POINT: conexão direta de um único par de dispositivos (Guia <-> Surdocego)
    private val strategy = Strategy.P2P_POINT_TO_POINT

    private var connectedEndpointId: String? = null

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {

        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            // Aceita a conexão automaticamente, sem exigir confirmação manual do usuário
            connectionsClient.acceptConnection(endpointId, payloadCallback)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    connectedEndpointId = endpointId
                    listener.onConnectionStatusChanged("Conectado com: $endpointId")
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    listener.onConnectionStatusChanged("Conexão Rejeitada")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    listener.onConnectionStatusChanged("Erro na Conexão")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            connectedEndpointId = null
            listener.onConnectionStatusChanged("Desconectado")
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {

        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
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
        val advertisingOptions = AdvertisingOptions.Builder().setStrategy(strategy).build()

        connectionsClient.startAdvertising(userName, serviceId, connectionLifecycleCallback, advertisingOptions)
            .addOnSuccessListener {
                listener.onConnectionStatusChanged("Anunciando com sucesso!")
            }
            .addOnFailureListener { e ->
                listener.onConnectionStatusChanged("ERRO ao anunciar: ${e.message}")
            }
    }

    fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder().setStrategy(strategy).build()

        connectionsClient.startDiscovery(serviceId, endpointDiscoveryCallback, discoveryOptions)
            .addOnSuccessListener {
                listener.onConnectionStatusChanged("Procurando com sucesso!")
            }
            .addOnFailureListener { e ->
                listener.onConnectionStatusChanged("ERRO ao procurar: ${e.message}")
            }
    }

    fun sendMessage(message: String) {
        connectedEndpointId?.let { id ->
            val bytes = message.toByteArray(Charsets.UTF_8)
            val payload = Payload.fromBytes(bytes)
            connectionsClient.sendPayload(id, payload)
        }
    }

    /** Encerra advertising, discovery e conexões ativas. Chamar em onPause/onDestroy. */
    fun stop() {
        connectionsClient.stopAdvertising()
        connectionsClient.stopDiscovery()
        connectionsClient.stopAllEndpoints()
        connectedEndpointId = null
    }
}