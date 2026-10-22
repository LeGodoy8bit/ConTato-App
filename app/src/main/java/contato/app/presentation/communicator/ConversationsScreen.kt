package contato.app.presentation.communicator

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.domain.model.Conversation
import contato.app.domain.model.UserMode
import contato.app.domain.state.ConnectionState
import contato.app.presentation.theme.ContatoIcons
import contato.app.presentation.theme.Motion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Aba Conversas (referência "Messages"): uma linha por aparelho com quem você conversou,
 * como no WhatsApp. Mesmo com uma conversa só, ela aparece na lista.
 *
 *  - Aviso "Você está sem login" (pode fechar), que abre a janela de login.
 *  - Conversa do aparelho conectado em destaque (cartão elevado e bolinha verde).
 *  - Hora, prévia da última mensagem ("Você: ...") e contador de não lidas.
 */
@Composable
internal fun ConversationsTab(
    session: CommunicatorSession,
    c: CommColors,
    onOpenConversation: (String) -> Unit,
    onOpenInfo: () -> Unit,
) {
    var loginNoticeClosed by rememberSaveable { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    val connected = session.connectionState == ConnectionState.CONNECTED
    val showNotice = !session.account.isSignedIn && !loginNoticeClosed

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 16.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ScreenHeading("Conversas", c, Modifier.weight(1f).semantics { heading() })
            IconButton(onClick = onOpenInfo) {
                Icon(Icons.Default.Info, contentDescription = "Sobre o ConTato e matriz Braille", tint = c.onSurface)
            }
        }
        Row(Modifier.padding(horizontal = 20.dp)) {
            ToneChip(connectionLabel(session.connectionState, session.peerName), connectionTone(session.connectionState), c, showCheck = connected)
        }
        Spacer(Modifier.height(12.dp))

        AnimatedVisibility(
            visible = showNotice,
            enter = expandVertically(tween(Motion.SLOW, easing = Motion.SmoothOut)) + fadeIn(tween(Motion.SLOW)),
            exit = shrinkVertically(tween(Motion.MEDIUM, easing = Motion.SmoothOut)) + fadeOut(tween(Motion.QUICK)),
        ) {
            LoginNotice(c, onLogin = { showLogin = true }, onClose = { loginNoticeClosed = true })
        }

        if (session.conversations.isEmpty()) {
            EmptyConversations(
                c = c,
                connected = connected,
                onPair = session::searchAgain,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            )
        } else {
            LazyColumn(
                Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(session.conversations, key = { it.id }) { conversation ->
                    val isActive = conversation.id == session.activeConversationId
                    Box(Modifier.animateItem()) {
                        ConversationRow(
                            conversation = conversation,
                            online = isActive && connected,
                            highlighted = isActive && connected,
                            c = c,
                            onClick = { onOpenConversation(conversation.id) },
                        )
                    }
                }
            }
        }
    }

    if (showLogin) LoginBottomSheet(session, c, onDismiss = { showLogin = false })
}

/** "Você está sem login": aviso discreto no topo da lista, com botão Entrar e X para fechar. */
@Composable
private fun LoginNotice(c: CommColors, onLogin: () -> Unit, onClose: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.accentSoft)
            .padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
            Icon(ContatoIcons.Cloud, null, tint = c.accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Você está sem login", color = c.onSurface,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold))
            Text("Faça login para salvar suas conversas na nuvem.", color = c.muted,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
            Spacer(Modifier.height(8.dp))
            Text(
                "Entrar",
                color = Color.White,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(c.sent)
                    .clickable(role = Role.Button, onClickLabel = "Entrar com Google", onClick = onLogin)
                    .heightIn(min = 40.dp)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold),
            )
        }
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.Top)) {
            Icon(Icons.Default.Close, contentDescription = "Fechar aviso", tint = c.muted, modifier = Modifier.size(20.dp))
        }
    }
}

// ───────────────────────── Linha da conversa ─────────────────────────

@Composable
private fun ConversationRow(
    conversation: Conversation,
    online: Boolean,
    highlighted: Boolean,
    c: CommColors,
    onClick: () -> Unit,
) {
    val last = conversation.lastMessage
    val unread = conversation.unreadCount(UserMode.COMMUNICATOR)
    val time = remember(last?.timestamp ?: conversation.updatedAt) { friendlyTime(last?.timestamp ?: conversation.updatedAt) }
    val preview = when {
        last == null -> "Conectado. Escreva a primeira mensagem."
        last.sender == UserMode.COMMUNICATOR -> "Você: ${last.content}"
        else -> last.content
    }
    val elevation by androidx.compose.animation.core.animateDpAsState(
        if (highlighted) 12.dp else 0.dp, tween(Motion.MEDIUM, easing = Motion.SmoothOut), label = "elev",
    )
    val shape = RoundedCornerShape(24.dp)
    val spoken = buildString {
        append(conversation.peerName)
        if (online) append(", conectado")
        append(". ").append(preview).append(". ").append(time)
        if (unread > 0) append(". $unread não lida").append(if (unread > 1) "s" else "")
    }

    Row(
        Modifier
            .fillMaxWidth()
            .shadow(elevation, shape, ambientColor = c.sent.copy(alpha = 0.25f), spotColor = c.sent.copy(alpha = 0.25f))
            .clip(shape)
            .background(c.surface)
            .then(if (c.dark) Modifier.border(1.dp, c.border, shape) else Modifier)
            .clickable(role = Role.Button, onClickLabel = "Abrir conversa", onClick = onClick)
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PeerAvatar(conversation.peerName, 56.dp, c, online)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.peerName, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp,
                        fontWeight = if (unread > 0 || highlighted) FontWeight.Bold else FontWeight.SemiBold),
                )
                Spacer(Modifier.width(8.dp))
                Text(time, color = if (unread > 0) c.accent else c.muted,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp))
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    preview, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = if (unread > 0) c.onSurface else c.muted,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp,
                        fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal),
                )
                if (unread > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp).clip(CircleShape).background(c.sent)
                            .padding(horizontal = 6.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(if (unread > 99) "99+" else "$unread", color = Color.White,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
        }
    }
}

/** Avatar colorido pela inicial (cores fixas por nome, para cada aparelho ter "a sua"). */
@Composable
internal fun PeerAvatar(name: String, size: Dp, c: CommColors, online: Boolean) {
    val palette = listOf(Color(0xFF2EC4B6), Color(0xFF3A86FF), Color(0xFFFFA62B), Color(0xFFEF5DA8), Color(0xFF8B5CF6), Color(0xFF22A06B))
    val tint = palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
    val dot by animateColorAsState(if (online) c.online else Color.Transparent, tween(Motion.FAST), label = "online")
    Box(Modifier.size(size)) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.Bold),
            )
        }
        if (online) {
            Box(
                Modifier.align(Alignment.TopEnd).size(size * 0.28f).clip(CircleShape).background(c.surface).padding(2.dp)
                    .clip(CircleShape).background(dot),
            )
        }
    }
}

@Composable
private fun EmptyConversations(c: CommColors, connected: Boolean, onPair: () -> Unit, modifier: Modifier) {
    Column(
        modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(88.dp).clip(RoundedCornerShape(28.dp)).background(c.accentSoft), contentAlignment = Alignment.Center) {
            Icon(ContatoIcons.Chat, null, tint = c.accent, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text("Nenhuma conversa ainda", color = c.onSurface, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(6.dp))
        Text(
            "Quando você conectar com um celular tátil, a conversa aparece aqui.",
            color = c.muted, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
        )
        if (!connected) {
            Spacer(Modifier.height(24.dp))
            ArrowPill("Parear agora", onPair, Modifier.widthIn(max = 320.dp))
        }
    }
}

// ───────────────────────── Datas amigáveis ─────────────────────────

private val hourFormat = SimpleDateFormat("HH:mm", Locale.forLanguageTag("pt-BR"))
private val dayFormat = SimpleDateFormat("dd/MM", Locale.forLanguageTag("pt-BR"))
private val longDayFormat = SimpleDateFormat("d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

private fun daysAgo(millis: Long): Int {
    fun startOfDay(t: Long) = Calendar.getInstance().apply {
        timeInMillis = t; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    return ((startOfDay(System.currentTimeMillis()) - startOfDay(millis)) / 86_400_000L).toInt()
}

/** Lista: "14:32" hoje, "Ontem", ou "12/10". */
internal fun friendlyTime(millis: Long): String = when (daysAgo(millis)) {
    0 -> hourFormat.format(Date(millis))
    1 -> "Ontem"
    else -> dayFormat.format(Date(millis))
}

/** Separador dentro da conversa: "Hoje", "Ontem", "12 de outubro". */
internal fun dayLabel(millis: Long): String = when (daysAgo(millis)) {
    0 -> "Hoje"
    1 -> "Ontem"
    else -> longDayFormat.format(Date(millis))
}
