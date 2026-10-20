package contato.app.presentation.deafblind

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import contato.app.core.connection.NearbyConnectionService
import contato.app.core.connection.NearbyListener
import contato.app.core.vibration.HapticMessagePlaybackService
import contato.app.domain.model.SystemFeedbackEvent
import contato.app.domain.model.WireMessage
import contato.app.domain.service.BrailleMapper
import contato.app.domain.service.BrailleTextNormalizer
import contato.app.domain.service.ConnectionFeedbackMapper
import contato.app.domain.service.HapticTimelineBuilder
import contato.app.domain.service.MessageBuilder
import contato.app.domain.service.SystemHapticPatterns
import contato.app.domain.service.WireProtocol
import contato.app.domain.state.ConnectionState
import contato.app.domain.state.DeafblindModeState
import contato.app.domain.state.DeafblindModeStateMachine
import java.util.UUID

/** Último comando reconhecido, mostrado na área de gestos (para a pessoa de apoio acompanhar). */
internal data class CommandEcho(val text: String, val ok: Boolean, val key: Long = System.nanoTime())

/**
 * Estado e regras do modo Surdocego (antes tudo ficava dentro da DeafblindActivity).
 *
 * - Conexão automática, com nova busca sozinha quando a conexão cai.
 * - Recebe a mensagem, vibra letra a letra e avisa o Comunicador de cada letra.
 * - Composição da resposta na cela Braille de 6 pontos + comandos por gesto.
 * - Tudo passa pela DeafblindModeStateMachine.
 */
internal class DeafblindSession(context: Context) : NearbyListener {

    private val nearby = NearbyConnectionService(context, this)
    private val playback = HapticMessagePlaybackService(context)
    private val modeStateMachine = DeafblindModeStateMachine()
    private val messageBuilder = MessageBuilder()
    private val timelineBuilder = HapticTimelineBuilder()
    private val handler = Handler(Looper.getMainLooper())

    /** Como este celular aparece para o Comunicador: "Celular tátil (modelo)". */
    private val localName = "${NearbyConnectionService.DEFAULT_DISCOVERY_NAME} (${Build.MODEL.take(18)})"

    // ───────── Estado lido pela tela ─────────
    var connectionState by mutableStateOf(ConnectionState.DISCONNECTED)
        private set
    var peerName by mutableStateOf<String?>(null)
        private set

    /** Última mensagem recebida, já normalizada (o que de fato vibra). */
    var message by mutableStateOf<String?>(null)
        private set
    /** Letra vibrando agora (-1 = nenhuma). */
    var playingIndex by mutableStateOf(-1)
        private set
    var isPlaying by mutableStateOf(false)
        private set

    /** Pontos marcados agora (índices 0..5 = pontos 1..6). */
    val points = mutableStateListOf(false, false, false, false, false, false)
    var composed by mutableStateOf("")
        private set
    var echo by mutableStateOf<CommandEcho?>(null)
        private set

    /** A cela libera depois da primeira mensagem (resposta vem depois de receber). */
    val canCompose: Boolean
        get() = connectionState == ConnectionState.CONNECTED && message != null && !isPlaying

    private var lastReceivedId = ""
    private var active = false
    private var permitted = false

    // ───────── Ciclo de vida ─────────

    fun onPermissionsGranted() {
        permitted = true
        modeStateMachine.transitionTo(DeafblindModeState.SEARCHING)
        if (active) nearby.startDiscovery(localName)
    }

    fun setActive(isActive: Boolean) {
        active = isActive
        if (isActive) {
            if (permitted && nearby.getCurrentState() == ConnectionState.DISCONNECTED) nearby.startDiscovery(localName)
        } else {
            // Primeiro marca inativo: o DISCONNECTED gerado pelo stop() não deve religar a busca.
            handler.removeCallbacksAndMessages(null)
            playback.stop()
            isPlaying = false
            playingIndex = -1
            nearby.stop()
        }
    }

    // ───────── Cela Braille e comandos ─────────

    fun togglePoint(index: Int) {
        if (!canCompose) { rejectNoMessage(); return }
        points[index] = !points[index]
        feedback(if (points[index]) SystemFeedbackEvent.DOT_ON else SystemFeedbackEvent.DOT_OFF)
    }

    /** Toque na área de gestos: transforma os pontos marcados em letra. */
    fun confirmLetter() {
        if (isPlaying) { stopPlayback(); return }
        if (!canCompose) { rejectNoMessage(); return }
        enterComposing()
        val letter = BrailleMapper.reverseMap(points.toList())
        if (letter != null && messageBuilder.appendCharacter(letter)) {
            feedback(SystemFeedbackEvent.COMMAND_RECOGNIZED)
            echo = CommandEcho("Letra $letter", ok = true)
        } else {
            feedback(SystemFeedbackEvent.ERROR)
            echo = CommandEcho(if (letter == null) "Combinação inválida" else "Limite de letras", ok = false)
        }
        clearPoints()
        composed = messageBuilder.currentText
    }

    /** Deslizar para a esquerda. */
    fun deleteLast() {
        if (!canCompose) { rejectNoMessage(); return }
        if (messageBuilder.deleteLastCharacter()) {
            feedback(SystemFeedbackEvent.COMMAND_RECOGNIZED)
            echo = CommandEcho("Apagou", ok = true)
        } else {
            feedback(SystemFeedbackEvent.ERROR)
            echo = CommandEcho("Nada para apagar", ok = false)
        }
        composed = messageBuilder.currentText
    }

    /** Deslizar para a direita. */
    fun addSpace() {
        if (!canCompose) { rejectNoMessage(); return }
        enterComposing()
        if (messageBuilder.appendSpace()) {
            feedback(SystemFeedbackEvent.COMMAND_RECOGNIZED)
            echo = CommandEcho("Espaço", ok = true)
        } else {
            feedback(SystemFeedbackEvent.ERROR)
            echo = CommandEcho("Limite de letras", ok = false)
        }
        composed = messageBuilder.currentText
    }

    /** Segurar: envia a resposta. */
    fun send() {
        if (!canCompose) { rejectNoMessage(); return }
        val text = messageBuilder.currentText.trim()
        if (text.isEmpty()) {
            feedback(SystemFeedbackEvent.ERROR)
            echo = CommandEcho("Resposta vazia", ok = false)
            return
        }
        enterComposing()
        modeStateMachine.transitionTo(DeafblindModeState.SENDING_MESSAGE)
        nearby.sendMessage(WireProtocol.encode(WireMessage.Text(UUID.randomUUID().toString(), text)))
        feedback(SystemFeedbackEvent.MESSAGE_SENT)
        echo = CommandEcho("Resposta enviada", ok = true)
        messageBuilder.clear()
        composed = ""
        clearPoints()
        modeStateMachine.transitionTo(DeafblindModeState.RETURNING_TO_WAIT)
        modeStateMachine.transitionTo(DeafblindModeState.WAITING_MESSAGE)
    }

    /** Deslizar para cima: repete a última mensagem (o Comunicador vê vibrando de novo). */
    fun repeatLast() {
        val text = message
        if (text == null || connectionState != ConnectionState.CONNECTED) {
            feedback(SystemFeedbackEvent.ERROR)
            echo = CommandEcho("Nada para repetir", ok = false)
            return
        }
        val state = modeStateMachine.currentState
        if (state == DeafblindModeState.WAITING_MESSAGE || state == DeafblindModeState.RECEIVING_MESSAGE) {
            modeStateMachine.transitionTo(DeafblindModeState.REPEATING_MESSAGE)
            modeStateMachine.transitionTo(DeafblindModeState.WAITING_MESSAGE)
        }
        feedback(SystemFeedbackEvent.COMMAND_RECOGNIZED)
        echo = CommandEcho("Repetindo", ok = true)
        handler.postDelayed({ playWithProgress(text, lastReceivedId) }, REPEAT_DELAY_MS)
    }

    /** Gesto antes da primeira mensagem (ou durante a vibração): vibra "erro" para a pessoa não ficar sem resposta. */
    private fun rejectNoMessage() {
        feedback(SystemFeedbackEvent.ERROR)
        echo = CommandEcho(
            when {
                connectionState != ConnectionState.CONNECTED -> "Sem conexão"
                isPlaying -> "Espere a vibração"
                else -> "Aguarde uma mensagem"
            },
            ok = false,
        )
    }

    /** Leva a máquina de estados até "compondo", também na segunda resposta em diante. */
    private fun enterComposing() {
        if (modeStateMachine.currentState == DeafblindModeState.WAITING_MESSAGE) {
            modeStateMachine.transitionTo(DeafblindModeState.RECEIVING_MESSAGE)
        }
        if (modeStateMachine.currentState == DeafblindModeState.RECEIVING_MESSAGE) {
            modeStateMachine.transitionTo(DeafblindModeState.COMPOSING_BRAILLE)
        }
    }

    private fun clearPoints() {
        for (i in points.indices) points[i] = false
    }

    private fun feedback(event: SystemFeedbackEvent) {
        playback.playSystemFeedback(SystemHapticPatterns.getPulsesFor(event))
    }

    // ───────── Recebimento e vibração ─────────

    private fun onIncomingText(wire: WireMessage.Text) {
        val normalized = BrailleTextNormalizer.normalizeForBraille(wire.text)
        if (normalized.isEmpty()) return
        message = normalized
        lastReceivedId = wire.id
        if (modeStateMachine.currentState == DeafblindModeState.WAITING_MESSAGE) {
            modeStateMachine.transitionTo(DeafblindModeState.RECEIVING_MESSAGE)
        }
        if (wire.id.isNotEmpty()) nearby.sendMessage(WireProtocol.encode(WireMessage.Received(wire.id)))
        playWithProgress(wire.text, wire.id)
    }

    /**
     * Vibra a mensagem e, no mesmo ritmo, atualiza a tela e avisa o Comunicador qual letra está vibrando.
     */
    private fun playWithProgress(text: String, id: String) {
        handler.removeCallbacksAndMessages(PROGRESS_TOKEN)
        val timeline = runCatching { timelineBuilder.buildTimeline(text) }.getOrNull()
        if (timeline == null) {
            // Texto que não dá para vibrar: avisa o Comunicador para ele não ficar parado em "Recebida".
            if (id.isNotEmpty()) nearby.sendMessage(WireProtocol.encode(WireMessage.Played(id)))
            return
        }

        playback.play(text)
        isPlaying = true
        val start = SystemClock.uptimeMillis()
        timeline.steps.forEach { step ->
            handler.postAtTime({
                playingIndex = step.index
                if (id.isNotEmpty()) nearby.sendMessage(WireProtocol.encode(WireMessage.Progress(id, step.index)))
            }, PROGRESS_TOKEN, start + step.startMs)
        }
        handler.postAtTime({
            isPlaying = false
            playingIndex = -1
            if (id.isNotEmpty()) nearby.sendMessage(WireProtocol.encode(WireMessage.Played(id)))
        }, PROGRESS_TOKEN, start + timeline.totalMs)
    }

    /** Toque na área de gestos durante a vibração: interrompe. */
    private fun stopPlayback() {
        handler.removeCallbacksAndMessages(PROGRESS_TOKEN)
        playback.stop()
        isPlaying = false
        playingIndex = -1
        if (lastReceivedId.isNotEmpty()) nearby.sendMessage(WireProtocol.encode(WireMessage.Played(lastReceivedId)))
        echo = CommandEcho("Vibração interrompida", ok = true)
    }

    // ───────── NearbyListener ─────────

    override fun onMessageReceived(message: String) {
        val wire = WireProtocol.decode(message)
        if (wire is WireMessage.Text) onIncomingText(wire)
    }

    override fun onConnectionStatusChanged(status: String) = Unit

    override fun onConnectionStateChanged(state: ConnectionState) {
        connectionState = state
        when (state) {
            ConnectionState.CONNECTED -> {
                peerName = nearby.getConnectedEndpointName()
                modeStateMachine.transitionTo(DeafblindModeState.CONNECTED)
                modeStateMachine.transitionTo(DeafblindModeState.WAITING_MESSAGE)
            }
            ConnectionState.CONNECTION_ERROR -> nearby.stop() // vai para DISCONNECTED, tratado abaixo
            ConnectionState.DISCONNECTED -> {
                peerName = null
                handler.removeCallbacksAndMessages(PROGRESS_TOKEN)
                isPlaying = false
                playingIndex = -1
                modeStateMachine.transitionTo(DeafblindModeState.SEARCHING)
                if (active && permitted) {
                    handler.postDelayed({
                        if (active && nearby.getCurrentState() == ConnectionState.DISCONNECTED) {
                            nearby.startDiscovery(localName)
                        }
                    }, RECONNECT_DELAY_MS)
                }
            }
            else -> Unit
        }
        // Vibração de sistema para cada etapa da conexão (procurando, encontrado, conectado...).
        ConnectionFeedbackMapper.mapConnectionStateToFeedback(state)?.let { feedback(it) }
    }

    fun release() {
        active = false
        handler.removeCallbacksAndMessages(null)
        playback.stop()
        nearby.stop()
    }

    companion object {
        private val PROGRESS_TOKEN = Any()
        private const val RECONNECT_DELAY_MS = 2000L
        private const val REPEAT_DELAY_MS = 450L // deixa o pulso de "comando reconhecido" terminar
    }
}
