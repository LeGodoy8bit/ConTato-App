package contato.app.presentation.deafblind

import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import contato.app.core.connection.NearbyConnectionService
import contato.app.core.connection.NearbyListener
import contato.app.core.vibration.HapticMessagePlaybackService
import contato.app.domain.model.Conversation
import contato.app.domain.model.DeafblindSettings
import contato.app.domain.model.Message
import contato.app.domain.model.MessageStatus
import contato.app.domain.model.SystemFeedbackEvent
import contato.app.domain.model.UserMode
import contato.app.domain.model.WireMessage
import contato.app.domain.service.BrailleMapper
import contato.app.domain.service.BrailleTextNormalizer
import contato.app.domain.service.ConnectionFeedbackMapper
import contato.app.domain.service.ConversationMerger
import contato.app.domain.service.DeafblindSettingsCodec
import contato.app.domain.service.HapticWaveformTuner
import contato.app.domain.service.HapticTimelineBuilder
import contato.app.domain.service.MessageBuilder
import contato.app.domain.service.SystemHapticPatterns
import contato.app.domain.service.WireProtocol
import contato.app.domain.state.ConnectionState
import contato.app.domain.state.DeafblindModeState
import contato.app.domain.state.DeafblindModeStateMachine
import contato.app.domain.repository.ConversationArchive
import contato.app.domain.repository.DeafblindSettingsRepository
import contato.app.presentation.common.CloudAccount
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
internal class DeafblindSession(
    context: Context,
    private val settingsRepository: DeafblindSettingsRepository,
    private val archive: ConversationArchive,
    val account: CloudAccount,
) : NearbyListener {

    private val nearby = NearbyConnectionService(context, this)
    private val playback = HapticMessagePlaybackService(context)
    private val modeStateMachine = DeafblindModeStateMachine()
    private val messageBuilder = MessageBuilder()
    private val timelineBuilder = HapticTimelineBuilder()
    private val handler = Handler(Looper.getMainLooper())

    /** Nome padrão deste celular para o Comunicador: "Celular tátil (modelo)". */
    val defaultName = "${NearbyConnectionService.DEFAULT_DISCOVERY_NAME} (${Build.MODEL.take(18)})"

    /** Como este celular aparece para o Comunicador: o nome escolhido nos Ajustes, ou o padrão. */
    val localName: String get() = settings.displayName.ifBlank { defaultName }

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

    /** Preferências (vibração, texto, tema, dicas). */
    var settings by mutableStateOf(settingsRepository.load())
        private set

    /** Histórico: uma conversa por comunicador, como no Comunicador. */
    val conversations = mutableStateListOf<Conversation>()
    var activeConversationId by mutableStateOf<String?>(null)
        private set
    val unreadTotal by derivedStateOf { conversations.sumOf { it.unreadCount(UserMode.DEAFBLIND) } }

    private var lastReceivedId = ""
    private var active = false
    private var permitted = false

    init {
        playback.tuning = settings.tuning
        if (settings.saveHistory) conversations.addAll(ConversationMerger.merge(archive.loadAll(), emptyList()))
        if (account.isSignedIn && settings.saveHistory) pullCloud(pushAfter = false)
    }

    // ───────── Ajustes ─────────

    /** Salva e aplica na hora (a amostra de vibração é tocada pela tela, ver [previewVibration]). */
    fun updateSettings(new: DeafblindSettings) {
        val old = settings
        val next = new.normalized()
        if (next == old) return
        settings = next
        settingsRepository.save(next)
        playback.tuning = next.tuning
        if (next.saveHistory != old.saveHistory) {
            if (next.saveHistory) archive.saveAll(conversations.toList()) else archive.clear()
        }
        if (next.displayName != old.displayName) announceName()
        account.saveProfile(localName, DeafblindSettingsCodec.toMap(next))
    }

    /** Vibra a letra A com a força e o ritmo atuais (ao soltar o controle ou tocar num atalho). */
    fun previewVibration() {
        if (!isPlaying) playback.play("A")
    }

    /**
     * O nome mudou: conectado, avisa o Comunicador na hora; procurando, recomeça a busca
     * para o novo nome já aparecer na lista dele.
     */
    private fun announceName() {
        when (connectionState) {
            ConnectionState.CONNECTED -> nearby.sendMessage(WireProtocol.encode(WireMessage.Profile(localName)))
            ConnectionState.SCANNING -> if (active && permitted) {
                nearby.stop()
                nearby.startDiscovery(localName)
            }
            else -> Unit // conectando: o nome novo vai na mensagem Profile assim que conectar
        }
    }

    // ───────── Treino ─────────

    /** Vibra uma letra (treino e "testar vibração"), com a força e o ritmo escolhidos. */
    fun playLetter(letter: Char) {
        if (isPlaying) return
        playback.play(letter.toString())
    }

    /** Vibra um texto inteiro (palavras e frases do treino). */
    fun playText(text: String) {
        if (isPlaying) return
        playback.play(text)
    }

    /** Vibração de comando aceito / recusado (confirmar letra, espaço, apagar no treino). */
    fun commandFeedback(ok: Boolean) = feedback(if (ok) SystemFeedbackEvent.COMMAND_RECOGNIZED else SystemFeedbackEvent.ERROR)

    /** Vibração curta de ponto marcado/desmarcado, para a cela do treino. */
    fun dotFeedback(on: Boolean) = feedback(if (on) SystemFeedbackEvent.DOT_ON else SystemFeedbackEvent.DOT_OFF)

    fun trainingResult(correct: Boolean) =
        feedback(if (correct) SystemFeedbackEvent.MESSAGE_FINISHED else SystemFeedbackEvent.ERROR)

    // ───────── Histórico e conta ─────────

    fun markRead(id: String) {
        val i = conversations.indexOfFirst { it.id == id }
        if (i < 0) return
        val c = conversations[i]
        val last = c.lastMessage?.timestamp ?: return
        if (c.lastReadAt >= last) return
        conversations[i] = c.copy(lastReadAt = last)
        persist(conversations[i], emptyList())
    }

    private fun activateConversationFor(peer: String) {
        val key = ConversationMerger.peerKey(peer)
        val existing = conversations.firstOrNull { ConversationMerger.peerKey(it.peerName) == key }
        activeConversationId = (existing ?: Conversation(
            id = UUID.randomUUID().toString(),
            peerName = peer,
            messages = emptyList(),
            updatedAt = System.currentTimeMillis(),
        ).also { conversations.add(0, it) }).id
    }

    private fun record(message: Message) {
        val id = activeConversationId ?: return
        val i = conversations.indexOfFirst { it.id == id }
        if (i < 0) return
        val c = conversations[i]
        val updated = c.copy(
            messages = (c.messages.filter { it.id != message.id } + message).takeLast(Conversation.MAX_MESSAGES),
            updatedAt = message.timestamp,
        )
        conversations.removeAt(i)
        conversations.add(0, updated)
        persist(updated, listOf(message))
    }

    private fun persist(conversation: Conversation, changed: List<Message>) {
        if (!settings.saveHistory) return
        archive.saveAll(conversations.toList())
        account.push(conversation, changed)
    }

    /** Login feito pela pessoa de apoio. Ao entrar, junta o histórico do aparelho com o da nuvem. */
    fun signIn(activityContext: Context) {
        account.launchSignIn(activityContext) {
            // Já tinha conta: aplica as preferências salvas (vibração, texto, cores) neste celular.
            account.fetchProfile { cloud ->
                var next = if (cloud != null) DeafblindSettingsCodec.fromMap(cloud.preferences, settings) else settings
                // Sem nome escolhido: usa o primeiro nome da conta Google (ex.: "Ana").
                if (next.displayName.isBlank()) {
                    account.user?.name?.substringBefore(' ')?.takeIf { it.isNotBlank() }?.let { next = next.copy(displayName = it) }
                }
                updateSettings(next)
                account.saveProfile(localName, DeafblindSettingsCodec.toMap(settings))
            }
            if (!settings.saveHistory) updateSettings(settings.copy(saveHistory = true))
            pullCloud(pushAfter = true)
            feedback(SystemFeedbackEvent.CONNECTED)
        }
    }

    fun signOut(activityContext: Context) = account.launchSignOut(activityContext)

    fun clearHistory() {
        conversations.clear()
        activeConversationId = null
        if (connectionState == ConnectionState.CONNECTED) {
            activateConversationFor(peerName?.takeIf { it.isNotBlank() } ?: DEFAULT_PEER)
        }
        archive.clear()
        if (account.isSignedIn) account.deleteCloudData { }
    }

    private fun pullCloud(pushAfter: Boolean) {
        account.pull { remote ->
            val activePeer = conversations.firstOrNull { it.id == activeConversationId }?.peerName
            val merged = ConversationMerger.merge(conversations.toList(), remote)
            conversations.clear()
            conversations.addAll(merged)
            if (activePeer != null) {
                val key = ConversationMerger.peerKey(activePeer)
                activeConversationId = merged.firstOrNull { ConversationMerger.peerKey(it.peerName) == key }?.id
            }
            archive.saveAll(merged)
            if (pushAfter) merged.forEach { account.push(it, it.messages) }
        }
    }

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

    /** Deslizar para cima: envia a resposta. */
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
        val replyId = UUID.randomUUID().toString()
        nearby.sendMessage(WireProtocol.encode(WireMessage.Text(replyId, text)))
        // DELIVERED: o Nearby entregou ao comunicador (SENT viraria "falhou" ao reabrir o histórico).
        record(Message(replyId, text, UserMode.DEAFBLIND, System.currentTimeMillis(), MessageStatus.DELIVERED))
        feedback(SystemFeedbackEvent.MESSAGE_SENT)
        echo = CommandEcho("Resposta enviada", ok = true)
        messageBuilder.clear()
        composed = ""
        clearPoints()
        modeStateMachine.transitionTo(DeafblindModeState.RETURNING_TO_WAIT)
        modeStateMachine.transitionTo(DeafblindModeState.WAITING_MESSAGE)
    }

    /** Deslizar para baixo: repete a última mensagem (o Comunicador vê vibrando de novo). */
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

    /**
     * A tela girou 180° (celular de ponta-cabeça) ou voltou. A cela gira junto, então os pontos
     * continuam no mesmo lugar para a mão; a vibração só avisa que girou.
     * Durante uma mensagem não interrompe a leitura: guarda e vibra assim que ela terminar.
     */
    fun onScreenFlipped(upsideDown: Boolean) {
        if (isPlaying) pendingFlip = upsideDown else flipFeedback(upsideDown)
    }

    private var pendingFlip: Boolean? = null

    private fun playPendingFlip() {
        val flip = pendingFlip ?: return
        pendingFlip = null
        flipFeedback(flip)
    }

    private fun flipFeedback(upsideDown: Boolean) {
        feedback(if (upsideDown) SystemFeedbackEvent.SCREEN_UPSIDE_DOWN else SystemFeedbackEvent.SCREEN_UPRIGHT)
        // Também na tela (pessoa de apoio e leitor de tela), não só na vibração.
        echo = CommandEcho(if (upsideDown) "Tela de ponta-cabeça" else "Tela na posição normal", ok = true)
    }

    private fun feedback(event: SystemFeedbackEvent) {
        // Uma vibração nova substitui a que está tocando: durante uma mensagem, nenhum aviso
        // (toque sem querer num ponto, mudança de conexão) pode cortar a leitura.
        if (isPlaying) return
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
        record(Message(wire.id.ifEmpty { UUID.randomUUID().toString() }, wire.text, UserMode.COMMUNICATOR,
            System.currentTimeMillis(), MessageStatus.DELIVERED))
        activeConversationId?.let(::markRead) // a pessoa sente a mensagem na hora: já conta como lida
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
        // Os tempos acompanham a velocidade escolhida nos Ajustes (lenta/normal/rápida).
        val tuning = settings.tuning
        timeline.steps.forEach { step ->
            handler.postAtTime({
                playingIndex = step.index
                if (id.isNotEmpty()) nearby.sendMessage(WireProtocol.encode(WireMessage.Progress(id, step.index)))
            }, PROGRESS_TOKEN, start + HapticWaveformTuner.scaleTime(step.startMs, tuning))
        }
        handler.postAtTime({
            isPlaying = false
            playingIndex = -1
            if (id.isNotEmpty()) nearby.sendMessage(WireProtocol.encode(WireMessage.Played(id)))
            playPendingFlip()
        }, PROGRESS_TOKEN, start + HapticWaveformTuner.scaleTime(timeline.totalMs, tuning))
    }

    /** Toque na área de gestos durante a vibração: interrompe. */
    private fun stopPlayback() {
        handler.removeCallbacksAndMessages(PROGRESS_TOKEN)
        playback.stop()
        isPlaying = false
        playingIndex = -1
        if (lastReceivedId.isNotEmpty()) nearby.sendMessage(WireProtocol.encode(WireMessage.Played(lastReceivedId)))
        echo = CommandEcho("Vibração interrompida", ok = true)
        playPendingFlip()
    }

    // ───────── NearbyListener ─────────

    override fun onMessageReceived(message: String) {
        when (val wire = WireProtocol.decode(message)) {
            is WireMessage.Text -> onIncomingText(wire)
            is WireMessage.Profile -> onPeerName(wire.name)
            else -> Unit
        }
    }

    /** O Comunicador avisou o nome dele (ao conectar ou depois de trocar). */
    private fun onPeerName(name: String) {
        peerName = name
        val id = activeConversationId ?: return
        val i = conversations.indexOfFirst { it.id == id }
        if (i < 0 || conversations[i].peerName == name) return
        // Já existia conversa com esse nome? Junta as duas (não deixa duas iguais na lista).
        val before = conversations[i]
        val (list, newId) = ConversationMerger.renameActive(conversations.toList(), id, name)
        // A absorvida pode já estar na nuvem com o nome antigo: renomeia lá também.
        if (newId != id && settings.saveHistory) account.push(before.copy(peerName = name), emptyList())
        conversations.clear()
        conversations.addAll(list)
        activeConversationId = newId
        val active = list.first { it.id == newId }
        persist(active, if (newId != id) active.messages else emptyList())
    }

    override fun onConnectionStatusChanged(status: String) = Unit

    override fun onConnectionStateChanged(state: ConnectionState) {
        connectionState = state
        when (state) {
            ConnectionState.CONNECTED -> {
                peerName = nearby.getConnectedEndpointName()
                activateConversationFor(peerName?.takeIf { it.isNotBlank() } ?: DEFAULT_PEER)
                nearby.sendMessage(WireProtocol.encode(WireMessage.Profile(localName)))
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
        account.release()
        active = false
        handler.removeCallbacksAndMessages(null)
        playback.stop()
        nearby.stop()
    }

    companion object {
        private val PROGRESS_TOKEN = Any()
        private const val DEFAULT_PEER = "Comunicador"
        private const val RECONNECT_DELAY_MS = 2000L
        private const val REPEAT_DELAY_MS = 450L // deixa o pulso de "comando reconhecido" terminar
    }
}
