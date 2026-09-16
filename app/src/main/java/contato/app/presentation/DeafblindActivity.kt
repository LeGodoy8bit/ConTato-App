package contato.app.presentation

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import contato.app.R
import contato.app.core.connection.NearbyConnectionService
import contato.app.core.connection.NearbyListener
import contato.app.core.connection.NearbyPermissions
import contato.app.core.vibration.HapticMessagePlaybackService
import contato.app.domain.service.BrailleMapper
import contato.app.domain.service.ConnectionFeedbackMapper
import contato.app.domain.service.MessageBuilder
import contato.app.domain.service.SystemHapticPatterns
import contato.app.domain.model.SystemFeedbackEvent
import contato.app.domain.state.ConnectionState
import contato.app.domain.state.DeafblindModeState
import contato.app.domain.state.DeafblindModeStateMachine

/**
 * Tela real do usuário Surdocego.
 *
 * Combina três responsabilidades já validadas separadamente:
 * - Conexão automática via NearbyConnectionService (sem botões).
 * - Feedback háptico de sistema (ConnectionFeedbackMapper + SystemHapticPatterns).
 * - Composição de resposta em Braille (BrailleMapper.reverseMap + MessageBuilder),
 *   seguindo o ciclo de DeafblindModeStateMachine.
 *
 * A célula Braille (6 pontos + comandos) fica sempre visível, mas só
 * responde a interação depois que uma mensagem é recebida — reflete a
 * regra da especificação de que a composição é uma RESPOSTA, não uma
 * ação livre a qualquer momento.
 */
class DeafblindActivity : Activity(), NearbyListener {

    private lateinit var nearbyService: NearbyConnectionService
    private lateinit var hapticPlayback: HapticMessagePlaybackService
    private val modeStateMachine = DeafblindModeStateMachine()
    private val messageBuilder = MessageBuilder()

    // Estado dos 6 pontos da célula Braille sendo composta no momento.
    private val currentPoints = mutableListOf(false, false, false, false, false, false)

    private lateinit var tvDebugStatus: TextView
    private lateinit var tvComposedMessage: TextView
    private lateinit var pointButtons: List<Button>
    private lateinit var btnDelete: Button
    private lateinit var btnSpace: Button
    private lateinit var btnConfirm: Button
    private lateinit var btnSendMessage: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deafblind)

        tvDebugStatus = findViewById(R.id.tvDebugStatus)
        tvComposedMessage = findViewById(R.id.tvComposedMessage)

        pointButtons = listOf(
            findViewById(R.id.btnPoint1),
            findViewById(R.id.btnPoint2),
            findViewById(R.id.btnPoint3),
            findViewById(R.id.btnPoint4),
            findViewById(R.id.btnPoint5),
            findViewById(R.id.btnPoint6)
        )

        btnDelete = findViewById(R.id.btnDelete)
        btnSpace = findViewById(R.id.btnSpace)
        btnConfirm = findViewById(R.id.btnConfirm)
        btnSendMessage = findViewById(R.id.btnSendMessage)

        nearbyService = NearbyConnectionService(this, this)
        hapticPlayback = HapticMessagePlaybackService(this)

        setupPointButtons()
        setupCommandButtons()
        setComposingEnabled(false) // desabilitado até chegar uma mensagem

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
            modeStateMachine.transitionTo(DeafblindModeState.SEARCHING)
            nearbyService.startDiscovery()
        } else {
            tvDebugStatus.text = "Permissões necessárias não concedidas."
        }
    }

    override fun onResume() {
        super.onResume()

        if (::nearbyService.isInitialized &&
            nearbyService.getCurrentState() == ConnectionState.DISCONNECTED
        ) {
            nearbyService.startDiscovery()
        }
    }

    // --- Célula Braille: toque nos pontos ---

    private fun setupPointButtons() {
        pointButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                currentPoints[index] = !currentPoints[index]
                updatePointButtonAppearance(button, currentPoints[index])
            }
        }
    }

    private fun updatePointButtonAppearance(button: Button, active: Boolean) {
        button.alpha = if (active) 1.0f else 0.5f
    }

    private fun resetPoints() {
        for (i in currentPoints.indices) {
            currentPoints[i] = false
        }
        pointButtons.forEach { updatePointButtonAppearance(it, false) }
    }

    // --- Comandos: apagar, espaço, confirmar, enviar ---

    private fun setupCommandButtons() {
        btnDelete.setOnClickListener {
            messageBuilder.deleteLastCharacter()
            playCommandFeedback()
            refreshComposedMessage()
        }

        btnSpace.setOnClickListener {
            messageBuilder.appendSpace()
            playCommandFeedback()
            refreshComposedMessage()
        }

        btnConfirm.setOnClickListener {
            confirmCurrentCharacter()
        }

        btnSendMessage.setOnClickListener {
            sendComposedMessage()
        }
    }

    private fun confirmCurrentCharacter() {
        // Primeira confirmação depois de receber uma mensagem: entra
        // formalmente no estado de composição.
        if (modeStateMachine.currentState == DeafblindModeState.RECEIVING_MESSAGE) {
            modeStateMachine.transitionTo(DeafblindModeState.COMPOSING_BRAILLE)
        }

        val character = BrailleMapper.reverseMap(currentPoints)

        if (character != null) {
            messageBuilder.appendCharacter(character)
            playCommandFeedback()
        } else {
            // Combinação de pontos não corresponde a nenhum caractere conhecido.
            hapticPlayback.playSystemFeedback(SystemHapticPatterns.getPulsesFor(SystemFeedbackEvent.ERROR))
        }

        resetPoints()
        refreshComposedMessage()
    }

    private fun sendComposedMessage() {
        if (messageBuilder.isEmpty()) return

        modeStateMachine.transitionTo(DeafblindModeState.SENDING_MESSAGE)

        nearbyService.sendMessage(messageBuilder.currentText)
        hapticPlayback.playSystemFeedback(SystemHapticPatterns.getPulsesFor(SystemFeedbackEvent.MESSAGE_SENT))

        messageBuilder.clear()
        refreshComposedMessage()
        resetPoints()

        modeStateMachine.transitionTo(DeafblindModeState.RETURNING_TO_WAIT)
        modeStateMachine.transitionTo(DeafblindModeState.WAITING_MESSAGE)
        setComposingEnabled(false)
    }

    private fun playCommandFeedback() {
        hapticPlayback.playSystemFeedback(SystemHapticPatterns.getPulsesFor(SystemFeedbackEvent.COMMAND_RECOGNIZED))
    }

    private fun refreshComposedMessage() {
        tvComposedMessage.text = messageBuilder.currentText
    }

    private fun setComposingEnabled(enabled: Boolean) {
        pointButtons.forEach { it.isEnabled = enabled }
        btnDelete.isEnabled = enabled
        btnSpace.isEnabled = enabled
        btnConfirm.isEnabled = enabled
        btnSendMessage.isEnabled = enabled
    }

    // --- Implementação de NearbyListener ---

    override fun onMessageReceived(message: String) {
        runOnUiThread {
            tvDebugStatus.text = "Mensagem: $message"
            setComposingEnabled(true)
        }

        if (modeStateMachine.currentState == DeafblindModeState.WAITING_MESSAGE) {
            modeStateMachine.transitionTo(DeafblindModeState.RECEIVING_MESSAGE)
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

        if (state == ConnectionState.CONNECTED) {
            modeStateMachine.transitionTo(DeafblindModeState.CONNECTED)
            modeStateMachine.transitionTo(DeafblindModeState.WAITING_MESSAGE)
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