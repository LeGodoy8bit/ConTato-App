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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Ajustes: conta (login com Google, abre uma janela explicando) e preferências.
 * O login real (Firebase) entra quando o modelo de dados estiver pronto; a interface já está aqui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsTab(session: CommunicatorSession, c: CommColors) {
    var showLogin by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ScreenHeading("Ajustes", c, Modifier.padding(start = 4.dp, top = 16.dp))
        Spacer(Modifier.height(16.dp))

        // ── Conta: convite para entrar ──
        LoginInviteCard(c, onClick = { showLogin = true })

        // ── Preferências ──
        SectionLabel("Preferências", c)
        CommCard(c) {
            SwitchRow(
                title = "Guardar conversas neste aparelho",
                subtitle = "Ficam só neste celular, até você apagar.",
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

    if (showLogin) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()
        ModalBottomSheet(
            onDismissRequest = { showLogin = false },
            sheetState = sheetState,
            containerColor = c.surface,
        ) {
            LoginSheet(c, onClose = {
                scope.launch { sheetState.hide() }.invokeOnCompletion { showLogin = false }
            })
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Apagar conversas?") },
            text = { Text("As mensagens serão removidas deste aparelho. Não dá para desfazer.") },
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

/** Janela de login (bottom sheet, referência "Send to"): explica o que muda antes de entrar. */
@Composable
private fun LoginSheet(c: CommColors, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 24.dp, end = 16.dp, bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Entrar com Google", color = c.onSurface, modifier = Modifier.weight(1f).semantics { heading() },
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold))
            IconButton(onClick = onClose) {
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
            Benefit(Icons.Default.Refresh, "Guardar suas conversas na nuvem", "E recuperar tudo se trocar de celular.", c)
            Benefit(Icons.Default.Star, "Levar suas preferências junto", "Nome, busca automática e outros ajustes.", c)
            Benefit(Icons.Default.Lock, "Proteger seus dados", "Só você acessa. Dá para sair e apagar quando quiser.", c)
        }
        Spacer(Modifier.height(24.dp))
        // Botão desligado até o Firebase entrar no projeto.
        Row(
            Modifier
                .padding(end = 8.dp)
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .clip(CircleShape)
                .background(c.border)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text("G", color = c.muted, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.width(10.dp))
            Text("Continuar com Google", color = c.muted,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().padding(end = 8.dp), horizontalArrangement = Arrangement.Center) {
            ToneChip("Disponível em breve", Tone.INFO, c)
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

