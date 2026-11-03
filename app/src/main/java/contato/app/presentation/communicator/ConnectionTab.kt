package contato.app.presentation.communicator

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.domain.state.ConnectionState
import androidx.compose.foundation.selection.toggleable

/**
 * Aba Conexão: tudo sobre o pareamento num lugar só, sem travar o app numa tela de busca.
 *  - estado atual com a ilustração dos dois celulares e as etapas Procurar → Conectar → Pronto;
 *  - botão principal (Conectar / Ver a conexão / Desconectar);
 *  - busca automática;
 *  - aparelhos com quem você já conversou.
 */
@Composable
internal fun ConnectionTab(session: CommunicatorSession, c: CommColors, onConnect: () -> Unit, onOpenConversation: (String) -> Unit) {
    val state = session.connectionState
    val connected = state == ConnectionState.CONNECTED
    val idle = state == ConnectionState.DISCONNECTED || state == ConnectionState.CONNECTION_ERROR
    val step = when (state) {
        ConnectionState.SCANNING -> 0
        ConnectionState.DEVICE_FOUND, ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> 1
        ConnectionState.CONNECTED -> 2
        else -> -1
    }
    val peer = session.peerName ?: CommunicatorSession.DEFAULT_PEER

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ScreenHeading("Conexão", c, Modifier.padding(start = 4.dp, top = 16.dp).semantics { heading() })
        Spacer(Modifier.height(16.dp))

        // Cartão principal com brilho azul e a ilustração.
        val glow = if (c.dark) Color(0xFF2B5BD7).copy(alpha = 0.30f) else Color(0xFFBBD2FF).copy(alpha = 0.55f)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(c.surface)
                .background(Brush.verticalGradient(listOf(glow, Color.Transparent)))
                .border(1.dp, c.border, RoundedCornerShape(28.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            PhonesIllustration(searching = step in 0..1, connected = connected, c = c)
            Spacer(Modifier.height(16.dp))
            AnimatedContent(targetState = connectionLabel(state, session.peerName), transitionSpec = { textSwapTransition() }, label = "chip") {
                ToneChip(it, connectionTone(state), c, showCheck = connected)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    connected -> "Você está conectado com $peer."
                    step >= 0 -> "Deixe os dois celulares próximos. No modo Surdocego, a busca começa sozinha."
                    session.autoSearchEnabled -> "Confira se o outro celular está no modo Surdocego, perto deste e com o Bluetooth ligado."
                    else -> "A busca automática está desligada. Toque em Conectar."
                },
                color = c.muted, textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 22.sp),
            )
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StepSegment("Procurar", done = step > 0, current = step == 0, c = c, modifier = Modifier.weight(1f))
                StepSegment("Conectar", done = step > 1, current = step == 1, c = c, modifier = Modifier.weight(1f))
                StepSegment("Pronto", done = connected, current = false, c = c, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(20.dp))
            when {
                connected -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PillButton("Abrir conversa", c, primary = true, modifier = Modifier.weight(1f)) {
                        session.activeConversationId?.let(onOpenConversation)
                    }
                    PillButton("Desconectar", c, primary = false, modifier = Modifier.weight(1f), onClick = session::disconnect)
                }
                idle -> ArrowPill("Conectar", onConnect)
                else -> PillButton("Ver a conexão", c, primary = false, modifier = Modifier.fillMaxWidth(), onClick = onConnect)
            }
        }

        SectionLabel("Preferências", c)
        CommCard(c) {
            Row(
                Modifier
                    .toggleable(value = session.autoSearchEnabled, role = Role.Switch, onValueChange = { session.setAutoSearch(it) })
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Procurar automaticamente", color = c.onSurface,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
                    Text("Ao abrir o app e quando a conexão cair.", color = c.muted,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = session.autoSearchEnabled, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = c.sent))
            }
            HorizontalDivider(color = c.border)
            Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Seu nome na conexão", color = c.muted, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp))
                    Text(session.displayName, color = c.onSurface,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
                }
                Text("Troque no Perfil", color = c.muted, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp))
            }
        }

        if (session.conversations.isNotEmpty()) {
            SectionLabel("Aparelhos recentes", c)
            CommCard(c) {
                session.conversations.take(6).forEachIndexed { i, conversation ->
                    val online = connected && conversation.id == session.activeConversationId
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { onOpenConversation(conversation.id) }
                            .clearAndSetSemantics {
                                contentDescription = "${conversation.peerName}${if (online) ", conectado" else ""}. Abrir conversa."
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PeerAvatar(conversation.peerName, 44.dp, c, online)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(conversation.peerName, color = c.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
                            Text(if (online) "Conectado agora" else "Última conversa: ${friendlyTime(conversation.updatedAt)}",
                                color = if (online) c.online else c.muted,
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
                        }
                    }
                    if (i < minOf(5, session.conversations.lastIndex)) HorizontalDivider(color = c.border, modifier = Modifier.padding(start = 72.dp))
                }
            }
            Text(
                "Para conectar com um destes, abra o ConTato no modo Surdocego no outro celular e toque em Conectar.",
                color = c.muted,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
            )
        }
    }
}

/** Botão em pílula simples (cheio ou com contorno). */
@Composable
internal fun PillButton(text: String, c: CommColors, primary: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .heightIn(min = 52.dp)
            .clip(CircleShape)
            .background(if (primary) c.sent else c.surface)
            .border(1.dp, if (primary) c.sent else c.border, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (primary) Color.White else c.onSurface, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
    }
}
