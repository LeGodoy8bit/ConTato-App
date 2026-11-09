package contato.app.presentation.communicator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.domain.model.Message
import contato.app.domain.model.MessageStatus
import contato.app.domain.model.MessageStyle
import contato.app.domain.model.UserMode
import contato.app.domain.state.ConnectionState
import contato.app.domain.service.SimplificationRules
import contato.app.domain.service.TimeEstimates
import contato.app.domain.repository.SimplifyResult
import contato.app.presentation.theme.Motion
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import contato.app.presentation.common.Mascot
import contato.app.presentation.common.MascotMood
import contato.app.presentation.theme.ContatoIcons
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ───────────────────────── Conversa aberta ─────────────────────────

/** Item da lista: separador de dia ("Hoje") ou balão. */
private sealed interface ChatItem {
    val key: String
    /** [position] entra na chave: dois "12 de outubro" de anos diferentes não repetem a chave. */
    data class Day(val label: String, val position: Int) : ChatItem { override val key get() = "day-$position-$label" }
    data class Bubble(val message: Message) : ChatItem { override val key get() = message.id }
}

/**
 * Uma conversa aberta (referência de chat): voltar e nome no topo, "Hoje" numa etiqueta,
 * balões com hora e vistos embaixo, campo com "+" (frases rápidas) e botão de enviar redondo.
 * Cor dos balões e tamanho do texto vêm de Ajustes → Estilo das mensagens.
 * Conversas antigas (de um aparelho que não está conectado) abrem só para leitura.
 */
@Composable
internal fun ChatDetail(
    session: CommunicatorSession,
    conversationId: String,
    c: CommColors,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
) {
    val conversation = session.conversation(conversationId)
    val messages = session.messagesOf(conversationId)
    val isActive = conversationId == session.activeConversationId
    val canWrite = session.canWriteTo(conversationId)
    val listState = rememberLazyListState()
    val style = session.messageStyle

    val items = remember(messages.toList()) {
        buildList<ChatItem> {
            var lastDay = ""
            messages.forEach { m ->
                val day = dayLabel(m.timestamp)
                if (day != lastDay) { add(ChatItem.Day(day, size)); lastDay = day }
                add(ChatItem.Bubble(m))
            }
        }
    }

    // Rola para a última mensagem e marca como lida quando chega ou sai uma nova.
    LaunchedEffect(conversationId, messages.size) {
        if (items.isNotEmpty()) listState.animateScrollToItem(items.lastIndex)
        session.markRead(conversationId)
    }

    Column(Modifier.fillMaxSize()) {
        ChatHeader(
            name = conversation?.peerName ?: session.peerName ?: CommunicatorSession.DEFAULT_PEER,
            state = if (isActive) session.connectionState else ConnectionState.DISCONNECTED,
            isActive = isActive,
            c = c,
            onBack = onBack,
            onOpenInfo = onOpenInfo,
        )

        AnimatedVisibility(visible = !canWrite) {
            if (isActive) OfflineBar(session.connectionState, c, onSearch = session::searchAgain)
            else ReadOnlyBar(conversation?.peerName ?: "este aparelho", c, onPair = session::searchAgain)
        }

        // Leitor de tela: anuncia a resposta que acabou de chegar (só as que chegam com a conversa aberta).
        val openedAt = remember(conversationId) { System.currentTimeMillis() }
        val newReply = messages.lastOrNull { it.sender != UserMode.COMMUNICATOR }?.takeIf { it.timestamp > openedAt }
        // Sempre presente (1 dp, invisível): o leitor de tela só anuncia quando o texto muda.
        Box(Modifier.size(1.dp).semantics {
            liveRegion = LiveRegionMode.Polite
            contentDescription = newReply?.let { "Nova resposta: ${it.content}" } ?: ""
        })

        Box(Modifier.weight(1f).fillMaxWidth().background(c.bg)) {
            if (messages.isEmpty()) {
                EmptyChat(c, Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items, key = { it.key }) { item ->
                        Box(Modifier.animateItem()) {
                            when (item) {
                                is ChatItem.Day -> DaySeparator(item.label, c)
                                is ChatItem.Bubble -> MessageBubble(item.message, session.progress[item.message.id], c, style)
                            }
                        }
                    }
                }
            }
        }

        if (isActive) {
            EstimateStrip(session.chatEstimate.takeIf { canWrite }, c)
            MessageInput(
                enabled = canWrite, c = c, style = style, onSend = session::send,
                canSimplify = session::canSimplify, onSimplify = session::simplify,
                feelEstimate = session::feelEstimateMs,
            )
        }
    }
}

@Composable
private fun ChatHeader(
    name: String,
    state: ConnectionState,
    isActive: Boolean,
    c: CommColors,
    onBack: () -> Unit,
    onOpenInfo: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(c.surface)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircleButton(ContatoIcons.ChevronLeft, "Voltar para as conversas", c, onClick = onBack)
            Spacer(Modifier.width(10.dp))
            PeerAvatar(name, 42.dp, c, online = isActive && state == ConnectionState.CONNECTED)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
                Text(name, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold))
                AnimatedContent(
                    targetState = when {
                        !isActive -> "Histórico"
                        state == ConnectionState.CONNECTED -> "Conectado"
                        else -> connectionLabel(state, null)
                    },
                    transitionSpec = { textSwapTransition() },
                    label = "headerStatus",
                ) { t ->
                    Text(t, color = if (t == "Conectado") c.online else c.muted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
                }
            }
            CircleButton(ContatoIcons.Info, "Como funciona e matriz Braille", c, onClick = onOpenInfo)
        }
        androidx.compose.material3.HorizontalDivider(color = c.border)
    }
}

@Composable
private fun CircleButton(icon: ImageVector, label: String, c: CommColors, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp) // área de toque mínima recomendada (48 × 48 dp)
            .clip(CircleShape)
            .border(1.dp, c.border, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = c.onSurface, modifier = Modifier.size(22.dp))
    }
}

/** "Hoje" numa etiqueta clara no meio (referência). */
@Composable
private fun DaySeparator(label: String, c: CommColors) {
    Box(Modifier.fillMaxWidth().padding(vertical = 6.dp), contentAlignment = Alignment.Center) {
        Text(
            label, color = c.muted,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(c.surface)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .semantics { heading() },
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium),
        )
    }
}

@Composable
private fun OfflineBar(state: ConnectionState, c: CommColors, onSearch: () -> Unit) {
    val searching = state != ConnectionState.DISCONNECTED && state != ConnectionState.CONNECTION_ERROR
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.searching.copy(alpha = 0.14f))
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            if (searching) "Procurando o celular tátil. As mensagens saem quando conectar."
            else "Sem conexão com o celular tátil.",
            color = c.onSurface,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        )
        if (!searching) {
            TextButton(onClick = onSearch, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(ContatoIcons.Refresh, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Procurar", color = c.accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Conversa antiga: avisa que para responder é preciso conectar com aquele aparelho. */
@Composable
private fun ReadOnlyBar(peer: String, c: CommColors, onPair: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.accentSoft)
            .padding(start = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            "Para continuar, conecte de novo com $peer.",
            color = c.onSurface,
            modifier = Modifier.weight(1f).padding(vertical = 10.dp),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
        )
        TextButton(onClick = onPair, modifier = Modifier.heightIn(min = 48.dp)) {
            Text("Parear", color = c.accent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun EmptyChat(c: CommColors, modifier: Modifier) {
    Column(modifier.padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Mascot(120.dp, mood = MascotMood.HAPPY)
        Spacer(Modifier.height(12.dp))
        Text("Diga oi!", color = c.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(6.dp))
        Text(
            "Prefira frases curtas: cada letra leva cerca de 2 segundos para vibrar.",
            color = c.muted,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        )
    }
}

// ───────────────────────── Balões ─────────────────────────

private val timeFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR"))

@Composable
private fun MessageBubble(message: Message, progress: PlaybackProgress?, c: CommColors, style: MessageStyle) {
    val mine = message.sender == UserMode.COMMUNICATOR
    val time = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }
    val bubble = if (mine) Color(style.bubble.argb) else c.received
    val textColor = if (mine) Color.White else c.onReceived
    val shape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp,
        bottomStart = if (mine) 20.dp else 6.dp,
        bottomEnd = if (mine) 6.dp else 20.dp,
    )
    // Balão + status lidos juntos pelo leitor de tela, dizendo quem escreveu.
    Column(
        Modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            stateDescription = if (mine) "Você" else "Resposta"
        },
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Box(
            Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(bubble)
                .then(if (mine || c.dark) Modifier else Modifier.border(1.dp, c.border, shape))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            // Enquanto vibra, a letra atual fica destacada dentro do próprio balão.
            if (mine && message.status == MessageStatus.PLAYING && progress != null) {
                ProgressText(progress, bubble, style.textSize.sp)
            } else {
                Text(message.content, color = textColor,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = style.textSize.sp.sp, lineHeight = (style.textSize.sp + 6).sp))
            }
        }
        Spacer(Modifier.height(3.dp))
        MetaLine(message, progress, mine, time, c, style.showTimes)
    }
}

/** Embaixo do balão: hora e, nas suas mensagens, o visto (✓ enviada, ✓✓ recebida, ✓✓ azul sentida). */
@Composable
private fun MetaLine(message: Message, progress: PlaybackProgress?, mine: Boolean, time: String, c: CommColors, showTime: Boolean) {
    val label = statusLabel(message.status, progress)
    Row(
        Modifier.padding(horizontal = 4.dp).clearAndSetSemantics {
            contentDescription = if (mine) "$label${if (showTime) ", $time" else ""}" else time
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showTime) {
            Text(time, color = c.muted, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
        }
        if (mine) {
            Spacer(Modifier.width(6.dp))
            when (message.status) {
                MessageStatus.SENDING, MessageStatus.SENT ->
                    Icon(ContatoIcons.Check, null, tint = c.muted, modifier = Modifier.size(14.dp))
                MessageStatus.DELIVERED ->
                    Icon(ContatoIcons.DoneAll, null, tint = c.muted, modifier = Modifier.size(15.dp))
                MessageStatus.PLAYING -> Text(label, color = c.accent,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
                MessageStatus.PLAYED -> {
                    Icon(ContatoIcons.DoneAll, null, tint = c.accent, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Sentida", color = c.accent, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                }
                MessageStatus.FAILED -> {
                    Icon(ContatoIcons.Warning, null, tint = c.danger, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("Não enviada", color = c.danger, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}

/** Texto normalizado com a letra que está vibrando agora em destaque; as já sentidas ficam mais claras. */
@Composable
private fun ProgressText(progress: PlaybackProgress, bubble: Color, textSp: Int) {
    val text = buildAnnotatedString {
        progress.text.forEachIndexed { i, ch ->
            when {
                i < progress.index -> withStyle(SpanStyle(color = Color.White.copy(alpha = 0.85f))) { append(ch) }
                i == progress.index -> withStyle(
                    SpanStyle(color = bubble, background = Color.White, fontWeight = FontWeight.Bold)
                ) { append(if (ch == ' ') '·' else ch) }
                else -> withStyle(SpanStyle(color = Color.White)) { append(ch) }
            }
        }
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = (textSp + 1).sp, lineHeight = (textSp + 8).sp, letterSpacing = 1.sp),
        modifier = Modifier.semantics {
            contentDescription = "Vibrando letra ${progress.index + 1} de ${progress.total}"
        },
    )
}

// ───────────────────────── Campo de mensagem ─────────────────────────

/** Frases prontas (botão "+"): ajudam quem está começando a conversar com frases curtas. */
private val quickPhrases = listOf(
    "Oi, tudo bem?", "Estou aqui", "Já volto", "Sim", "Não", "Obrigado", "Você está bem?", "Vamos comer?",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageInput(
    enabled: Boolean,
    c: CommColors,
    style: MessageStyle,
    onSend: (String) -> Boolean,
    canSimplify: (String) -> Boolean,
    onSimplify: suspend (String) -> SimplifyResult,
    feelEstimate: (String) -> Long,
) {
    var text by rememberSaveable { mutableStateOf("") }
    // IA: tudo fica preso à frase que gerou; se a pessoa continuar digitando, some sozinho.
    var simplifyingFor by remember { mutableStateOf<String?>(null) }
    var lastResult by remember { mutableStateOf<Pair<String, SimplifyResult>?>(null) }
    var dismissedFor by remember { mutableStateOf<String?>(null) }
    val aiScope = rememberCoroutineScope()
    val trimmed = text.trim()
    val result = lastResult?.takeIf { it.first == trimmed }?.second
    val panel: AiPanel = when {
        !enabled -> AiPanel.None
        simplifyingFor != null -> if (simplifyingFor == trimmed) AiPanel.Thinking else AiPanel.None
        dismissedFor == trimmed -> AiPanel.None
        !canSimplify(trimmed) -> AiPanel.None // IA desligada/indisponível ou frase curta
        result is SimplifyResult.Shorter -> AiPanel.Suggest(AiSuggestion(trimmed, result.text))
        result == SimplifyResult.NoGain -> AiPanel.Chip("A frase já está curta", active = false)
        result == SimplifyResult.Failed -> AiPanel.Chip("Não deu certo. Tentar de novo", active = true)
        else -> AiPanel.Chip("Simplificar com IA", active = true)
    }

    fun simplify() {
        val original = trimmed
        simplifyingFor = original
        aiScope.launch {
            try {
                lastResult = original to onSimplify(original)
            } finally {
                simplifyingFor = null
            }
        }
    }
    var showPhrases by remember { mutableStateOf(false) }
    val canSend = enabled && text.isNotBlank()
    val hasUnsupported = text.any { it.isDigit() || (!it.isLetter() && !it.isWhitespace()) }
    val sendColor = Color(style.bubble.argb)

    fun send() {
        if (canSend && onSend(text)) text = ""
    }

    Column(Modifier.fillMaxWidth().background(c.surface).padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 10.dp)) {
        if (hasUnsupported) {
            Text("Números e pontuação não vibram; só as letras serão sentidas.", color = c.muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))
        }
        AnimatedContent(
            targetState = panel,
            transitionSpec = { fadeIn(tween(Motion.FAST)) togetherWith fadeOut(tween(Motion.QUICK)) },
            label = "ia",
        ) { p ->
            when (p) {
                AiPanel.None -> Spacer(Modifier.fillMaxWidth())
                is AiPanel.Chip -> SimplifyChip(p.label, p.active, c, onClick = ::simplify)
                AiPanel.Thinking -> AiSuggestionCard(null, loading = true, c = c, onUse = {}, onKeep = {})
                is AiPanel.Suggest -> AiSuggestionCard(
                    suggestion = p.suggestion,
                    loading = false,
                    c = c,
                    onUse = { s -> text = s.text; dismissedFor = s.text.trim() },
                    onKeep = { dismissedFor = trimmed },
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(1.dp, c.border, CircleShape)
                    .clickable(enabled = enabled, role = Role.Button, onClickLabel = "Frases rápidas") { showPhrases = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(ContatoIcons.Plus, contentDescription = "Frases rápidas", tint = if (enabled) c.onSurface else c.muted)
            }
            Spacer(Modifier.width(8.dp))
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(CircleShape)
                    .background(if (c.dark) c.bg else Color(0xFFEEF1F6))
                    .padding(start = 16.dp, end = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
                    if (text.isEmpty()) {
                        Text(if (enabled) "Escreva uma mensagem" else "Aguardando conexão",
                            color = c.muted, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
                    }
                    BasicTextField(
                        value = text,
                        onValueChange = { if (it.length <= CommunicatorSession.MAX_LENGTH) text = it },
                        enabled = enabled,
                        textStyle = TextStyle(color = c.onSurface, fontSize = 16.sp, lineHeight = 22.sp),
                        cursorBrush = SolidColor(c.accent),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Mensagem" },
                    )
                }
                // Contador + quanto tempo a pessoa vai levar para sentir (antes mesmo de enviar).
                val feel = if (trimmed.isEmpty()) 0L else feelEstimate(trimmed)
                Column(horizontalAlignment = Alignment.End) {
                    Text("${text.length}/${CommunicatorSession.MAX_LENGTH}", color = c.muted,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp))
                    if (feel > 0) {
                        Text(TimeEstimates.friendly(feel), color = c.muted,
                            modifier = Modifier.semantics { contentDescription = "Leva ${TimeEstimates.friendly(feel).removePrefix("~")} para sentir" },
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (canSend) sendColor else c.border)
                    .clickable(enabled = canSend, role = Role.Button, onClickLabel = "Enviar mensagem") { send() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(ContatoIcons.Send, contentDescription = "Enviar",
                    tint = if (canSend) Color.White else c.muted, modifier = Modifier.size(22.dp))
            }
        }
    }

    if (showPhrases) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(onDismissRequest = { showPhrases = false }, sheetState = sheet, containerColor = c.surface) {
            Column(Modifier.navigationBarsPadding().padding(start = 20.dp, end = 20.dp, bottom = 24.dp)) {
                Text("Frases rápidas", color = c.onSurface, modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                Text("Toque para colocar no campo. Frases curtas são sentidas mais rápido.", color = c.muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
                Spacer(Modifier.height(14.dp))
                quickPhrases.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        row.forEach { phrase ->
                            Box(
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(c.accentSoft)
                                    .clickable(role = Role.Button) {
                                        text = phrase
                                        scope.launch { sheet.hide() }.invokeOnCompletion { showPhrases = false }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(phrase, color = c.onSurface, textAlign = TextAlign.Center,
                                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** Sugestão da IA ligada à frase que a gerou. */
private data class AiSuggestion(val original: String, val text: String)

/** O que aparece acima do campo de texto por causa da IA. */
private sealed interface AiPanel {
    data object None : AiPanel
    data class Chip(val label: String, val active: Boolean) : AiPanel
    data object Thinking : AiPanel
    data class Suggest(val suggestion: AiSuggestion) : AiPanel
}

/** Botãozinho em pílula acima do campo: "Simplificar com IA". */
@Composable
private fun SimplifyChip(label: String, active: Boolean, c: CommColors, onClick: () -> Unit) {
    Row(
        Modifier
            .padding(start = 4.dp, bottom = 8.dp)
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(if (active) c.accentSoft else Color.Transparent)
            .clickable(enabled = active, role = Role.Button, onClickLabel = "Pedir uma versão mais curta", onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (active) ContatoIcons.Sparkles else ContatoIcons.Check, contentDescription = null,
            tint = if (active) c.accent else c.muted, modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (active) c.onSurface else c.muted,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
    }
}

/** Cartão com a versão sugerida: mostra quantas letras a menos vão vibrar e deixa escolher. */
@Composable
private fun AiSuggestionCard(
    suggestion: AiSuggestion?,
    loading: Boolean,
    c: CommColors,
    onUse: (AiSuggestion) -> Unit,
    onKeep: () -> Unit,
) {
    Column(
        Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.accentSoft)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(ContatoIcons.Sparkles, contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                if (loading || suggestion == null) "Pensando numa versão mais curta…" else "Sugestão da IA",
                color = c.muted,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
            )
        }
        if (suggestion != null && !loading) {
            Spacer(Modifier.height(6.dp))
            Text(suggestion.text, color = c.onSurface,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold))
            val saved = SimplificationRules.lettersSaved(suggestion.original, suggestion.text)
            Text("$saved letras a menos para sentir", color = c.muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton("Usar", c, primary = true, modifier = Modifier.weight(1f)) { onUse(suggestion) }
                PillButton("Manter a minha", c, primary = false, modifier = Modifier.weight(1f), onClick = onKeep)
            }
        }
    }
}

/**
 * Faixa acima do campo de texto com o tempo estimado:
 *  - enquanto a mensagem vibra: "Sentindo a mensagem · faltam ~12 s";
 *  - depois: "Aguardando a resposta · costuma levar ~1 min" (aprende com as respostas anteriores).
 */
@Composable
private fun EstimateStrip(estimate: CommunicatorSession.ChatEstimate?, c: CommColors) {
    // Relógio que anda de 1 em 1 segundo só enquanto há estimativa na tela.
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(estimate) {
        while (estimate != null) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    AnimatedContent(
        targetState = estimate,
        contentKey = { it?.javaClass },
        transitionSpec = { fadeIn(tween(Motion.FAST)) togetherWith fadeOut(tween(Motion.QUICK)) },
        label = "estimate",
    ) { e ->
        if (e == null) {
            Spacer(Modifier.fillMaxWidth())
            return@AnimatedContent
        }
        val (title, detail) = when (e) {
            is CommunicatorSession.ChatEstimate.Feeling -> {
                val left = e.endsAt - now
                "Sentindo a mensagem" to if (left > 1500) "faltam ${TimeEstimates.friendly(left)}" else "quase lá"
            }
            is CommunicatorSession.ChatEstimate.Waiting -> {
                val waited = now - e.since
                "Aguardando a resposta" to if (waited < e.typicalMs) "costuma levar ${TimeEstimates.friendly(e.typicalMs)}"
                else "a pessoa ainda está escrevendo"
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .background(c.surface)
                .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                .clip(CircleShape)
                .background(c.accentSoft)
                // Leitor de tela: só o título (muda pouco) é anunciado; a contagem de segundos não.
                .clearAndSetSemantics { contentDescription = title; liveRegion = LiveRegionMode.Polite }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (e is CommunicatorSession.ChatEstimate.Feeling) ContatoIcons.Vibration else ContatoIcons.Clock,
                contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            // Só o título é anunciado (muda pouco); a contagem não fica repetindo no leitor de tela.
            Text(title, color = c.onSurface,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
            Text(" · $detail", color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp))
        }
    }
}
