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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.domain.model.Conversation
import contato.app.domain.model.UserMode
import contato.app.domain.state.ConnectionState
import contato.app.presentation.common.Mascot
import contato.app.presentation.common.MascotMood
import contato.app.presentation.theme.ContatoIcons
import contato.app.presentation.theme.Motion
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Aba Conversas (referência "Messages"):
 *  - título com botão "+" (nova conexão) e busca;
 *  - aviso "Você está sem login" (pode fechar);
 *  - "Aparelhos" numa fileira de avatares (o conectado com bolinha verde);
 *  - "Mensagens": todas as conversas num cartão só, com hora, prévia e não lidas.
 */
@Composable
internal fun ConversationsTab(
    session: CommunicatorSession,
    c: CommColors,
    onOpenConversation: (String) -> Unit,
    onOpenInfo: () -> Unit,
    onNewConnection: () -> Unit,
) {
    var loginNoticeClosed by rememberSaveable { mutableStateOf(false) }
    var showLogin by remember { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    val connected = session.connectionState == ConnectionState.CONNECTED
    val showNotice = !session.account.isSignedIn && !loginNoticeClosed

    val filtered = remember(query, session.conversations.toList()) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) session.conversations.toList()
        else session.conversations.filter { conv ->
            conv.peerName.lowercase().contains(q) || conv.messages.any { it.content.lowercase().contains(q) }
        }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        item(key = "header") {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ScreenHeading("Conversas", c, Modifier.weight(1f).semantics { heading() })
                RoundIcon(Icons.Default.Info, "Como funciona e matriz Braille", c, onClick = onOpenInfo)
                Spacer(Modifier.width(8.dp))
                RoundIcon(Icons.Default.Add, "Nova conexão", c, filled = true, onClick = onNewConnection)
            }
        }
        item(key = "search") {
            SearchField(query, c, onChange = { query = it })
        }
        item(key = "notice") {
            AnimatedVisibility(
                visible = showNotice,
                enter = expandVertically(tween(Motion.SLOW, easing = Motion.SmoothOut)) + fadeIn(tween(Motion.SLOW)),
                exit = shrinkVertically(tween(Motion.MEDIUM, easing = Motion.SmoothOut)) + fadeOut(tween(Motion.QUICK)),
            ) {
                LoginNotice(c, onLogin = { showLogin = true }, onClose = { loginNoticeClosed = true })
            }
        }

        if (session.conversations.isEmpty()) {
            item(key = "empty") {
                EmptyConversations(c, connected, onPair = onNewConnection)
            }
        } else {
            item(key = "devices") {
                DevicesRow(session, c, connected, onOpenConversation, onNewConnection)
            }
            item(key = "messagesTitle") {
                Text("Mensagens", color = c.onSurface,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 8.dp).semantics { heading() },
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
            }
            if (filtered.isEmpty()) {
                item(key = "noResults") {
                    Text("Nada encontrado para \"$query\".", color = c.muted,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp))
                }
            }
            itemsIndexed(filtered, key = { _, conv -> conv.id }) { index, conversation ->
                val isActive = conversation.id == session.activeConversationId
                // Todas as linhas dentro de um cartão só: arredonda só a primeira e a última.
                val top = if (index == 0) 24.dp else 0.dp
                val bottom = if (index == filtered.lastIndex) 24.dp else 0.dp
                Column(
                    Modifier
                        .animateItem()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom))
                        .background(c.surface),
                ) {
                    ConversationRow(
                        conversation = conversation,
                        online = isActive && connected,
                        c = c,
                        onClick = { onOpenConversation(conversation.id) },
                    )
                    if (index < filtered.lastIndex) HorizontalDivider(color = c.border, modifier = Modifier.padding(start = 84.dp))
                }
            }
        }
    }

    if (showLogin) LoginBottomSheet(session, c, onDismiss = { showLogin = false })
}

@Composable
private fun RoundIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, c: CommColors, filled: Boolean = false, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (filled) c.onSurface else c.surface)
            .border(1.dp, if (filled) c.onSurface else c.border, CircleShape)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = if (filled) c.surface else c.onSurface, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SearchField(query: String, c: CommColors, onChange: (String) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(if (c.dark) c.surface else Color(0xFFE6EAF2))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = c.muted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f).padding(vertical = 12.dp)) {
            if (query.isEmpty()) Text("Buscar", color = c.muted, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = c.onSurface, fontSize = 16.sp),
                cursorBrush = SolidColor(c.accent),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Buscar conversas" },
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onChange("") }) {
                Icon(Icons.Default.Close, contentDescription = "Limpar busca", tint = c.muted, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Fileira "Aparelhos" (referência "Online friends"): o conectado primeiro, com bolinha verde. */
@Composable
private fun DevicesRow(
    session: CommunicatorSession,
    c: CommColors,
    connected: Boolean,
    onOpenConversation: (String) -> Unit,
    onNewConnection: () -> Unit,
) {
    val ordered = remember(session.conversations.toList(), session.activeConversationId, connected) {
        session.conversations.sortedByDescending { connected && it.id == session.activeConversationId }
    }
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 8.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(c.surface)
            .padding(vertical = 14.dp),
    ) {
        Text("Aparelhos", color = c.onSurface, modifier = Modifier.padding(start = 16.dp, bottom = 10.dp).semantics { heading() },
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
        LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item(key = "add") {
                DeviceChip("Conectar", c, online = false, add = true, onClick = onNewConnection)
            }
            items(ordered, key = { it.id }) { conv ->
                DeviceChip(conv.peerName, c, online = connected && conv.id == session.activeConversationId) {
                    onOpenConversation(conv.id)
                }
            }
        }
    }
}

@Composable
private fun DeviceChip(name: String, c: CommColors, online: Boolean, add: Boolean = false, onClick: () -> Unit) {
    Column(
        Modifier
            .width(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = if (add) "Nova conexão" else "$name${if (online) ", conectado" else ""}" }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (add) {
            Box(Modifier.size(56.dp).clip(CircleShape).border(2.dp, c.border, CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Add, null, tint = c.accent)
            }
        } else {
            Box(Modifier.size(60.dp).border(2.dp, if (online) c.sent else Color.Transparent, CircleShape).padding(3.dp)) {
                PeerAvatar(name, 54.dp, c, online)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name.substringBefore(" (").ifBlank { name }, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
        )
    }
}

/** "Você está sem login": aviso discreto no topo da lista, com botão Entrar e X para fechar. */
@Composable
private fun LoginNotice(c: CommColors, onLogin: () -> Unit, onClose: () -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
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
private fun ConversationRow(conversation: Conversation, online: Boolean, c: CommColors, onClick: () -> Unit) {
    val last = conversation.lastMessage
    val unread = conversation.unreadCount(UserMode.COMMUNICATOR)
    val time = remember(last?.timestamp ?: conversation.updatedAt) { friendlyTime(last?.timestamp ?: conversation.updatedAt) }
    val preview = when {
        last == null -> if (online) "Conectado agora. Escreva a primeira mensagem." else "Sem mensagens"
        last.sender == UserMode.COMMUNICATOR -> "Você: ${last.content}"
        else -> last.content
    }
    val spoken = buildString {
        append(conversation.peerName)
        if (online) append(", conectado")
        append(". ").append(preview).append(". ").append(time)
        if (unread > 0) append(". $unread não lida").append(if (unread > 1) "s" else "")
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "Abrir conversa", onClick = onClick)
            .clearAndSetSemantics { contentDescription = spoken }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PeerAvatar(conversation.peerName, 52.dp, c, online)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    conversation.peerName, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp,
                        fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.SemiBold),
                )
                Spacer(Modifier.width(8.dp))
                Text(time, color = if (unread > 0) c.accent else c.muted, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp))
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    preview, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    color = when { online && last == null -> c.online; unread > 0 -> c.onSurface; else -> c.muted },
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp,
                        fontWeight = if (unread > 0) FontWeight.SemiBold else FontWeight.Normal),
                )
                if (unread > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.defaultMinSize(minWidth = 22.dp, minHeight = 22.dp).clip(CircleShape).background(c.sent).padding(horizontal = 6.dp),
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

/** Avatar colorido pela inicial (cores fixas por nome) ou a foto, com a bolinha verde de conectado. */
@Composable
internal fun PeerAvatar(name: String, size: Dp, c: CommColors, online: Boolean, photo: ImageBitmap? = null) {
    val palette = listOf(Color(0xFF2EC4B6), Color(0xFF3A86FF), Color(0xFFFFA62B), Color(0xFFEF5DA8), Color(0xFF8B5CF6), Color(0xFF22A06B))
    val tint = palette[(name.hashCode() and Int.MAX_VALUE) % palette.size]
    val dot by animateColorAsState(if (online) c.online else Color.Transparent, tween(Motion.FAST), label = "online")
    Box(Modifier.size(size)) {
        Box(Modifier.fillMaxSize().clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
            if (photo != null) {
                Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    name.trim().firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.4f).sp, fontWeight = FontWeight.Bold),
                )
            }
        }
        if (online) {
            Box(
                Modifier.align(Alignment.BottomEnd).size(size * 0.28f).clip(CircleShape).background(c.surface).padding(2.dp)
                    .clip(CircleShape).background(dot),
            )
        }
    }
}

@Composable
private fun EmptyConversations(c: CommColors, connected: Boolean, onPair: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Mascot(150.dp, mood = MascotMood.CURIOUS)
        Spacer(Modifier.height(12.dp))
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
            ArrowPill("Conectar agora", onPair, Modifier.widthIn(max = 320.dp))
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
