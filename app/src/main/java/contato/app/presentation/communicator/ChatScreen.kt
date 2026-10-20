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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.domain.model.Message
import contato.app.domain.model.MessageStatus
import contato.app.domain.model.UserMode
import contato.app.domain.state.ConnectionState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ───────────────────────── Aba Conversa ─────────────────────────

@Composable
internal fun ChatTab(session: CommunicatorSession, c: CommColors, onOpenInfo: () -> Unit) {
    val connected = session.connectionState == ConnectionState.CONNECTED
    val listState = rememberLazyListState()

    // Rola para a última mensagem quando chega ou sai uma nova.
    LaunchedEffect(session.messages.size) {
        if (session.messages.isNotEmpty()) listState.animateScrollToItem(session.messages.lastIndex)
    }

    Column(Modifier.fillMaxSize()) {
        ChatHeader(session, c, onOpenInfo)

        AnimatedVisibility(visible = !connected) {
            OfflineBar(session.connectionState, c, onSearch = session::searchAgain)
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            if (session.messages.isEmpty()) {
                EmptyChat(c, Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(session.messages, key = { it.id }) { message ->
                        // Entrada/remoção suave de cada balão (sobe 12 dp e aparece).
                        Box(Modifier.animateItem()) {
                            MessageBubble(message, session.progress[message.id], c)
                        }
                    }
                }
            }
        }

        MessageInput(enabled = connected, c = c, onSend = session::send)
    }
}

@Composable
private fun ChatHeader(session: CommunicatorSession, c: CommColors, onOpenInfo: () -> Unit) {
    val state = session.connectionState
    val name = session.peerName ?: CommunicatorSession.DEFAULT_PEER
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(name, 44.dp, c, statusColor = connectionDotColor(state, c))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).semantics(mergeDescendants = true) {}) {
            Text(name, color = c.onSurface, maxLines = 1,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold))
            AnimatedContent(
                targetState = if (state == ConnectionState.CONNECTED) "Conectado" else connectionLabel(state, null),
                transitionSpec = { textSwapTransition() },
                label = "headerStatus",
            ) { t ->
                Text(t, color = c.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
            }
        }
        IconButton(onClick = onOpenInfo) {
            Icon(Icons.Default.Info, contentDescription = "Sobre o ConTato e matriz Braille", tint = c.onSurface)
        }
    }
}

@Composable
private fun OfflineBar(state: ConnectionState, c: CommColors, onSearch: () -> Unit) {
    val searching = state != ConnectionState.DISCONNECTED && state != ConnectionState.CONNECTION_ERROR
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 4.dp)
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
                Icon(Icons.Default.Refresh, null, tint = c.accent, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Procurar", color = c.accent, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EmptyChat(c: CommColors, modifier: Modifier) {
    Column(modifier.padding(horizontal = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Email, null, tint = c.accent, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.height(16.dp))
        Text("Nenhuma mensagem ainda", color = c.onSurface,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(6.dp))
        Text(
            "Prefira frases curtas: cada letra leva cerca de 2 segundos para vibrar.",
            color = c.muted,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
        )
    }
}

// ───────────────────────── Balões ─────────────────────────

private val timeFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR"))

@Composable
private fun MessageBubble(message: Message, progress: PlaybackProgress?, c: CommColors) {
    val mine = message.sender == UserMode.COMMUNICATOR
    val time = remember(message.timestamp) { timeFormat.format(Date(message.timestamp)) }
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
    ) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp, topEnd = 20.dp,
                        bottomStart = if (mine) 20.dp else 6.dp,
                        bottomEnd = if (mine) 6.dp else 20.dp,
                    )
                )
                .background(if (mine) c.sent else c.received)
                .then(if (mine || c.dark) Modifier else Modifier.border(1.dp, c.border, RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)))
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            // Enquanto vibra, a letra atual fica destacada dentro do próprio balão.
            if (mine && message.status == MessageStatus.PLAYING && progress != null) {
                ProgressText(progress, c)
            } else {
                Text(message.content, color = if (mine) c.onSent else c.onReceived,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp))
            }
            Text(time, color = (if (mine) c.onSent else c.muted).copy(alpha = 0.8f),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                modifier = Modifier.align(Alignment.End).padding(top = 2.dp))
        }
        if (mine) {
            Spacer(Modifier.height(4.dp))
            StatusChip(message.status, progress, c)
        }
    }
}

/** Texto normalizado com a letra que está vibrando agora em destaque; as já sentidas ficam mais claras. */
@Composable
private fun ProgressText(progress: PlaybackProgress, c: CommColors) {
    val text = buildAnnotatedString {
        progress.text.forEachIndexed { i, ch ->
            when {
                i < progress.index -> withStyle(SpanStyle(color = c.onSent.copy(alpha = 0.6f))) { append(ch) }
                i == progress.index -> withStyle(
                    SpanStyle(color = c.sent, background = Color.White, fontWeight = FontWeight.Bold)
                ) { append(if (ch == ' ') '·' else ch) }
                else -> withStyle(SpanStyle(color = c.onSent)) { append(ch) }
            }
        }
    }
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp, letterSpacing = 1.sp),
        modifier = Modifier.semantics {
            contentDescription = "Vibrando letra ${progress.index + 1} de ${progress.total}"
        },
    )
}

// ───────────────────────── Campo de mensagem ─────────────────────────

@Composable
private fun MessageInput(enabled: Boolean, c: CommColors, onSend: (String) -> Boolean) {
    var text by rememberSaveable { mutableStateOf("") }
    val canSend = enabled && text.isNotBlank()
    val hasUnsupported = text.any { it.isDigit() || (!it.isLetter() && !it.isWhitespace()) }

    fun send() {
        if (canSend && onSend(text)) text = ""
    }

    Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 6.dp, bottom = 8.dp)) {
        if (hasUnsupported) {
            Text("Números e pontuação não vibram; só as letras serão sentidas.", color = c.muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                modifier = Modifier.padding(start = 12.dp, bottom = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(CircleShape)
                    .background(c.surface)
                    .border(1.dp, c.border, CircleShape)
                    .padding(start = 18.dp, end = 14.dp),
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
                Text("${text.length}/${CommunicatorSession.MAX_LENGTH}", color = c.muted,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp))
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if (canSend) c.sent else c.border)
                    .clickable(enabled = canSend, role = Role.Button, onClickLabel = "Enviar mensagem") { send() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar",
                    tint = if (canSend) Color.White else c.muted, modifier = Modifier.size(22.dp))
            }
        }
    }
}
