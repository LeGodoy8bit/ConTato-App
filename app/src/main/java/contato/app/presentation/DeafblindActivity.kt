package contato.app.presentation

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import androidx.core.app.ActivityCompat
import contato.app.R
import contato.app.core.connection.NearbyConnectionService
import contato.app.core.connection.NearbyListener
import contato.app.core.connection.NearbyPermissions
import contato.app.core.vibration.HapticMessagePlaybackService
import contato.app.domain.service.ConnectionFeedbackMapper
import contato.app.domain.service.SystemHapticPatterns
import contato.app.domain.state.ConnectionState

class DeafblindActivity : Activity(), NearbyListener {

    private lateinit var nearbyService: NearbyConnectionService
    private lateinit var hapticPlayback: HapticMessagePlaybackService
    private lateinit var tvDebugStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deafblind)

        tvDebugStatus = findViewById(R.id.tvDebugStatus)

        nearbyService = NearbyConnectionService(this, this)
        hapticPlayback = HapticMessagePlaybackService(this)

        // A busca só é iniciada depois que o usuário responder ao pedido
        // de permissão, em onRequestPermissionsResult — nunca aqui direto,
        // pois requestPermissions() é assíncrono.
        ActivityCompat.requestPermissions(
            this,
            NearbyPermissions.getRequiredPermissions(),
            PERMISSIONS_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != PERMISSIONS_REQUEST_CODE) return

        val allGranted = grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }

        if (allGranted) {
            nearbyService.startDiscovery()
        } else {
            tvDebugStatus.text = "Permissões necessárias não concedidas."
        }
    }

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            tvDebugStatus.text = "Mensagem: $message"
        }
        hapticPlayback.play(message)
    }

    override fun onConnectionStatusChanged(status: String) {
        runOnUiThread {
            tvDebugStatus.text = status
        }
    }

    override fun onConnectionStateChanged(state: ConnectionState) {
        runOnUiThread {
            tvDebugStatus.text = "Estado: $state"
        }

        ConnectionFeedbackMapper.mapConnectionStateToFeedback(state)?.let { feedbackEvent ->
            val pulses = SystemHapticPatterns.getPulsesFor(feedbackEvent)
            hapticPlayback.playSystemFeedback(pulses)
        }
    }

    override fun onPause() {
        super.onPause()
        hapticPlayback.stop()
        nearbyService.stop()
    }

    companion object {
        private const val PERMISSIONS_REQUEST_CODE = 1
    }
}