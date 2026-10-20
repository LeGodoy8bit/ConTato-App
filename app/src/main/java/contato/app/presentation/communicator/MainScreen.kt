package contato.app.presentation.communicator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.presentation.theme.Motion
import contato.app.presentation.theme.motionReduced

internal enum class MainTab(val label: String, val icon: ImageVector) {
    CHAT("Conversas", Icons.Default.Email),
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
                    MainTab.CHAT -> ChatTab(session, c, onOpenInfo = { onOpenInfo(InfoTab.HOW) })
                    MainTab.PROFILE -> ProfileTab(session, c, onOpenInfo)
                    MainTab.SETTINGS -> SettingsTab(session, c)
                }
            }
        }
        if (!imeVisible) {
            BottomBar(tab, c, onSelect = { tab = it })
        }
    }
}

// ───────────────────────── Barra inferior flutuante ─────────────────────────

@Composable
private fun BottomBar(selected: MainTab, c: CommColors, onSelect: (MainTab) -> Unit) {
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
                NavItem(t, selected == t, c) { onSelect(t) }
            }
        }
    }
}

/** Aba escolhida mostra ícone + nome numa pílula; as outras, só o ícone. A pílula cresce com animação. */
@Composable
private fun NavItem(tab: MainTab, selected: Boolean, c: CommColors, onClick: () -> Unit) {
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
            .semantics { contentDescription = tab.label }
            .heightIn(min = 52.dp)
            .widthIn(min = 52.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(tab.icon, contentDescription = null, tint = c.onNav.copy(alpha = if (selected) 1f else 0.75f), modifier = Modifier.size(22.dp))
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
