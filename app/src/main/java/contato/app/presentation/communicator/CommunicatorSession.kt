package contato.app.presentation.communicator

import android.content.Context
import androidx.compose.runtime.derivedStateOf
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import contato.app.core.config.HapticConfig
import contato.app.core.connection.NearbyConnectionService
import contato.app.core.connection.NearbyListener
import contato.app.core.vibration.HapticMessagePlaybackService
import contato.app.domain.model.Conversation
import contato.app.domain.model.Message
import contato.app.domain.model.MessageStatus
import contato.app.domain.model.UserMode
import contato.app.domain.model.WireMessage
import contato.app.domain.repository.CommunicatorProfileRepository
import contato.app.domain.repository.ConversationArchive
import contato.app.domain.service.BrailleTextNormalizer
import contato.app.domain.service.ConversationMerger
import contato.app.domain.service.WireProtocol
import contato.app.domain.state.ConnectionState
import contato.app.domain.state.ConversationState
import contato.app.domain.state.ConversationStateMachine
import contato.app.presentation.common.CloudAccount
import java.util.UUID

/** Em que letra o celular do Surdocego está agora (texto já normalizado: A-Z e espaço). */
internal data class PlaybackProgress(val index: Int, val text: String) {
    val total: Int get() = text.length
    val character: Char? get() = text.getOrNull(index)
}

internal enum class BannerKind { SUCCESS, ERROR }

/** Aviso no topo da tela (conectado, conexão perdida...). [key] força reexibir avisos iguais. */
internal data class Banner(
    val kind: BannerKind,
    val title: String,
    val message: String,
    val key: Long = System.nanoTime(),
)

/**
 * Estado e regras da sessão do Comunicador: conexão, conversa, status de cada mensagem
 * e preferências. As telas Compose só leem estes campos e chamam estas funções.
 *
 * Não é um ViewModel de propósito (o projeto não usa a biblioteca de ViewModel). Vive
 * enquanto a CommunicatorActivity vive; a Activity não é recriada ao girar a tela
 * (configChanges no Manifest), então a conexão também não cai por isso.
 */
internal class CommunicatorSession(
    context: Context,
    private val profile: CommunicatorProfileRepository,
    private val archive: ConversationArchive,
    val account: CloudAccount,
) : NearbyListener {

    private val nearby = NearbyConnectionService(context, this)
    private val playback = HapticMessagePlaybackService(context)
    private val conversation = ConversationStateMachine()
    private val handler = Handler(Looper.getMainLooper())

    var connectionState by mutableStateOf(ConnectionState.DISCONNECTED)
        private set
    var peerName by mutableStateOf<String?>(null)
        private set
    var displayName by mutableStateOf(profile.getDisplayName())
        private set
    var historyEnabled by mutableStateOf(profile.isHistoryEnabled())
        private set
    var autoSearchEnabled by mutableStateOf(profile.isAutoSearchEnabled())
        private set
    var banner by mutableStateOf<Banner?>(null)

    /** Já conectou alguma vez nesta sessão? (para não voltar à tela de conexão sozinho). */
    var hasConnected by mutableStateOf(false)
        private set

    /** Todas as conversas (uma por aparelho), mais recente primeiro. */
    val conversations = mutableStateListOf<Conversation>()

    /** Conversa do aparelho conectado agora (ou do último conectado). */
    var activeConversationId by mutableStateOf<String?>(null)
        private set

    /** Mensagens da conversa ativa, atualizadas ao vivo (status, letra vibrando...). */
    val messages = mutableStateListOf<Message>()
    val progress = mutableStateMapOf<String, PlaybackProgress>()

    /** Total de mensagens não lidas (bolinha na aba Conversas). */
    val unreadTotal by derivedStateOf { conversations.sumOf { it.unreadCount(UserMode.COMMUNICATOR) } }

    private var started = false
    private var autoReconnect = true

    init {
        if (historyEnabled) conversations.addAll(archive.loadAll().sortedByDescending { it.updatedAt })
        if (account.isSignedIn && historyEnabled) pullCloud(pushAfter = false)
    }

    // ───────── Conversas ─────────

    fun conversation(id: String): Conversation? = conversations.firstOrNull { it.id == id }

    /** Mensagens para a tela de uma conversa: a ativa vem ao vivo, as outras do histórico. */
    fun messagesOf(id: String): List<Message> =
        if (id == activeConversationId) messages else conversation(id)?.messages.orEmpty()

    /** Só dá para escrever na conversa do aparelho que está conectado agora. */
    fun canWriteTo(id: String): Boolean =
        id == activeConversationId && connectionState == ConnectionState.CONNECTED

    fun markRead(id: String) {
        val i = conversations.indexOfFirst { it.id == id }
        if (i < 0) return
        val c = conversations[i]
        val last = c.lastMessage?.timestamp ?: return
        if (c.lastReadAt >= last) return
        conversations[i] = c.copy(lastReadAt = last)
        persist(conversations[i], emptyList())
    }

    /** Ao conectar: abre (ou cria) a conversa com aquele aparelho. */
    private fun activateConversationFor(peer: String) {
        val key = ConversationMerger.peerKey(peer)
        val existing = conversations.firstOrNull { ConversationMerger.peerKey(it.peerName) == key }
        val conversation = existing ?: Conversation(
            id = UUID.randomUUID().toString(),
            peerName = peer,
            messages = emptyList(),
            updatedAt = System.currentTimeMillis(),
        ).also { conversations.add(0, it) }
        activeConversationId = conversation.id
        messages.clear()
        messages.addAll(conversation.messages)
        progress.clear()
    }

    /** Copia a conversa ativa para a lista, salva no aparelho e manda o que mudou para a nuvem. */
    private fun commitActive(changed: List<Message>) {
        val id = activeConversationId ?: return
        val i = conversations.indexOfFirst { it.id == id }
        if (i < 0) return
        val updated = conversations[i].copy(
            messages = messages.toList(),
            updatedAt = messages.lastOrNull()?.timestamp ?: conversations[i].updatedAt,
        )
        conversations.removeAt(i)
        conversations.add(0, updated)
        persist(updated, changed)
    }

    private fun persist(conversation: Conversation, changed: List<Message>) {
        if (!historyEnabled) return
        archive.saveAll(conversations.toList())
        // Status intermediário (letra a letra) não vai para a nuvem: economiza gravações.
        val relevant = changed.filter { it.status != MessageStatus.PLAYING && it.status != MessageStatus.SENDING }
        account.push(conversation, relevant)
    }

    // ───────── Conta (Firebase) ─────────

    /** "Continuar com Google". Ao entrar, junta as conversas do aparelho com as da nuvem. */
    fun signIn(activityContext: Context) {
        account.launchSignIn(activityContext) {
            // Já tinha conta: traz nome e preferências salvos; depois grava o estado atual.
            account.fetchProfile { cloud ->
                if (cloud != null) {
                    if (cloud.displayName.isNotBlank()) rename(cloud.displayName)
                    cloud.preferences["autoSearch"]?.toBooleanStrictOrNull()?.let { if (it != autoSearchEnabled) setAutoSearch(it) }
                }
                syncProfile()
            }
            if (!historyEnabled) setHistory(true) // entrar para salvar já liga o "Guardar conversas"
            pullCloud(pushAfter = true)
            banner = Banner(BannerKind.SUCCESS, "Conta conectada", "Suas conversas agora ficam salvas na nuvem.")
        }
    }

    fun signOut(activityContext: Context) {
        account.launchSignOut(activityContext) {
            banner = Banner(BannerKind.SUCCESS, "Você saiu da conta", "As conversas continuam neste aparelho.")
        }
    }

    private fun pullCloud(pushAfter: Boolean) {
        account.pull { remote ->
            val activePeer = conversation(activeConversationId ?: "")?.peerName
            val merged = ConversationMerger.merge(conversations.toList(), remote)
            conversations.clear()
            conversations.addAll(merged)
            // A conversa ativa pode ter ganhado o id da nuvem e mensagens antigas.
            if (activePeer != null) {
                val key = ConversationMerger.peerKey(activePeer)
                merged.firstOrNull { ConversationMerger.peerKey(it.peerName) == key }?.let { c ->
                    activeConversationId = c.id
                    val live = messages.associateBy { it.id }
                    messages.clear()
                    messages.addAll(ConversationMerger.mergeMessages(c.messages, live.values.toList()))
                }
            }
            archive.saveAll(merged)
            if (pushAfter) merged.forEach { account.push(it, it.messages) }
        }
    }

    // ───────── Conexão ─────────

    /** Chamado quando as permissões estão concedidas. Procura sozinho se a busca automática estiver ligada. */
    fun start() {
        if (started) return
        started = true
        autoReconnect = autoSearchEnabled
        if (autoSearchEnabled) nearby.startAdvertising(displayName)
    }

    /** Modo de pareamento ("Parear agora"): recomeça a busca do zero, mesmo com a busca automática desligada. */
    fun searchAgain() {
        autoReconnect = true
        handler.removeCallbacksAndMessages(RECONNECT_TOKEN)
        if (nearby.getCurrentState() != ConnectionState.DISCONNECTED) nearby.stop()
        nearby.startAdvertising(displayName)
    }

    /** Botão "Desconectar": encerra e NÃO volta a procurar sozinho. */
    fun disconnect() {
        autoReconnect = false
        handler.removeCallbacksAndMessages(RECONNECT_TOKEN)
        nearby.stop()
    }

    /** Fim da Activity. */
    fun release() {
        account.release()
        started = false
        autoReconnect = false
        handler.removeCallbacksAndMessages(null)
        playback.stop()
        nearby.stop()
    }

    private fun scheduleReconnect() {
        if (!started || !autoReconnect) return
        handler.removeCallbacksAndMessages(RECONNECT_TOKEN)
        handler.postAtTime({
            if (started && autoReconnect && nearby.getCurrentState() == ConnectionState.DISCONNECTED) {
                nearby.startAdvertising(displayName)
            }
        }, RECONNECT_TOKEN, android.os.SystemClock.uptimeMillis() + RECONNECT_DELAY_MS)
    }

    // ───────── Mensagens ─────────

    /** Envia uma mensagem. Devolve false se não foi possível (sem conexão, sem letras). */
    fun send(raw: String): Boolean {
        val text = raw.trim().take(MAX_LENGTH)
        if (text.isEmpty() || connectionState != ConnectionState.CONNECTED) return false
        val normalized = BrailleTextNormalizer.normalizeForBraille(text)
        if (normalized.isEmpty()) {
            banner = Banner(BannerKind.ERROR, "Mensagem sem letras", "O ConTato vibra apenas letras de A a Z.")
            return false
        }
        // Raro, mas "ß" vira "SS": o texto que vibra pode ficar maior que o digitado.
        if (normalized.length > MAX_LENGTH) {
            banner = Banner(BannerKind.ERROR, "Mensagem longa demais", "Encurte um pouco a mensagem.")
            return false
        }

        // Uma nova mensagem pode sair mesmo enquanto se espera resposta da anterior.
        if (conversation.currentState == ConversationState.WAITING_RESPONSE) {
            conversation.transitionTo(ConversationState.IDLE)
        }
        conversation.transitionTo(ConversationState.COMPOSING)
        conversation.transitionTo(ConversationState.PROCESSING)
        conversation.transitionTo(ConversationState.SENDING)

        val message = Message(
            id = UUID.randomUUID().toString(),
            content = text,
            sender = UserMode.COMMUNICATOR,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING,
        )
        messages += message
        nearby.sendMessage(WireProtocol.encode(WireMessage.Text(message.id, text)))
        updateStatus(message.id, MessageStatus.SENT)

        conversation.transitionTo(ConversationState.SENT)
        conversation.transitionTo(ConversationState.WAITING_RESPONSE)
        return true
    }

    /** Matriz Braille: deixa o Comunicador sentir a vibração de uma letra no próprio celular. */
    fun previewLetter(letter: Char) {
        playback.play(letter.toString())
    }

    private fun updateStatus(id: String, status: MessageStatus) {
        val i = messages.indexOfFirst { it.id == id }
        if (i < 0) return
        messages[i] = messages[i].copy(status = status)
        commitActive(listOf(messages[i]))
    }

    private fun onIncomingText(wire: WireMessage.Text) {
        // A mesma mensagem nunca entra duas vezes (a lista usa o id como chave).
        if (wire.id.isNotEmpty() && messages.any { it.id == wire.id }) return
        conversation.transitionTo(ConversationState.RECEIVING)
        val incoming = Message(
            id = wire.id.ifEmpty { UUID.randomUUID().toString() },
            content = wire.text,
            sender = UserMode.DEAFBLIND,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.DELIVERED,
        )
        messages += incoming
        commitActive(listOf(incoming))
        conversation.transitionTo(ConversationState.RECEIVED)
        conversation.transitionTo(ConversationState.IDLE)
    }

    private fun onReceived(id: String) {
        val current = messages.firstOrNull { it.id == id }?.status ?: return
        if (current.ordinal < MessageStatus.DELIVERED.ordinal) updateStatus(id, MessageStatus.DELIVERED)
    }

    private fun onProgress(id: String, index: Int) {
        val message = messages.firstOrNull { it.id == id } ?: return
        val text = BrailleTextNormalizer.normalizeForBraille(message.content)
        progress[id] = PlaybackProgress(index, text)
        if (message.status != MessageStatus.PLAYING) updateStatus(id, MessageStatus.PLAYING)
    }

    private fun onPlayed(id: String) {
        progress.remove(id)
        updateStatus(id, MessageStatus.PLAYED)
    }

    /** Conexão caiu: o que não foi confirmado vira "não enviada"; o que vibrava fica como entregue. */
    private fun markInFlightAfterDisconnect() {
        val changed = mutableListOf<Message>()
        for (i in messages.indices) {
            val m = messages[i]
            val next = when (m.status) {
                MessageStatus.SENDING, MessageStatus.SENT -> MessageStatus.FAILED
                MessageStatus.PLAYING -> MessageStatus.DELIVERED
                else -> null
            } ?: continue
            messages[i] = m.copy(status = next)
            changed += messages[i]
        }
        progress.clear()
        if (changed.isNotEmpty()) commitActive(changed)
    }

    // ───────── Perfil e preferências ─────────

    fun rename(name: String) {
        val clean = name.trim()
        if (clean.isEmpty()) return
        profile.saveDisplayName(clean)
        displayName = profile.getDisplayName()
        syncProfile()
        // Se ainda está procurando, recomeça para o celular tátil ver o nome novo.
        if (nearby.getCurrentState() == ConnectionState.SCANNING) {
            nearby.stop()
            nearby.startAdvertising(displayName)
        }
    }

    fun setAutoSearch(enabled: Boolean) {
        profile.setAutoSearchEnabled(enabled)
        autoSearchEnabled = enabled
        autoReconnect = enabled
        if (enabled) {
            if (started && nearby.getCurrentState() == ConnectionState.DISCONNECTED) nearby.startAdvertising(displayName)
        } else {
            handler.removeCallbacksAndMessages(RECONNECT_TOKEN)
        }
        syncProfile()
    }

    fun setHistory(enabled: Boolean) {
        profile.setHistoryEnabled(enabled)
        historyEnabled = enabled
        if (enabled) archive.saveAll(conversations.toList()) else archive.clear()
        syncProfile()
    }

    /** Nome e preferências do Comunicador na nuvem (só se estiver logado). */
    private fun syncProfile() {
        account.saveProfile(
            displayName,
            mapOf("historyEnabled" to historyEnabled.toString(), "autoSearch" to autoSearchEnabled.toString()),
        )
    }

    /** Apaga tudo deste aparelho e, se estiver logado, também da nuvem. */
    fun clearHistory() {
        val keepActive = conversation(activeConversationId ?: "")?.copy(messages = emptyList(), lastReadAt = 0L)
        messages.clear()
        progress.clear()
        conversations.clear()
        // A conversa com o aparelho conectado continua existindo (vazia), para seguir conversando.
        if (keepActive != null && connectionState == ConnectionState.CONNECTED) conversations.add(keepActive)
        else activeConversationId = null
        archive.clear()
        if (account.isSignedIn) {
            account.deleteCloudData { ok ->
                if (!ok) banner = Banner(BannerKind.ERROR, "Nuvem não apagada", "Confira a internet e tente de novo.")
            }
        }
    }

    // ───────── NearbyListener ─────────

    override fun onMessageReceived(message: String) {
        when (val wire = WireProtocol.decode(message)) {
            is WireMessage.Text -> onIncomingText(wire)
            is WireMessage.Received -> onReceived(wire.id)
            is WireMessage.Progress -> onProgress(wire.id, wire.index)
            is WireMessage.Played -> onPlayed(wire.id)
            null -> Unit
        }
    }

    override fun onConnectionStatusChanged(status: String) {
        // Texto livre, só para debug. A tela usa o estado formal abaixo.
    }

    override fun onConnectionStateChanged(state: ConnectionState) {
        val wasConnected = connectionState == ConnectionState.CONNECTED
        connectionState = state

        when (state) {
            ConnectionState.CONNECTED -> {
                peerName = nearby.getConnectedEndpointName()?.takeIf { it.isNotBlank() } ?: DEFAULT_PEER
                activateConversationFor(peerName ?: DEFAULT_PEER)
                hasConnected = true
                autoReconnect = autoSearchEnabled // depois de um pareamento manual, volta à preferência
                banner = Banner(BannerKind.SUCCESS, "Conectado", "Você está conectado com $peerName.")
            }
            ConnectionState.CONNECTION_ERROR -> {
                banner = Banner(BannerKind.ERROR, "Não foi possível conectar", "Tentando de novo.")
                nearby.stop() // vai para DISCONNECTED, que agenda a nova busca
            }
            ConnectionState.DISCONNECTED -> {
                if (wasConnected) {
                    markInFlightAfterDisconnect()
                    banner = Banner(
                        BannerKind.ERROR, "Conexão perdida",
                        if (autoReconnect) "Procurando ${peerName ?: DEFAULT_PEER} de novo." else "Para conectar de novo, use Parear agora no Perfil.",
                    )
                }
                peerName = null
                scheduleReconnect()
            }
            else -> Unit
        }
    }

    companion object {
        const val MAX_LENGTH = HapticConfig.MAX_MESSAGE_LENGTH
        /** Nome usado quando o outro aparelho não informa o dele. */
        const val DEFAULT_PEER = "Celular tátil"
        private const val RECONNECT_DELAY_MS = 1500L
        private val RECONNECT_TOKEN = Any()
    }
}
