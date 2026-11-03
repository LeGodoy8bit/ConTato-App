package contato.app.presentation.deafblind

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.domain.model.Conversation
import contato.app.domain.model.UserMode
import contato.app.presentation.communicator.dayLabel
import contato.app.presentation.communicator.friendlyTime
import contato.app.presentation.theme.ContatoIcons
import contato.app.presentation.theme.Motion

/**
 * Conversas do Surdocego (para a pessoa de apoio consultar):
 *  - sem login: aviso "Você está sem login" e o botão do Google, que a pessoa de apoio usa;
 *  - lista de conversas (uma por comunicador), igual à do Comunicador;
 *  - tocar abre a conversa só para leitura.
 */
@Composable
internal fun DeafblindConversationsScreen(session: DeafblindSession, colors: DbColors, onBack: () -> Unit) {
    var openId by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = openId != null) { openId = null }

    AnimatedContent(
        targetState = openId,
        transitionSpec = { fadeIn(tween(Motion.FAST)) togetherWith fadeOut(tween(Motion.QUICK)) },
        label = "dbConversations",
    ) { id ->
        val conversation = id?.let { cid -> session.conversations.firstOrNull { it.id == cid } }
        if (conversation == null) {
            ConversationList(session, colors, onBack, onOpen = { openId = it })
        } else {
            LaunchedEffect(conversation.id) { session.markRead(conversation.id) }
            ConversationDetail(conversation, colors, onBack = { openId = null })
        }
    }
}

@Composable
private fun ConversationList(session: DeafblindSession, colors: DbColors, onBack: () -> Unit, onOpen: (String) -> Unit) {
    var confirmClear by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val account = session.account
    val user = account.user

    Column(Modifier.fillMaxSize().background(colors.bg).statusBarsPadding().navigationBarsPadding()) {
        DbHeader("Conversas", colors, onBack)
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "account") {
                if (user == null) {
                    LoginCard(
                        colors = colors,
                        configured = account.isConfigured,
                        busy = account.busy,
                        error = account.error,
                        onLogin = { session.signIn(context) },
                    )
                } else {
                    DbCard(colors) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).clip(CircleShape).background(colors.dotOn), contentAlignment = Alignment.Center) {
                                Icon(ContatoIcons.Cloud, null, tint = colors.onDotOn, modifier = Modifier.size(24.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(user.name ?: "Conta conectada", color = colors.onBg, maxLines = 1,
                                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
                                Text("Conversas salvas na nuvem", color = colors.muted,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
                            }
                        }
                        RowAction(ContatoIcons.LogOut, "Sair da conta", colors.onBg) { confirmSignOut = true }
                    }
                }
            }
            if (session.conversations.isEmpty()) {
                item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(top = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(88.dp).clip(RoundedCornerShape(28.dp)).background(colors.surface), contentAlignment = Alignment.Center) {
                            Icon(ContatoIcons.Chat, null, tint = colors.accent, modifier = Modifier.size(40.dp))
                        }
                        Spacer(Modifier.height(16.dp))
                        Text("Nenhuma conversa ainda", color = colors.onBg,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                    }
                }
            } else {
                items(session.conversations, key = { it.id }) { conversation ->
                    Box(Modifier.animateItem()) {
                        ConversationRow(conversation, conversation.id == session.activeConversationId, colors) { onOpen(conversation.id) }
                    }
                }
                item(key = "clear") {
                    RowAction(ContatoIcons.Trash, "Apagar conversas", colors.error) { confirmClear = true }
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Apagar conversas?") },
            text = { Text(if (user == null) "Serão apagadas deste aparelho." else "Serão apagadas deste aparelho e da conta na nuvem.") },
            confirmButton = {
                TextButton(onClick = { session.clearHistory(); confirmClear = false }) {
                    Text("Apagar", color = colors.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
        )
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sair da conta?") },
            text = { Text("As conversas continuam neste aparelho.") },
            confirmButton = {
                TextButton(onClick = { confirmSignOut = false; session.signOut(context) }) {
                    Text("Sair", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancelar") } },
        )
    }
}

/** "Você está sem login": pede para a pessoa de apoio entrar com Google. */
@Composable
private fun LoginCard(colors: DbColors, configured: Boolean, busy: Boolean, error: String?, onLogin: () -> Unit) {
    DbCard(colors) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(colors.dotOff), contentAlignment = Alignment.Center) {
                    Icon(ContatoIcons.Cloud, null, tint = colors.accent, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Text("Você está sem login", color = colors.onBg,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Peça para a pessoa de apoio entrar com uma conta Google. Assim as conversas ficam salvas na nuvem.",
                color = colors.muted, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 23.sp),
            )
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 60.dp)
                    .clip(CircleShape)
                    .background(if (configured) colors.bg else colors.border)
                    .border(1.5.dp, if (configured) colors.dotOn else colors.border, CircleShape)
                    .clickable(enabled = configured && !busy, role = Role.Button, onClick = onLogin),
                contentAlignment = Alignment.Center,
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = colors.accent, strokeWidth = 2.5.dp)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("G", color = if (configured) Color(0xFF4285F4) else colors.muted,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold))
                        Spacer(Modifier.width(12.dp))
                        Text(if (configured) "Continuar com Google" else "Login disponível em breve",
                            color = if (configured) colors.onBg else colors.muted,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
            if (error != null) {
                Spacer(Modifier.height(8.dp))
                Text(error, color = colors.error, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
            }
        }
    }
}

@Composable
private fun RowAction(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint)
        Spacer(Modifier.width(14.dp))
        Text(text, color = tint, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, active: Boolean, colors: DbColors, onClick: () -> Unit) {
    val last = conversation.lastMessage
    val unread = conversation.unreadCount(UserMode.DEAFBLIND)
    val time = friendlyTime(last?.timestamp ?: conversation.updatedAt)
    val preview = when {
        last == null -> "Sem mensagens"
        last.sender == UserMode.DEAFBLIND -> "Você: ${last.content}"
        else -> last.content
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, if (active) colors.dotOn else colors.border, RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = "${conversation.peerName}${if (active) ", conectado" else ""}. $preview. $time" +
                        if (unread > 0) ". $unread não lidas" else ""
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(colors.dotOn), contentAlignment = Alignment.Center) {
            Text(conversation.peerName.trim().firstOrNull()?.uppercase() ?: "?", color = colors.onDotOn,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(conversation.peerName, color = colors.onBg, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                Text(time, color = colors.muted, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp))
            }
            Text(preview, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
        }
        if (unread > 0) {
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(26.dp).clip(CircleShape).background(colors.dotOn), contentAlignment = Alignment.Center) {
                Text("$unread", color = colors.onDotOn, style = MaterialTheme.typography.labelMedium.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

/** Conversa só para leitura, com separadores de dia. */
@Composable
private fun ConversationDetail(conversation: Conversation, colors: DbColors, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(colors.bg).statusBarsPadding().navigationBarsPadding()) {
        DbHeader(conversation.peerName, colors, onBack)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            reverseLayout = false,
        ) {
            var lastDay = ""
            conversation.messages.forEachIndexed { index, m ->
                val day = dayLabel(m.timestamp)
                if (day != lastDay) {
                    lastDay = day
                    item(key = "day-$index-$day") {
                        Text(day, color = colors.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp))
                    }
                }
                item(key = m.id) {
                    val mine = m.sender == UserMode.DEAFBLIND
                    Box(Modifier.fillMaxWidth(), contentAlignment = if (mine) Alignment.CenterEnd else Alignment.CenterStart) {
                        Column(
                            Modifier
                                .widthIn(max = 320.dp)
                                .clip(RoundedCornerShape(22.dp))
                                .background(if (mine) colors.dotOn else colors.surface)
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        ) {
                            Text(if (mine) "Você" else conversation.peerName, color = if (mine) colors.onDotOn.copy(alpha = 0.8f) else colors.muted,
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                            Text(m.content, color = if (mine) colors.onDotOn else colors.onBg,
                                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 19.sp, lineHeight = 26.sp))
                        }
                    }
                }
            }
        }
    }
}
