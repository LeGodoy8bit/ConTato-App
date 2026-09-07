package contato.app.presentation

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import contato.app.R
import contato.app.core.connection.NearbyConnectionService
import contato.app.core.connection.NearbyListener
import contato.app.core.vibration.HapticMessagePlaybackService

/**
 * Tela de teste manual para a comunicação P2P via Nearby Connections.
 *
 * Papel "Guia": anuncia e envia mensagens de texto.
 * Papel "Surdocego": procura, recebe mensagens e sente a vibração correspondente.
 * A conversão de texto para vibração acontece em onMessageReceived, via
 * HapticMessagePlaybackService — é aqui que a comunicação em rede se conecta
 * ao pipeline Braille/háptico já existente no domínio do app.
 */
class NearbyDebugActivity : AppCompatActivity(), NearbyListener {

    private lateinit var nearbyService: NearbyConnectionService
    private lateinit var hapticPlayback: HapticMessagePlaybackService

    private lateinit var tvStatus: TextView
    private lateinit var tvReceived: TextView
    private lateinit var etMessage: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_debug)
        checkPermissions()

        nearbyService = NearbyConnectionService(this, this)
        hapticPlayback = HapticMessagePlaybackService(this)

        tvStatus = findViewById(R.id.tvStatus)
        tvReceived = findViewById(R.id.tvReceivedMessage)
        etMessage = findViewById(R.id.etMessage)

        findViewById<Button>(R.id.btnGuide).setOnClickListener {
            nearbyService.startAdvertising("Guia de Teste")
            tvStatus.text = "Status: Anunciando..."
        }

        findViewById<Button>(R.id.btnDeafblind).setOnClickListener {
            nearbyService.startDiscovery()
            tvStatus.text = "Status: Procurando..."
        }

        findViewById<Button>(R.id.btnSend).setOnClickListener {
            val text = etMessage.text.toString()
            if (text.isNotEmpty()) {
                nearbyService.sendMessage(text)
                etMessage.text.clear()
                Toast.makeText(this, "Enviado!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            tvReceived.text = message
        }
        hapticPlayback.play(message)
    }

    override fun onConnectionStatusChanged(status: String) {
        runOnUiThread {
            tvStatus.text = "Status: $status"
        }
    }

    override fun onPause() {
        super.onPause()
        hapticPlayback.stop()
        nearbyService.stop()
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.NEARBY_WIFI_DEVICES)
        }

        ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 1)
    }
}