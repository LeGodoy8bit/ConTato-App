package contato.app.presentation.communicator

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.domain.state.ConnectionState
import contato.app.presentation.theme.ContatoIcons
import contato.app.presentation.theme.Motion
import contato.app.presentation.theme.motionReduced

internal enum class MainTab(val label: String, val icon: ImageVector) {
    CHAT("Conversas", ContatoIcons.Chat),
    CONNECTION("Conexão", ContatoIcons.Sensors),
    PROFILE("Perfil", ContatoIcons.User),
    SETTINGS("Ajustes", ContatoIcons.Settings),
}

/**
 * Tela principal do Comunicador: quatro abas (Conversas, Conexão, Perfil, Ajustes) na barra de baixo.
 * Troca de aba desliza para o lado da aba escolhida (transitions.dev "page side-by-side").
 * A barra some enquanto o teclado está aberto.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MainScreen(session: CommunicatorSession, onOpenInfo: (InfoTab) -> Unit, onConnect: () -> Unit) {
    val c = rememberCommColors()
    val reduce = motionReduced()
    var tab by rememberSaveable { mutableStateOf(MainTab.CHAT) }
    val imeVisible = WindowInsets.isImeVisible
    // Guarda o estado de cada aba (rascunho da mensagem, rolagem) ao trocar de aba.
    val tabStates = rememberSaveableStateHolder()
    // Conversa aberta (null = lista). Ao chegar conectado, já abre a conversa daquele aparelho.
    var openConversation by rememberSaveable {
        mutableStateOf(session.activeConversationId.takeIf { session.connectionState == ConnectionState.CONNECTED })
    }
    // A conversa aberta pode trocar de id (ex.: ao juntar com a nuvem): segue a conversa ativa.
    LaunchedEffect(session.activeConversationId, session.conversations.size) {
        val open = openConversation
        if (open != null && session.conversation(open) == null) openConversation = session.activeConversationId
    }
    // Conectou pela tela de conexão: abre a conversa daquele aparelho.
    // (só pedidos novos: voltar de outra tela não reabre a conversa)
    var handledOpenRequest by rememberSaveable { mutableIntStateOf(session.openChatRequest) }
    LaunchedEffect(session.openChatRequest) {
        if (session.openChatRequest > handledOpenRequest) {
            handledOpenRequest = session.openChatRequest
            if (session.activeConversationId != null) {
                tab = MainTab.CHAT
                openConversation = session.activeConversationId
            }
        }
    }
    val openId = openConversation?.takeIf { session.conversation(it) != null }
    BackHandler(enabled = openId != null) { openConversation = null }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            .imePadding(),
    ) {
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                if (reduce) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) { dir * it / 12 } +
                            fadeIn(tween(Motion.FAST, easing = Motion.SmoothOut))) togetherWith
                            (slideOutHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) { -dir * it / 12 } +
                                    fadeOut(tween(Motion.QUICK)))
                }
            },
            label = "tabs",
        ) { current ->
            tabStates.SaveableStateProvider(current.name) {
                when (current) {
                    MainTab.CHAT -> ConversationsHost(
                        session = session,
                        c = c,
                        openId = openId,
                        onOpen = { openConversation = it },
                        onOpenInfo = { onOpenInfo(InfoTab.HOW) },
                        onNewConnection = onConnect,
                    )
                    MainTab.CONNECTION -> ConnectionTab(session, c, onConnect = onConnect, onOpenConversation = {
                        openConversation = it
                        tab = MainTab.CHAT
                    })
                    MainTab.PROFILE -> ProfileTab(session, c, onOpenInfo)
                    MainTab.SETTINGS -> SettingsTab(session, c)
                }
            }
        }
        // Escondida com o teclado aberto e dentro de uma conversa (como nos apps de mensagem).
        if (!imeVisible && !(tab == MainTab.CHAT && openId != null)) {
            BottomBar(tab, c, unread = session.unreadTotal, onSelect = { tab = it })
        }
    }
}

// ───────────────────────── Barra inferior ─────────────────────────

/**
 * Barra de baixo flutuante em pílula (referência de app de mensagens): fundo escuro,
 * a aba escolhida vira uma pílula clara com ícone + nome; as outras mostram só o ícone.
 */
@Composable
private fun BottomBar(selected: MainTab, c: CommColors, unread: Int, onSelect: (MainTab) -> Unit) {
    val barBg = if (c.dark) Color(0xFF1C2433) else ContatoNavyBar
    Box(
        Modifier
            .fillMaxWidth()
            .background(c.bg)
            .navigationBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(barBg)
                .padding(6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { t ->
                NavItem(t, selected == t, c, badge = if (t == MainTab.CHAT) unread else 0) { onSelect(t) }
            }
        }
    }
}

private val ContatoNavyBar = Color(0xFF1E2838)

@Composable
private fun NavItem(tab: MainTab, selected: Boolean, c: CommColors, badge: Int, onClick: () -> Unit) {
    val reduce = motionReduced()
    val pill by animateColorAsState(
        if (selected) Color.White.copy(alpha = 0.14f) else Color.Transparent,
        tween(if (reduce) 0 else Motion.FAST), label = "navPill",
    )
    val tint by animateColorAsState(
        if (selected) Color.White else Color.White.copy(alpha = 0.62f),
        tween(if (reduce) 0 else Motion.FAST), label = "navTint",
    )
    Row(
        Modifier
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(pill)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = if (badge > 0) "${tab.label}, $badge não lidas" else tab.label }
            .animateContentSize(if (reduce) snap() else tween(Motion.FAST, easing = Motion.SmoothOut))
            .padding(horizontal = if (selected) 18.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box {
            Icon(tab.icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            // Bolinha de não lidas: entra com um "pop" (transitions.dev "notification badge").
            val badgeScale by animateFloatAsState(
                targetValue = if (badge > 0) 1f else 0f,
                animationSpec = tween(
                    if (badge > 0) Motion.VERY_SLOW else Motion.QUICK,
                    easing = if (badge > 0) Motion.Bounce else Motion.SmoothOut,
                ),
                label = "badge",
            )
            if (badgeScale > 0.01f) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-3).dp)
                        .graphicsLayer { scaleX = badgeScale; scaleY = badgeScale }
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF5A5F)),
                )
            }
        }
        if (selected) {
            Spacer(Modifier.width(8.dp))
            Text(tab.label, color = tint, maxLines = 1,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

/** Lista de conversas ou a conversa aberta, deslizando lado a lado (lista ↔ detalhe). */
@Composable
private fun ConversationsHost(
    session: CommunicatorSession,
    c: CommColors,
    openId: String?,
    onOpen: (String?) -> Unit,
    onOpenInfo: () -> Unit,
    onNewConnection: () -> Unit,
) {
    val reduce = motionReduced()
    AnimatedContent(
        targetState = openId,
        transitionSpec = {
            if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            else {
                val forward = targetState != null
                (slideInHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) { if (forward) it / 4 else -it / 4 } +
                        fadeIn(tween(Motion.FAST))) togetherWith
                        (slideOutHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) { if (forward) -it / 4 else it / 4 } +
                                fadeOut(tween(Motion.QUICK)))
            }
        },
        label = "conversations",
    ) { id ->
        if (id == null || session.conversation(id) == null) {
            ConversationsTab(session, c, onOpenConversation = { onOpen(it) }, onOpenInfo = onOpenInfo, onNewConnection = onNewConnection)
        } else {
            ChatDetail(session, id, c, onBack = { onOpen(null) }, onOpenInfo = onOpenInfo)
        }
    }
}
