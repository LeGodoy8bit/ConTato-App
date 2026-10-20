package contato.app.presentation.communicator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.data.CommunicatorProfilePreferencesRepository
import contato.app.domain.state.ConnectionState
import contato.app.presentation.theme.motionReduced

/**
 * Perfil: quem é você, o pareamento com o celular tátil e "Sobre o ConTato".
 * (Preferências e conta ficam em Ajustes.)
 */
@Composable
internal fun ProfileTab(session: CommunicatorSession, c: CommColors, onOpenInfo: (InfoTab) -> Unit) {
    var editingName by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ScreenHeading("Perfil", c, Modifier.padding(start = 4.dp, top = 16.dp))

        // ── Identidade ──
        Spacer(Modifier.height(16.dp))
        CommCard(c) {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Avatar(session.displayName, 56.dp, c)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(session.displayName, color = c.onSurface,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold))
                    Text("Comunicador", color = c.muted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
                }
                IconButton(onClick = { editingName = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Editar nome", tint = c.accent)
                }
            }
        }

        // ── Pareamento ──
        SectionLabel("Pareamento", c)
        PairingCard(session, c)

        // ── Sobre ──
        SectionLabel("Sobre o ConTato", c)
        CommCard(c) {
            LinkRow(Icons.Default.Info, "Como funciona", "O caminho da mensagem até a vibração.", c) { onOpenInfo(InfoTab.HOW) }
            HorizontalDivider(color = c.border, modifier = Modifier.padding(start = 66.dp))
            LinkRow(Icons.Default.Phone, "Matriz Braille", "As 26 letras. Toque numa para sentir.", c) { onOpenInfo(InfoTab.MATRIX) }
        }
    }

    if (editingName) {
        NameDialog(
            current = session.displayName,
            onDismiss = { editingName = false },
            onSave = { session.rename(it); editingName = false },
        )
    }
}

/**
 * Cartão de pareamento: estado atual, com quem está conectado, busca automática e o botão
 * "Parear agora" (para procurar de novo manualmente, ex.: depois do login).
 */
@Composable
private fun PairingCard(session: CommunicatorSession, c: CommColors) {
    val state = session.connectionState
    val connected = state == ConnectionState.CONNECTED
    val searching = !connected && state != ConnectionState.DISCONNECTED && state != ConnectionState.CONNECTION_ERROR

    CommCard(c) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            PairingIcon(searching = searching, connected = connected, c = c)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                AnimatedContent(
                    targetState = if (connected) session.peerName ?: CommunicatorSession.DEFAULT_PEER else "Nenhum aparelho",
                    transitionSpec = { textSwapTransition() },
                    label = "peer",
                ) { t ->
                    Text(t, color = c.onSurface,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.height(4.dp))
                ToneChip(if (connected) "Conectado" else connectionLabel(state, null), connectionTone(state), c)
            }
        }

        HorizontalDivider(color = c.border)

        Row(
            Modifier
                .clickable(role = Role.Switch) { session.setAutoSearch(!session.autoSearchEnabled) }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Procurar automaticamente", color = c.onSurface,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold))
                Text("Ao abrir o app e quando a conexão cair.", color = c.muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
            }
            Spacer(Modifier.width(12.dp))
            Switch(checked = session.autoSearchEnabled, onCheckedChange = null,
                colors = SwitchDefaults.colors(checkedTrackColor = c.sent))
        }

        Box(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            if (connected) {
                OutlinedButton(
                    onClick = session::disconnect,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = CircleShape,
                ) { Text("Desconectar", color = c.onSurface, fontWeight = FontWeight.SemiBold) }
            } else {
                ArrowPill(if (searching) "Procurando..." else "Parear agora", onClick = session::searchAgain)
            }
        }
    }
}

/** Ícone de pareamento: ondas enquanto procura, verde quando conectado. */
@Composable
private fun PairingIcon(searching: Boolean, connected: Boolean, c: CommColors) {
    val reduce = motionReduced()
    val t by rememberInfiniteTransition(label = "pair").animateFloat(
        0f, 1f, infiniteRepeatable(tween(1600, easing = LinearEasing)), label = "t",
    )
    Box(Modifier.size(48.dp).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        if (searching && !reduce) {
            Canvas(Modifier.fillMaxSize()) {
                drawCircle(c.searching.copy(alpha = (1f - t) * 0.5f), size.minDimension / 2f * (0.6f + 0.4f * t), style = Stroke(2.dp.toPx()))
            }
        }
        Box(
            Modifier.size(40.dp).clip(CircleShape)
                .background(if (connected) c.online.copy(alpha = 0.15f) else c.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (searching) Icons.Default.Refresh else Icons.Default.Phone, null,
                tint = if (connected) c.online else c.accent, modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
internal fun LinkRow(icon: ImageVector, title: String, subtitle: String?, c: CommColors, tint: Color = c.accent, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (tint == c.accent) c.onSurface else tint,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold))
            if (subtitle != null) {
                Text(subtitle, color = c.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = c.muted)
    }
}

@Composable
private fun NameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(current) }
    val max = CommunicatorProfilePreferencesRepository.MAX_NAME_LENGTH
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seu nome") },
        text = {
            Column {
                Text("Aparece no celular tátil quando vocês se conectam.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.length <= max) value = it },
                    label = { Text("Nome") },
                    supportingText = { Text("${value.length}/$max") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text("Salvar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
