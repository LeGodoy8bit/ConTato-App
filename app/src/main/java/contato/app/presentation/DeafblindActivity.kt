package contato.app.presentation

import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import contato.app.R
import contato.app.core.connection.NearbyConnectionService
import contato.app.core.connection.NearbyListener
import contato.app.core.connection.NearbyPermissions
import contato.app.core.vibration.HapticMessagePlaybackService
import contato.app.domain.model.SystemFeedbackEvent
import contato.app.domain.service.BrailleMapper
import contato.app.domain.service.ConnectionFeedbackMapper
import contato.app.domain.service.MessageBuilder
import contato.app.domain.service.SystemHapticPatterns
import contato.app.domain.state.ConnectionState
import contato.app.domain.state.DeafblindModeState
import contato.app.domain.state.DeafblindModeStateMachine

/**
 * Tela real do usuário Surdocego.
 *
 * Combina quatro responsabilidades:
 * - Conexão automática via NearbyConnectionService (sem botões).
 * - Feedback háptico de sistema (ConnectionFeedbackMapper + SystemHapticPatterns).
 * - Composição de resposta em Braille (BrailleMapper.reverseMap + MessageBuilder).
 * - Gesto de repetir a última mensagem recebida (deslizar para cima).
 *
 * Tudo isso é orquestrado através de DeafblindModeStateMachine.
 */
class DeafblindActivity : Activity(), NearbyListener {

    // --- Serviços e lógica de domínio ---

    private lateinit var nearbyService: NearbyConnectionService          // conexão P2P (Nearby Connections)
    private lateinit var hapticPlayback: HapticMessagePlaybackService    // fachada de vibração (Braille + sistema)
    private val modeStateMachine = DeafblindModeStateMachine()           // controla o ciclo de estados do Surdocego
    private val messageBuilder = MessageBuilder()                       // monta o texto da resposta, letra por letra

    // Estado "ao vivo" dos 6 pontos da célula Braille sendo composta agora
    // (antes de confirmar). Índice 0..5 = pontos P1..P6.
    private val currentPoints = mutableListOf(false, false, false, false, false, false)

    // Guarda a última mensagem recebida, para permitir repeti-la sem
    // precisar de nova transmissão do Comunicador (seção 3.5 da especificação).
    private var lastReceivedMessage: String? = null

    // Detecta o gesto de deslizar para cima em qualquer ponto da tela.
    private lateinit var gestureDetector: GestureDetector

    // --- Views ---

    private lateinit var tvDebugStatus: TextView       // texto de status/debug (não essencial ao usuário final)
    private lateinit var tvComposedMessage: TextView   // mostra o texto sendo composto no momento
    private lateinit var pointButtons: List<Button>    // os 6 botões da célula Braille (P1..P6)
    private lateinit var btnDelete: Button             // comando: apagar último caractere
    private lateinit var btnSpace: Button              // comando: adicionar espaço
    private lateinit var btnConfirm: Button            // comando: confirmar o caractere atual
    private lateinit var btnSendMessage: Button        // comando: enviar a mensagem composta

    // ==========================================================
    // CICLO DE VIDA DA ACTIVITY
    // ==========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_deafblind)

        // Configura o reconhecedor de gestos (usado em dispatchTouchEvent, abaixo).
        gestureDetector = GestureDetector(this, SwipeUpGestureListener())

        // Conecta as variáveis de View aos elementos do layout.
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
        setComposingEnabled(false) // célula Braille começa desabilitada, até chegar uma mensagem

        // Pede permissões; startDiscovery() só acontece depois da resposta
        // (em onRequestPermissionsResult), nunca aqui direto.
        ActivityCompat.requestPermissions(
            this,
            NearbyPermissions.getRequiredPermissions(),
            PERMISSIONS_REQUEST_CODE
        )
    }

    /** Chamado quando o usuário responde ao pedido de permissões. */
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

    /** Retoma a busca automaticamente se a conexão caiu enquanto o app estava em segundo plano. */
    override fun onResume() {
        super.onResume()

        if (::nearbyService.isInitialized &&
            nearbyService.getCurrentState() == ConnectionState.DISCONNECTED
        ) {
            nearbyService.startDiscovery()
        }
    }

    /** Para a vibração e encerra a conexão quando o app sai de foco. */
    override fun onPause() {
        super.onPause()
        hapticPlayback.stop()
        nearbyService.stop()
    }

    /**
     * Intercepta TODO toque na tela antes de qualquer botão processá-lo,
     * repassando ao gestureDetector — assim o gesto de "deslizar para cima"
     * funciona mesmo que o dedo comece sobre um dos botões da célula Braille.
     */
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        return super.dispatchTouchEvent(event)
    }

    // ==========================================================
    // CÉLULA BRAILLE: toque nos 6 pontos
    // ==========================================================

    /** Associa cada botão de ponto a um clique que inverte seu estado (ativo/inativo). */
    private fun setupPointButtons() {
        pointButtons.forEachIndexed { index, button ->
            button.setOnClickListener {
                currentPoints[index] = !currentPoints[index]
                updatePointButtonAppearance(button, currentPoints[index])
            }
        }
    }

    /** Feedback visual simples (opacidade) de qual ponto está ativo agora. */
    private fun updatePointButtonAppearance(button: Button, active: Boolean) {
        button.alpha = if (active) 2.0f else 0.5f
    }

    /** Zera os 6 pontos, preparando a célula para o próximo caractere. */
    private fun resetPoints() {
        for (i in currentPoints.indices) {
            currentPoints[i] = false
        }
        pointButtons.forEach { updatePointButtonAppearance(it, false) }
    }

    // ==========================================================
    // COMANDOS: apagar, espaço, confirmar, enviar
    // ==========================================================

    /** Associa cada botão de comando à sua ação correspondente. */
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

    /**
     * Converte os pontos pressionados em um caractere (via BrailleMapper.reverseMap)
     * e adiciona à mensagem em construção. Se a combinação não corresponder a
     * nenhuma letra conhecida, vibra o padrão de erro em vez de travar.
     */
    private fun confirmCurrentCharacter() {
        // Primeira confirmação depois de receber mensagem: entra formalmente em composição.
        if (modeStateMachine.currentState == DeafblindModeState.RECEIVING_MESSAGE) {
            modeStateMachine.transitionTo(DeafblindModeState.COMPOSING_BRAILLE)
        }

        val character = BrailleMapper.reverseMap(currentPoints)

        if (character != null) {
            messageBuilder.appendCharacter(character)
            playCommandFeedback()
        } else {
            hapticPlayback.playSystemFeedback(SystemHapticPatterns.getPulsesFor(SystemFeedbackEvent.ERROR))
        }

        resetPoints()
        refreshComposedMessage()
    }

    /** Envia a mensagem composta ao Comunicador e retorna ao estado de espera. */
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

    /** Vibração curta padrão usada para confirmar que um comando foi reconhecido. */
    private fun playCommandFeedback() {
        hapticPlayback.playSystemFeedback(SystemHapticPatterns.getPulsesFor(SystemFeedbackEvent.COMMAND_RECOGNIZED))
    }

    /** Atualiza o texto exibido com o conteúdo atual do MessageBuilder. */
    private fun refreshComposedMessage() {
        tvComposedMessage.text = messageBuilder.currentText
    }

    /** Habilita/desabilita toda a célula Braille e os comandos de uma vez. */
    private fun setComposingEnabled(enabled: Boolean) {
        pointButtons.forEach { it.isEnabled = enabled }
        btnDelete.isEnabled = enabled
        btnSpace.isEnabled = enabled
        btnConfirm.isEnabled = enabled
        btnSendMessage.isEnabled = enabled
    }

    // ==========================================================
    // GESTO: repetir última mensagem (deslizar para cima)
    // ==========================================================

    /**
     * Repete a última mensagem recebida, sem exigir nova transmissão do
     * Comunicador. Ignorado se não houver mensagem guardada, ou se o app
     * estiver ocupado compondo/enviando uma resposta no momento.
     */
    private fun repeatLastMessage() {
        val message = lastReceivedMessage

        if (message == null) {
            // Nada para repetir ainda — vibra erro em vez de ficar em silêncio.
            hapticPlayback.playSystemFeedback(SystemHapticPatterns.getPulsesFor(SystemFeedbackEvent.ERROR))
            return
        }

        val currentState = modeStateMachine.currentState
        if (currentState != DeafblindModeState.WAITING_MESSAGE &&
            currentState != DeafblindModeState.RECEIVING_MESSAGE
        ) {
            return // gesto ignorado fora de um momento de espera/recebimento
        }

        modeStateMachine.transitionTo(DeafblindModeState.REPEATING_MESSAGE)

        // Vibração curta confirmando o comando, seguida da mensagem em si.
        hapticPlayback.playSystemFeedback(SystemHapticPatterns.getPulsesFor(SystemFeedbackEvent.COMMAND_RECOGNIZED))
        hapticPlayback.play(message)

        modeStateMachine.transitionTo(DeafblindModeState.WAITING_MESSAGE)
    }

    /**
     * Reconhece o gesto de deslizar para cima (fling vertical, de baixo pra
     * cima) em qualquer ponto da tela, e dispara repeatLastMessage().
     *
     * "inner class" porque precisa acessar repeatLastMessage(), que pertence
     * à Activity de fora.
     */
    private inner class SwipeUpGestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onFling(
            e1: MotionEvent?,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (e1 == null) return false

            val deltaY = e2.y - e1.y   // negativo = dedo subiu na tela
            val deltaX = e2.x - e1.x

            val isVerticalSwipe = kotlin.math.abs(deltaY) > kotlin.math.abs(deltaX)
            val isUpwardSwipe = deltaY < -SWIPE_MIN_DISTANCE
            val isFastEnough = kotlin.math.abs(velocityY) > SWIPE_MIN_VELOCITY

            if (isVerticalSwipe && isUpwardSwipe && isFastEnough) {
                repeatLastMessage()
                return true
            }

            return false
        }
    }

    // ==========================================================
    // IMPLEMENTAÇÃO DE NearbyListener
    // ==========================================================

    /** Chamado quando uma mensagem de texto chega pela rede. */
    override fun onMessageReceived(message: String) {
        lastReceivedMessage = message // guarda para permitir repetição futura

        runOnUiThread {
            tvDebugStatus.text = "Mensagem: $message"
            setComposingEnabled(true)
        }

        if (modeStateMachine.currentState == DeafblindModeState.WAITING_MESSAGE) {
            modeStateMachine.transitionTo(DeafblindModeState.RECEIVING_MESSAGE)
        }

        hapticPlayback.play(message)
    }

    /** Callback de texto livre, usado só para debug visual. */
    override fun onConnectionStatusChanged(status: String) {
        runOnUiThread {
            tvDebugStatus.text = status
        }
    }

    /** Chamado a cada transição válida da ConnectionStateMachine. */
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

    // ==========================================================
    // CONSTANTES
    // ==========================================================

    companion object {
        // Código usado para identificar a resposta ao pedido de permissões.
        private const val PERMISSIONS_REQUEST_CODE = 1

        // Parâmetros do gesto de "deslizar para cima" (repetir mensagem).
        // Valores iniciais — a especificação prevê calibração em testes
        // de usabilidade; ajustar aqui conforme feedback real de uso.
        private const val SWIPE_MIN_DISTANCE = 150
        private const val SWIPE_MIN_VELOCITY = 200
    }
}