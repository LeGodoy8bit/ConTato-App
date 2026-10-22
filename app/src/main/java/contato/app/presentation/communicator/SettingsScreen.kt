package contato.app.presentation.communicator

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.presentation.theme.ContatoIcons
import kotlinx.coroutines.launch

/**
 * Ajustes: conta (login com Google numa janela que explica o que muda) e preferências.
 * Sem o Firebase configurado no projeto, o botão aparece desligado com "Disponível em breve".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTab(session: CommunicatorSession, c: CommColors) {
    var showLogin by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val user = session.account.user

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ScreenHeading("Ajustes", c, Modifier.padding(start = 4.dp, top = 16.dp))
        Spacer(Modifier.height(16.dp))

        // ── Conta ──
        if (user == null) {
            LoginInviteCard(c, onClick = { showLogin = true })
        } else {
            AccountCard(user.name ?: "Sua conta", user.email, c, onSignOut = { confirmSignOut = true })
        }

        // ── Preferências ──
        SectionLabel("Preferências", c)
        CommCard(c) {
            SwitchRow(
                title = "Guardar conversas",
                subtitle = if (user == null) "Ficam só neste celular, até você apagar." else "Neste celular e na sua conta, na nuvem.",
                checked = session.historyEnabled,
                enabled = true,
                c = c,
                onToggle = { session.setHistory(!session.historyEnabled) },
            )
            HorizontalDivider(color = c.border)
            SwitchRow(
                title = "Simplificar mensagens com IA",
                subtitle = "Sugere uma versão mais curta antes de enviar.",
                checked = false,
                enabled = false,
                badge = "Em breve",
                c = c,
                onToggle = {},
            )
            HorizontalDivider(color = c.border)
            LinkRow(Icons.Default.Delete, "Apagar conversas", null, c, tint = DangerRed) { confirmClear = true }
        }
    }

    if (showLogin) LoginBottomSheet(session, c, onDismiss = { showLogin = false })

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sair da conta?") },
            text = { Text("As conversas continuam neste aparelho. As novas não vão mais para a nuvem.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    session.signOut(context)
                }) { Text("Sair", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancelar") } },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Apagar conversas?") },
            text = {
                Text(
                    if (user == null) "As mensagens serão removidas deste aparelho. Não dá para desfazer."
                    else "As mensagens serão removidas deste aparelho e da sua conta na nuvem. Não dá para desfazer.",
                )
            },
            confirmButton = {
                TextButton(onClick = { session.clearHistory(); confirmClear = false }) {
                    Text("Apagar", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancelar") } },
        )
    }
}

/** Cartão-convite (referência: cartão com ícone redondo e seta). Abre a janela de login. */
@Composable
private fun LoginInviteCard(c: CommColors, onClick: () -> Unit) {
    val gradient = Brush.linearGradient(
        if (c.dark) listOf(Color(0xFF1F3A7A), Color(0xFF1E2838)) else listOf(Color(0xFFDDE8FF), Color(0xFFF7F9FF)),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(gradient)
            .border(1.dp, c.border, RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClickLabel = "Entrar com Google", onClick = onClick)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(c.surface), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.AccountCircle, null, tint = c.accent, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("Salve suas conversas na nuvem", color = c.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(2.dp))
            Text("Entre com o Google para guardar conversas e preferências e usar em outro celular.",
                color = c.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
        }
    }
}

/** Conta conectada: nome, e-mail e "Sair". */
@Composable
private fun AccountCard(name: String, email: String?, c: CommColors, onSignOut: () -> Unit) {
    CommCard(c) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PeerAvatar(name, 52.dp, c, online = false)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = c.onSurface, maxLines = 1,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
                if (email != null) {
                    Text(email, color = c.muted, maxLines = 1,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
                }
                Spacer(Modifier.height(6.dp))
                ToneChip("Conversas salvas na nuvem", Tone.SUCCESS, c, showCheck = true)
            }
        }
        HorizontalDivider(color = c.border)
        LinkRow(Icons.AutoMirrored.Filled.ExitToApp, "Sair da conta", null, c, onClick = onSignOut)
    }
}

/**
 * Janela de login (bottom sheet, referência "Send to"): explica o que muda e tem o botão do Google.
 * Usada nos Ajustes e no aviso "Você está sem login" da lista de conversas.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun LoginBottomSheet(session: CommunicatorSession, c: CommColors, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val account = session.account
    fun close() {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }
    // Entrou: fecha a janela sozinha.
    LaunchedEffect(account.user) {
        if (account.user != null) close()
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState, containerColor = c.surface) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 16.dp, bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Entrar com Google", color = c.onSurface, modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold))
                IconButton(onClick = ::close) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(c.bg), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Close, contentDescription = "Fechar", tint = c.onSurface, modifier = Modifier.size(18.dp))
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Text("Com uma conta, o ConTato pode:", color = c.muted, modifier = Modifier.padding(end = 8.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, lineHeight = 22.sp))
            Spacer(Modifier.height(16.dp))
            Column(Modifier.padding(end = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Benefit(ContatoIcons.Cloud, "Guardar suas conversas na nuvem", "E recuperar tudo se trocar de celular.", c)
                Benefit(Icons.Default.Star, "Levar seu nome junto", "O nome que aparece para o celular tátil.", c)
                Benefit(Icons.Default.Lock, "Proteger seus dados", "Só você acessa. Dá para sair e apagar quando quiser.", c)
            }
            Spacer(Modifier.height(24.dp))
            GoogleButton(
                enabled = account.isConfigured && !account.busy,
                busy = account.busy,
                c = c,
                modifier = Modifier.padding(end = 8.dp),
                onClick = { session.signIn(context) },
            )
            Spacer(Modifier.height(10.dp))
            val error = account.error
            when {
                !account.isConfigured -> Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.Center) {
                    ToneChip("Disponível em breve", Tone.INFO, c)
                }
                error != null -> Text(error, color = DangerRed, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp).semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Ao entrar, você concorda com os Termos de Uso e a Política de Privacidade do ConTato.",
                color = c.muted, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp),
            )
        }
    }
}

/** Botão "Continuar com Google": branco com borda (padrão de marca do Google), com carregando. */
@Composable
private fun GoogleButton(enabled: Boolean, busy: Boolean, c: CommColors, modifier: Modifier, onClick: () -> Unit) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .background(if (enabled || busy) c.surface else c.border)
            .border(1.dp, if (enabled) c.muted.copy(alpha = 0.5f) else c.border, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), color = c.accent, strokeWidth = 2.dp)
            Spacer(Modifier.width(12.dp))
            Text("Entrando...", color = c.onSurface,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
        } else {
            Text("G", color = if (enabled) Color(0xFF4285F4) else c.muted,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
            Spacer(Modifier.width(12.dp))
            Text("Continuar com Google", color = if (enabled) c.onSurface else c.muted,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun Benefit(icon: ImageVector, title: String, body: String, c: CommColors) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(c.accentSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.onSurface,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold))
            Text(body, color = c.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean,
    c: CommColors,
    badge: String? = null,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .clickable(enabled = enabled, role = Role.Switch, onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = c.onSurface.copy(alpha = if (enabled) 1f else 0.6f), modifier = Modifier.weight(1f, fill = false),
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold))
                if (badge != null) {
                    Spacer(Modifier.width(8.dp))
                    ToneChip(badge, Tone.INFO, c)
                }
            }
            Text(subtitle, color = c.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = c.sent))
    }
}

