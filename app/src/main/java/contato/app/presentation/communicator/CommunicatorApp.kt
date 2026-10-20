package contato.app.presentation.communicator

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.core.connection.NearbyPermissions
import contato.app.domain.state.ConnectionState
import contato.app.presentation.theme.Motion
import contato.app.presentation.theme.motionReduced
import kotlinx.coroutines.delay

private enum class CommScreen { PERMISSION, CONNECT, MAIN, INFO }

/**
 * Raiz do modo Comunicador:
 *  1. Permissões (pedidas sozinhas ao entrar).
 *  2. Conexão: procura sozinha (se a busca automática estiver ligada) e mostra cada etapa.
 *  3. Conectou: mostra "Conectado" por um instante e abre as abas Conversas / Perfil / Ajustes.
 *  "Sobre o ConTato" abre por cima, já na aba pedida (Como funciona ou Matriz Braille).
 */
@Composable
internal fun CommunicatorApp(
    session: CommunicatorSession,
    permissionsGranted: Boolean,
    onPermissionsResult: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    val granted = permissionsGranted
    var denied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        denied = result.values.any { !it }
        onPermissionsResult()
    }

    LaunchedEffect(Unit) {
        if (!granted) launcher.launch(NearbyPermissions.getRequiredPermissions())
    }
    LaunchedEffect(granted) {
        if (granted) session.start()
    }

    var showMain by rememberSaveable { mutableStateOf(session.hasConnected) }
    var infoTab by rememberSaveable { mutableStateOf<InfoTab?>(null) }

    // Conectou na tela de conexão: deixa o "Conectado" aparecer e abre as conversas.
    LaunchedEffect(session.connectionState) {
        if (session.connectionState == ConnectionState.CONNECTED && !showMain) {
            delay(1200)
            showMain = true
        }
    }

    BackHandler(enabled = infoTab != null) { infoTab = null }

    val target = when {
        !granted -> CommScreen.PERMISSION
        infoTab != null -> CommScreen.INFO
        !showMain -> CommScreen.CONNECT
        else -> CommScreen.MAIN
    }
    val reduce = motionReduced()
    // Guarda o estado de cada tela (ex.: a aba aberta) quando "Sobre" abre por cima.
    val screenStates = rememberSaveableStateHolder()

    Box(Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = target,
            transitionSpec = {
                if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                else (fadeIn(tween(Motion.FAST, easing = Motion.SmoothOut)) +
                        scaleIn(tween(Motion.FAST, easing = Motion.SmoothOut), initialScale = 0.97f)) togetherWith
                        (fadeOut(tween(Motion.QUICK)) + scaleOut(tween(Motion.QUICK), targetScale = 0.99f))
            },
            label = "comm",
        ) { screen ->
            screenStates.SaveableStateProvider(screen.name) {
            when (screen) {
                CommScreen.PERMISSION -> PermissionScreen(
                    denied = denied,
                    onRequest = { launcher.launch(NearbyPermissions.getRequiredPermissions()) },
                    onOpenSettings = onOpenAppSettings,
                )
                CommScreen.INFO -> InfoScreen(
                    initialTab = infoTab ?: InfoTab.HOW,
                    onBack = { infoTab = null },
                    onPreviewLetter = session::previewLetter,
                )
                CommScreen.CONNECT -> ConnectScreen(
                    state = session.connectionState,
                    peerName = session.peerName,
                    autoSearch = session.autoSearchEnabled,
                    onSearchAgain = session::searchAgain,
                    onSkip = { showMain = true },
                )
                CommScreen.MAIN -> MainScreen(session = session, onOpenInfo = { infoTab = it })
            }
            }
        }

        BannerHost(
            banner = session.banner,
            onDismiss = { session.banner = null },
            modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding(),
        )
    }
}

// ───────────────────────── Fundo claro com brilho azul (referências) ─────────────────────────

@Composable
private fun GlowScreen(c: CommColors, content: @Composable ColumnScope.() -> Unit) {
    val glow = if (c.dark) Color(0xFF2B5BD7).copy(alpha = 0.35f) else Color(0xFF8DB4FF).copy(alpha = 0.45f)
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .background(Brush.radialGradient(listOf(glow, Color.Transparent), center = Offset(0f, 0f), radius = 1400f))
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        val viewport = maxHeight
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewport)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
    }
}

@Composable
private fun Title(title: String, subtitle: String, c: CommColors) {
    Text(
        title, color = c.onSurface, textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(horizontal = 8.dp).semantics { heading(); liveRegion = LiveRegionMode.Polite },
    )
    Spacer(Modifier.height(10.dp))
    Text(
        subtitle, color = c.muted, textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        modifier = Modifier.widthIn(max = 420.dp).padding(horizontal = 12.dp),
    )
}

/** Cartão branco do rodapé, estilo "bottom sheet" das referências. */
@Composable
private fun ColumnScope.BottomSheetCard(c: CommColors, content: @Composable ColumnScope.() -> Unit) {
    Spacer(Modifier.weight(1f).heightIn(min = 24.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(32.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

// ───────────────────────── Permissões ─────────────────────────

@Composable
private fun PermissionScreen(denied: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    val c = rememberCommColors()
    GlowScreen(c) {
        Spacer(Modifier.height(40.dp))
        Box(
            Modifier.size(96.dp).clip(RoundedCornerShape(32.dp)).background(c.surface).border(1.dp, c.border, RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = c.accent, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        ToneChip(if (denied) "Permissão negada" else "Passo 1 de 2", if (denied) Tone.DANGER else Tone.INFO, c)
        Spacer(Modifier.height(14.dp))
        Title(
            if (denied) "Precisamos da sua permissão" else "Permita a conexão",
            "O ConTato liga os dois celulares por Bluetooth e Wi-Fi, sem usar internet.",
            c,
        )
        BottomSheetCard(c) {
            PermissionRow(Icons.Default.Phone, "Dispositivos próximos", "Para encontrar o celular tátil.", c)
            Spacer(Modifier.height(14.dp))
            PermissionRow(Icons.Default.LocationOn, "Localização", "Pedida só em alguns Android para a busca. Nada é rastreado.", c)
            Spacer(Modifier.height(20.dp))
            ArrowPill(if (denied) "Tentar de novo" else "Permitir", onRequest)
            if (denied) {
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Default.Settings, null, tint = c.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Abrir configurações do app", color = c.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(icon: ImageVector, title: String, body: String, c: CommColors) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(c.accentSoft), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
            Text(body, color = c.muted, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp))
        }
    }
}

// ───────────────────────── Conexão ─────────────────────────

@Composable
private fun ConnectScreen(
    state: ConnectionState,
    peerName: String?,
    autoSearch: Boolean,
    onSearchAgain: () -> Unit,
    onSkip: () -> Unit,
) {
    val c = rememberCommColors()
    val connected = state == ConnectionState.CONNECTED
    val idle = state == ConnectionState.DISCONNECTED
    val failed = state == ConnectionState.CONNECTION_ERROR
    val step = when (state) {
        ConnectionState.SCANNING -> 0
        ConnectionState.DEVICE_FOUND, ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> 1
        ConnectionState.CONNECTED -> 2
        else -> -1
    }
    val peer = peerName ?: CommunicatorSession.DEFAULT_PEER

    GlowScreen(c) {
        Spacer(Modifier.height(24.dp))
        PhonesIllustration(searching = step in 0..1, connected = connected, c = c)
        Spacer(Modifier.height(20.dp))
        AnimatedContent(targetState = connectionLabel(state, peerName), transitionSpec = { textSwapTransition() }, label = "chip") {
            ToneChip(it, connectionTone(state), c, showCheck = connected)
        }
        Spacer(Modifier.height(14.dp))
        Title(
            when {
                connected -> "Tudo pronto!"
                step == 1 -> "Quase lá..."
                failed -> "Não deu certo desta vez"
                idle && !autoSearch -> "Pronto para parear"
                idle -> "Ainda não encontramos"
                else -> "Procurando o celular tátil"
            },
            when {
                connected -> "Você está conectado com $peer."
                failed || idle && autoSearch -> "Confira se o outro celular está no modo Surdocego, perto deste e com o Bluetooth ligado."
                idle -> "A busca automática está desligada. Toque em Parear agora para procurar."
                else -> "Deixe os dois celulares próximos. No modo Surdocego, a busca começa sozinha."
            },
            c,
        )

        BottomSheetCard(c) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StepSegment("Procurar", done = step > 0, current = step == 0, c = c, modifier = Modifier.weight(1f))
                StepSegment("Conectar", done = step > 1, current = step == 1, c = c, modifier = Modifier.weight(1f))
                StepSegment("Pronto", done = connected, current = false, c = c, modifier = Modifier.weight(1f))
            }
            if (!connected) {
                Spacer(Modifier.height(20.dp))
                ArrowPill(if (idle && !autoSearch) "Parear agora" else "Procurar de novo", onSearchAgain)
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onSkip, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text("Ver conversas sem conectar", color = c.accent, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** Segmento de progresso com rótulo (referência das barras "Backlog / In Progress / Completed"). */
@Composable
private fun StepSegment(label: String, done: Boolean, current: Boolean, c: CommColors, modifier: Modifier) {
    val reduce = motionReduced()
    val bar by animateColorAsState(
        when { done -> c.online; current -> c.accent; else -> c.border },
        if (reduce) tween(0) else tween(Motion.MEDIUM, easing = Motion.SmoothOut), label = "seg",
    )
    val pulse = if (current && !reduce) {
        rememberInfiniteTransition(label = "segPulse").animateFloat(
            0.45f, 1f, infiniteRepeatable(tween(900, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse), label = "a",
        ).value
    } else 1f
    val spoken = label + when { done -> ", concluído"; current -> ", em andamento"; else -> "" }
    Column(
        modifier.clearAndSetSemantics { contentDescription = spoken },
        horizontalAlignment = Alignment.Start,
    ) {
        Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(bar.copy(alpha = bar.alpha * pulse)))
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (done) {
                Icon(Icons.Default.Check, null, tint = c.online, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                label,
                color = if (done || current) c.onSurface else c.muted,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = if (current) FontWeight.Bold else FontWeight.Medium),
            )
        }
    }
}

/** Dois celulares e ondas entre eles. As ondas param com "reduzir animações". */
@Composable
private fun PhonesIllustration(searching: Boolean, connected: Boolean, c: CommColors) {
    val reduce = motionReduced()
    val t = if (searching && !reduce) {
        rememberInfiniteTransition(label = "waves").animateFloat(
            0f, 1f, infiniteRepeatable(tween(1800, easing = LinearEasing)), label = "t",
        ).value
    } else 0f
    val link by animateColorAsState(if (connected) c.online else c.accent, tween(Motion.SLOW), label = "link")

    Canvas(Modifier.size(width = 260.dp, height = 150.dp).clearAndSetSemantics { }) {
        val pw = size.width * 0.26f
        val ph = size.height * 0.92f
        val top = (size.height - ph) / 2f
        drawPhone(Offset(0f, top), Size(pw, ph), c.sent, Color.White.copy(alpha = 0.9f), filledCell = false)
        drawPhone(Offset(size.width - pw, top), Size(pw, ph), c.surface, link, filledCell = true, border = c.border)

        val y = size.height / 2f
        val x0 = pw + 10.dp.toPx()
        val x1 = size.width - pw - 10.dp.toPx()
        if (connected) {
            drawLine(link, Offset(x0, y), Offset(x1, y), strokeWidth = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
        } else {
            val n = 5
            for (i in 0 until n) {
                val p = ((i.toFloat() / n) + t) % 1f
                val x = x0 + (x1 - x0) * p
                val a = if (searching) (1f - kotlin.math.abs(p - 0.5f) * 2f).coerceIn(0.15f, 1f) else 0.25f
                drawCircle(link.copy(alpha = a), 5.dp.toPx(), Offset(x, y))
            }
        }
    }
}

private fun DrawScope.drawPhone(at: Offset, s: Size, body: Color, dots: Color, filledCell: Boolean, border: Color? = null) {
    val r = CornerRadius(s.width * 0.22f)
    drawRoundRect(body, at, s, r)
    if (border != null) drawRoundRect(border, at, s, r, style = Stroke(1.5.dp.toPx()))
    val dot = s.width * 0.1f
    for (i in 0 until 6) {
        val cx = at.x + s.width * (if (i < 3) 0.36f else 0.64f)
        val cy = at.y + s.height * (0.34f + 0.16f * (i % 3))
        val on = i in listOf(1, 2, 3, 4)
        if (filledCell && on || !filledCell) drawCircle(dots.copy(alpha = if (on) 1f else 0.35f), dot, Offset(cx, cy))
        else drawCircle(dots.copy(alpha = 0.35f), dot - 1f, Offset(cx, cy), style = Stroke(1.2.dp.toPx()))
    }
}
