package contato.app.presentation.deafblind

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.R
import contato.app.core.connection.NearbyPermissions
import contato.app.domain.service.BrailleMapper
import contato.app.domain.state.ConnectionState
import contato.app.presentation.theme.Motion
import contato.app.presentation.theme.motionReduced
import kotlinx.coroutines.delay
import kotlin.math.abs

// ───────────────────────── Cores (sempre escuro, alto contraste) ─────────────────────────
// Escolha deliberada: surdocegueira adquirida costuma vir com baixa visão residual.
// Fundo escuro reduz o brilho e o texto branco grande dá o maior contraste possível.
private val Bg = Color(0xFF0D1420)
private val Surface = Color(0xFF1A2333)
private val Border = Color(0xFF2C3850)
private val Muted = Color(0xFFB4C0D3)
private val Accent = Color(0xFF5C97FF)
private val Navy = Color(0xFF131924)
private val Online = Color(0xFF4CC26A)
private val Searching = Color(0xFFF2C14E)
private val ErrorSoft = Color(0xFFF2B8B5)

/** Raiz do modo Surdocego: permissões (feitas pela pessoa de apoio) e depois a tela principal. */
@Composable
internal fun DeafblindApp(
    session: DeafblindSession,
    permissionsGranted: Boolean,
    onPermissionsResult: () -> Unit,
    onOpenAppSettings: () -> Unit,
) {
    var denied by rememberSaveable { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        denied = result.values.any { !it }
        onPermissionsResult()
    }
    LaunchedEffect(Unit) {
        if (!permissionsGranted) launcher.launch(NearbyPermissions.getRequiredPermissions())
    }
    LaunchedEffect(permissionsGranted) {
        if (permissionsGranted) session.onPermissionsGranted()
    }

    if (permissionsGranted) {
        DeafblindScreen(session)
    } else {
        PermissionScreen(
            denied = denied,
            onRequest = { launcher.launch(NearbyPermissions.getRequiredPermissions()) },
            onOpenSettings = onOpenAppSettings,
        )
    }
}

// ───────────────────────── Tela principal ─────────────────────────

@Composable
internal fun DeafblindScreen(session: DeafblindSession) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding(),
    ) {
        // Pontos grandes, mas cabendo em qualquer tela: limitados pela largura e pela altura.
        val dot = minOf(maxWidth * 0.22f, maxHeight * 0.095f, 96.dp).coerceAtLeast(60.dp)

        Column(Modifier.fillMaxSize()) {
            StatusRow(session.connectionState, session.peerName)
            MessageCard(
                message = session.message,
                playingIndex = session.playingIndex,
                isPlaying = session.isPlaying,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(12.dp))
            ComposeLine(session)
            Spacer(Modifier.height(12.dp))
            BrailleInput(
                points = session.points,
                enabled = session.canCompose,
                dotSize = dot,
                onToggle = session::togglePoint,
            )
            Spacer(Modifier.height(14.dp))
            GestureZone(session)
        }
    }
}

// ───────────────────────── Topo: status da conexão ─────────────────────────

@Composable
private fun StatusRow(state: ConnectionState, peer: String?) {
    val connected = state == ConnectionState.CONNECTED
    val searching = !connected && state != ConnectionState.DISCONNECTED && state != ConnectionState.CONNECTION_ERROR
    val reduce = motionReduced()
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 1f, targetValue = 0.35f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a",
    )
    val text = when {
        connected -> "Conectado com ${peer ?: "comunicador"}"
        searching -> "Procurando o comunicador"
        else -> "Sem conexão. Procurando de novo"
    }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f, fill = false)
                .clip(CircleShape)
                .background(Surface)
                .border(1.dp, Border, CircleShape)
                .padding(horizontal = 14.dp, vertical = 8.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(10.dp).clip(CircleShape)
                    .background((if (connected) Online else Searching).copy(alpha = if (connected || reduce) 1f else pulse)),
            )
            Spacer(Modifier.width(10.dp))
            AnimatedContent(
                targetState = text,
                transitionSpec = { textSwap() },
                label = "status",
            ) { t ->
                Text(t, color = Color.White, maxLines = 2,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold))
            }
        }
        Spacer(Modifier.width(12.dp))
        Image(
            painter = painterResource(R.drawable.logo_contato_dark),
            contentDescription = "ConTato",
            colorFilter = ColorFilter.tint(Color.White),
            modifier = Modifier.size(width = 74.dp, height = 22.dp),
        )
    }
}

/** Troca de texto no lugar (transitions.dev "text states swap"): sobe e some / entra de baixo. */
private fun textSwap() =
    (fadeIn(tween(Motion.QUICK, easing = Motion.InOut)) + slideInVertically(tween(Motion.QUICK)) { it / 4 }) togetherWith
            (fadeOut(tween(Motion.QUICK, easing = Motion.InOut)) + slideOutVertically(tween(Motion.QUICK)) { -it / 4 })

// ───────────────────────── Mensagem recebida ─────────────────────────

@Composable
private fun MessageCard(message: String?, playingIndex: Int, isPlaying: Boolean, modifier: Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(28.dp))
            .padding(20.dp),
    ) {
        val label = when {
            message == null -> "Aguardando mensagem"
            isPlaying && playingIndex >= 0 -> "Vibrando letra ${playingIndex + 1} de ${message.length}"
            isPlaying -> "Vibrando"
            else -> "Mensagem recebida"
        }
        AnimatedContent(targetState = label, transitionSpec = { textSwap() }, label = "label") { l ->
            Text(l, color = if (isPlaying) Accent else Muted, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(10.dp))

        Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            if (message == null) {
                Text(
                    "Quando o comunicador enviar, a mensagem aparece aqui e vibra letra por letra.",
                    color = Muted,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 18.sp, lineHeight = 26.sp),
                )
            } else {
                // Letras já sentidas mais apagadas; a atual em destaque branco.
                val annotated = buildAnnotatedString {
                    message.forEachIndexed { i, ch ->
                        when {
                            isPlaying && i == playingIndex -> withStyle(
                                SpanStyle(color = Navy, background = Color.White, fontWeight = FontWeight.ExtraBold)
                            ) { append(if (ch == ' ') '·' else ch) }
                            isPlaying && i < playingIndex -> withStyle(SpanStyle(color = Muted.copy(alpha = 0.7f))) { append(ch) }
                            else -> withStyle(SpanStyle(color = Color.White)) { append(ch) }
                        }
                    }
                }
                Text(annotated, style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 32.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
            }
        }

        // Rodapé: letra atual + seus pontos, ou a dica de repetir.
        Spacer(Modifier.height(12.dp))
        val current = if (isPlaying) message?.getOrNull(playingIndex) else null
        AnimatedContent(
            targetState = current,
            transitionSpec = {
                (fadeIn(tween(Motion.FAST)) + scaleIn(tween(Motion.FAST, easing = Motion.Bounce), initialScale = 0.85f)) togetherWith
                        fadeOut(tween(Motion.QUICK))
            },
            label = "letter",
        ) { ch ->
            when {
                ch != null -> CurrentLetter(ch)
                message != null -> RepeatHint()
                else -> Spacer(Modifier.height(1.dp))
            }
        }
    }
}

@Composable
private fun CurrentLetter(ch: Char) {
    val points = BrailleMapper.brailleTable[ch]
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(Color.White), contentAlignment = Alignment.Center) {
            Text(if (ch == ' ') "␣" else ch.toString(), color = Navy,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold))
        }
        Spacer(Modifier.width(16.dp))
        if (points != null) {
            MiniCell(points)
            Spacer(Modifier.width(12.dp))
            Text(
                "Pontos " + points.mapIndexedNotNull { i, on -> if (on) i + 1 else null }.joinToString(", "),
                color = Muted, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            )
        } else {
            Text("Espaço entre palavras", color = Muted, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp))
        }
    }
}

@Composable
private fun MiniCell(points: List<Boolean>) {
    Canvas(Modifier.size(width = 30.dp, height = 44.dp).clearAndSetSemantics { }) {
        val r = 5.5.dp.toPx()
        val xs = listOf(r + 1f, size.width - r - 1f)
        val ys = listOf(r + 1f, size.height / 2f, size.height - r - 1f)
        for (i in 0 until 6) {
            val c = Offset(xs[i / 3], ys[i % 3])
            if (points[i]) drawCircle(Color.White, r, c)
            else drawCircle(Muted.copy(alpha = 0.6f), r - 1f, c, style = Stroke(1.5.dp.toPx()))
        }
    }
}

@Composable
private fun RepeatHint() {
    Row(
        Modifier.clip(CircleShape).background(Accent.copy(alpha = 0.16f)).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.KeyboardArrowUp, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
        Text("Deslize para cima embaixo para repetir", color = Color.White,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
    }
}

// ───────────────────────── Resposta em composição ─────────────────────────

@Composable
private fun ComposeLine(session: DeafblindSession) {
    val hint = when {
        session.connectionState != ConnectionState.CONNECTED -> "A resposta libera quando conectar."
        session.message == null -> "A cela libera depois da primeira mensagem."
        session.isPlaying -> "Aguarde a vibração terminar."
        else -> "Marque os pontos e toque embaixo para confirmar."
    }
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Sua resposta", color = Muted, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(2.dp))
        if (session.composed.isEmpty()) {
            Text(hint, color = Muted, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 20.sp))
        } else {
            Text(session.composed + "_", color = Color.White, maxLines = 2,
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp))
        }
    }
}

// ───────────────────────── Cela Braille (6 pontos) ─────────────────────────

/** Pontos na posição real da cela: 1 2 3 na coluna da esquerda, 4 5 6 na da direita. */
@Composable
private fun BrailleInput(points: List<Boolean>, enabled: Boolean, dotSize: Dp, onToggle: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        for (col in 0..1) {
            Column(verticalArrangement = Arrangement.spacedBy(dotSize * 0.16f)) {
                for (row in 0..2) {
                    val index = col * 3 + row
                    DotButton(index, points[index], enabled, dotSize) { onToggle(index) }
                }
            }
            if (col == 0) Spacer(Modifier.width(dotSize * 0.55f))
        }
    }
}

@Composable
private fun DotButton(index: Int, active: Boolean, enabled: Boolean, size: Dp, onToggle: () -> Unit) {
    val reduce = motionReduced()
    val fill by animateColorAsState(
        if (active) Color.White else Color.Transparent,
        animationSpec = if (reduce) snap() else tween(Motion.QUICK), label = "fill",
    )
    val scale by animateFloatAsState(
        if (active) 1f else 0.94f,
        animationSpec = if (reduce) snap() else tween(Motion.FAST, easing = Motion.Bounce), label = "scale",
    )
    Box(
        Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(fill)
            .border(3.dp, Color.White.copy(alpha = if (enabled) 0.85f else 0.25f), CircleShape)
            .toggleable(value = active, enabled = enabled, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics {
                contentDescription = "Ponto ${index + 1}"
                stateDescription = if (active) "marcado" else "desmarcado"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${index + 1}",
            color = if (active) Navy else Color.White.copy(alpha = if (enabled) 1f else 0.4f),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = (size.value * 0.3f).sp, fontWeight = FontWeight.Bold),
        )
    }
}

// ───────────────────────── Área de gestos ─────────────────────────

/**
 * Faixa de baixo, onde ficam os comandos por gesto:
 *   toque = confirmar letra (ou parar a vibração) · ← apagar · → espaço · ↑ repetir · segurar = enviar.
 * Com TalkBack, os mesmos comandos aparecem como ações.
 */
@Composable
private fun GestureZone(session: DeafblindSession) {
    val confirm by rememberUpdatedState(session::confirmLetter)
    val delete by rememberUpdatedState(session::deleteLast)
    val space by rememberUpdatedState(session::addSpace)
    val repeat by rememberUpdatedState(session::repeatLast)
    val send by rememberUpdatedState(session::send)

    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 140.dp)
            .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .background(Surface)
            .border(1.dp, Border, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .pointerInput(Unit) {
                detectTapGestures(onTap = { confirm() }, onLongPress = { send() })
            }
            .pointerInput(Unit) {
                val threshold = 64.dp.toPx()
                var total = Offset.Zero
                detectDragGestures(
                    onDragStart = { total = Offset.Zero },
                    onDrag = { change, amount -> change.consume(); total += amount },
                    onDragEnd = {
                        when {
                            abs(total.x) > abs(total.y) && total.x < -threshold -> delete()
                            abs(total.x) > abs(total.y) && total.x > threshold -> space()
                            abs(total.y) > abs(total.x) && total.y < -threshold -> repeat()
                        }
                    },
                )
            }
            .semantics {
                contentDescription = "Área de gestos"
                onClick(label = "Confirmar letra") { confirm(); true }
                customActions = listOf(
                    CustomAccessibilityAction("Apagar") { delete(); true },
                    CustomAccessibilityAction("Espaço") { space(); true },
                    CustomAccessibilityAction("Repetir mensagem") { repeat(); true },
                    CustomAccessibilityAction("Enviar resposta") { send(); true },
                )
            }
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(Border))
        Spacer(Modifier.height(10.dp))
        EchoLine(session.echo)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Legend(Icons.Default.Check, "Toque", "confirma", Modifier.weight(1f))
            Legend(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Esquerda", "apaga", Modifier.weight(1f))
            Legend(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Direita", "espaço", Modifier.weight(1f))
            Legend(Icons.Default.KeyboardArrowUp, "Para cima", "repete", Modifier.weight(1f))
            Legend(Icons.AutoMirrored.Filled.Send, "Segurar", "envia", Modifier.weight(1f))
        }
    }
}

/** Último comando reconhecido; some depois de alguns segundos. */
@Composable
private fun EchoLine(echo: CommandEcho?) {
    var visible by remember { mutableStateOf<CommandEcho?>(null) }
    LaunchedEffect(echo?.key) {
        visible = echo
        if (echo != null) {
            delay(2500)
            visible = null
        }
    }
    AnimatedContent(targetState = visible, transitionSpec = { textSwap() }, label = "echo") { e ->
        Text(
            e?.text ?: "Área de gestos",
            color = when {
                e == null -> Muted
                e.ok -> Color.White
                else -> ErrorSoft
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold),
        )
    }
}

@Composable
private fun Legend(icon: ImageVector, gesture: String, action: String, modifier: Modifier) {
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .padding(horizontal = 2.dp, vertical = 8.dp)
            .clearAndSetSemantics { },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(4.dp))
        Text(gesture, color = Color.White, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.Bold))
        Text(action, color = Muted, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp))
    }
}

// ───────────────────────── Permissões ─────────────────────────

@Composable
private fun PermissionScreen(denied: Boolean, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(40.dp))
        Box(Modifier.size(88.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Lock, null, tint = Navy, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Pessoa de apoio: permita a conexão", color = Color.White, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(12.dp))
        Text(
            "O ConTato usa Bluetooth e Wi-Fi para encontrar o celular do comunicador, sem internet. " +
                    "Em alguns Android a localização também é pedida para isso. Nada é rastreado.",
            color = Muted, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        )
        Spacer(Modifier.height(28.dp))
        Button(
            onClick = onRequest,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Navy),
        ) { Text(if (denied) "Tentar de novo" else "Permitir", fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        if (denied) {
            TextButton(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Abrir configurações do app", color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
