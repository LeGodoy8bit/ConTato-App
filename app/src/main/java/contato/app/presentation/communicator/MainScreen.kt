package contato.app.presentation.communicator

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
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
    PROFILE("Perfil", Icons.Default.Person),
    SETTINGS("Ajustes", Icons.Default.Settings),
}

/**
 * Tela principal do Comunicador: três abas (Conversas, Perfil, Ajustes) numa barra flutuante.
 * Troca de aba desliza para o lado da aba escolhida (transitions.dev "page side-by-side").
 * A barra some enquanto o teclado está aberto.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MainScreen(session: CommunicatorSession, onOpenInfo: (InfoTab) -> Unit) {
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
                    )
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

// ───────────────────────── Barra inferior flutuante ─────────────────────────

@Composable
private fun BottomBar(selected: MainTab, c: CommColors, unread: Int, onSelect: (MainTab) -> Unit) {
    Box(
        Modifier.fillMaxWidth().navigationBarsPadding().padding(top = 8.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(c.nav)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { t ->
                NavItem(t, selected == t, c, badge = if (t == MainTab.CHAT) unread else 0) { onSelect(t) }
            }
        }
    }
}

/** Aba escolhida mostra ícone + nome numa pílula; as outras, só o ícone. A pílula cresce com animação. */
@Composable
private fun NavItem(tab: MainTab, selected: Boolean, c: CommColors, badge: Int, onClick: () -> Unit) {
    val reduce = motionReduced()
    val bg by animateColorAsState(
        if (selected) c.sent else Color.Transparent,
        tween(if (reduce) 0 else Motion.FAST), label = "navBg",
    )
    Row(
        Modifier
            .clip(CircleShape)
            .background(bg)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .semantics { contentDescription = if (badge > 0) "${tab.label}, $badge não lidas" else tab.label }
            .heightIn(min = 52.dp)
            .widthIn(min = 52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Box {
            Icon(tab.icon, contentDescription = null, tint = c.onNav.copy(alpha = if (selected) 1f else 0.75f), modifier = Modifier.size(22.dp))
            // Bolinha de não lidas: entra com um "pop".
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
        AnimatedVisibility(
            visible = selected,
            enter = expandHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) + fadeIn(tween(Motion.FAST)),
            exit = shrinkHorizontally(tween(Motion.QUICK, easing = Motion.SmoothOut)) + fadeOut(tween(Motion.QUICK)),
        ) {
            Text(
                tab.label,
                color = c.onNav,
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold),
            )
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
            ConversationsTab(session, c, onOpenConversation = { onOpen(it) }, onOpenInfo = onOpenInfo)
        } else {
            ChatDetail(session, id, c, onBack = { onOpen(null) }, onOpenInfo = onOpenInfo)
        }
    }
}
