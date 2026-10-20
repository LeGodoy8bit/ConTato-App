package contato.app.presentation.deafblind

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.core.connection.NearbyPermissions
import contato.app.domain.state.ConnectionState
import contato.app.presentation.theme.ContatoIcons
import contato.app.presentation.theme.Motion
import contato.app.presentation.theme.motionReduced
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs

internal enum class DbScreen { HOME, TRAINING, CONVERSATIONS, SETTINGS }

/**
 * Raiz do modo Surdocego: permissões (feitas pela pessoa de apoio), tela principal
 * e as telas de Treino, Conversas e Ajustes (abertas pelo menu do topo).
 *
 * O tamanho do texto escolhido nos Ajustes vale para todas as telas do modo.
 */
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

    val colors = remember(session.settings.theme) { dbColors(session.settings.theme) }
    var screen by rememberSaveable { mutableStateOf(DbScreen.HOME) }
    // Chegou mensagem com outra tela aberta: volta para a principal, onde ela é mostrada.
    LaunchedEffect(session.isPlaying) {
        if (session.isPlaying) screen = DbScreen.HOME
    }
    BackHandler(enabled = screen != DbScreen.HOME) { screen = DbScreen.HOME }

    val density = LocalDensity.current
    val scaled = Density(density.density, density.fontScale * session.settings.textSize.scale)
    val reduce = motionReduced()

    CompositionLocalProvider(LocalDensity provides scaled) {
        if (!permissionsGranted) {
            PermissionScreen(
                denied = denied,
                colors = colors,
                onRequest = { launcher.launch(NearbyPermissions.getRequiredPermissions()) },
                onOpenSettings = onOpenAppSettings,
            )
        } else {
            AnimatedContent(
                targetState = screen,
                transitionSpec = {
                    if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    else {
                        val forward = targetState != DbScreen.HOME
                        (slideInHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) { if (forward) it / 5 else -it / 5 } +
                                fadeIn(tween(Motion.FAST))) togetherWith
                                (slideOutHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) { if (forward) -it / 5 else it / 5 } +
                                        fadeOut(tween(Motion.QUICK)))
                    }
                },
                label = "deafblind",
            ) { target ->
                when (target) {
                    DbScreen.HOME -> DeafblindHome(session, colors, onOpen = { screen = it })
                    DbScreen.TRAINING -> DeafblindTrainingScreen(session, colors, onBack = { screen = DbScreen.HOME })
                    DbScreen.CONVERSATIONS -> DeafblindConversationsScreen(session, colors, onBack = { screen = DbScreen.HOME })
                    DbScreen.SETTINGS -> DeafblindSettingsScreen(session, colors, onBack = { screen = DbScreen.HOME })
                }
            }
        }
    }
}

// ───────────────────────── Tela principal (protótipo) ─────────────────────────

/**
 * Layout do protótipo: topo compacto (status + mensagem), a cela de 6 pontos ocupando
 * o máximo de espaço e, embaixo, a área de gestos.
 */
@Composable
private fun DeafblindHome(session: DeafblindSession, colors: DbColors, onOpen: (DbScreen) -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        // Área de gestos: cerca de 1/4 da tela, nunca menor que um polegar confortável.
        val gestureHeight = (maxHeight * 0.24f).coerceIn(150.dp, 260.dp)

        Column(Modifier.fillMaxSize()) {
            TopBar(session, colors, onMenu = { showMenu = true })
            MessageStrip(session, colors)
            ReplyLine(session, colors)

            // Cela: o maior ponto que couber na largura e na altura que sobram.
            BoxWithConstraints(
                Modifier.weight(1f).fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                val byWidth = maxWidth / (2f + CELL_COL_GAP)
                val byHeight = maxHeight / (3f + 2f * CELL_ROW_GAP)
                val dot = minOf(byWidth, byHeight, 200.dp).coerceAtLeast(48.dp)
                BrailleCell(
                    points = session.points,
                    dotSize = dot,
                    colors = colors,
                    dimmed = !session.canCompose,
                    onToggle = session::togglePoint,
                )
            }

            GestureArea(session, colors, Modifier.height(gestureHeight))
        }
    }

    if (showMenu) {
        MenuSheet(
            colors = colors,
            unread = session.unreadTotal,
            onDismiss = { showMenu = false },
            onOpen = { showMenu = false; onOpen(it) },
        )
    }
}

// ───────────────────────── Topo ─────────────────────────

@Composable
private fun TopBar(session: DeafblindSession, colors: DbColors, onMenu: () -> Unit) {
    val state = session.connectionState
    val connected = state == ConnectionState.CONNECTED
    val reduce = motionReduced()
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        1f, 0.35f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a",
    )
    val text = when {
        connected -> "Conectado com ${session.peerName ?: "comunicador"}"
        state == ConnectionState.DISCONNECTED || state == ConnectionState.CONNECTION_ERROR -> "Sem conexão. Procurando"
        else -> "Procurando o comunicador"
    }
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier
                .weight(1f)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
                .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(10.dp).clip(CircleShape)
                    .background((if (connected) colors.online else colors.searching).copy(alpha = if (connected || reduce) 1f else pulse)),
            )
            Spacer(Modifier.width(10.dp))
            AnimatedContent(targetState = text, transitionSpec = { textSwap() }, label = "status") { t ->
                Text(t, color = colors.onBg, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
            }
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(colors.surface)
                .border(1.dp, colors.border, CircleShape)
                .clickable(role = Role.Button, onClickLabel = "Abrir menu: treino, conversas e ajustes", onClick = onMenu),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Menu, contentDescription = "Menu", tint = colors.onBg)
            if (session.unreadTotal > 0) {
                Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(10.dp).clip(CircleShape).background(Color(0xFFFF5A5F)))
            }
        }
    }
}

/** Troca de texto no lugar (transitions.dev "text states swap"). */
private fun textSwap() =
    (fadeIn(tween(Motion.QUICK, easing = Motion.InOut)) + slideInVertically(tween(Motion.QUICK)) { it / 4 }) togetherWith
            (fadeOut(tween(Motion.QUICK, easing = Motion.InOut)) + slideOutVertically(tween(Motion.QUICK)) { -it / 4 })

// ───────────────────────── Mensagem recebida (compacta) ─────────────────────────

@Composable
private fun MessageStrip(session: DeafblindSession, colors: DbColors) {
    val message = session.message
    val playing = session.isPlaying
    val index = session.playingIndex
    val label = when {
        message == null -> "Aguardando mensagem"
        playing && index >= 0 -> "Vibrando letra ${index + 1} de ${message.length}"
        playing -> "Vibrando"
        else -> "Mensagem recebida"
    }
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(targetState = label, transitionSpec = { textSwap() }, label = "label", modifier = Modifier.weight(1f)) { l ->
                Text(l, color = if (playing) colors.accent else colors.muted,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
            }
            if (message != null && !playing) {
                Row(Modifier.clearAndSetSemantics { }, verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.KeyboardArrowDown, null, tint = colors.muted, modifier = Modifier.size(18.dp))
                    Text("repetir", color = colors.muted, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp))
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (message == null) {
            Text("A mensagem aparece aqui e vibra letra por letra.", color = colors.muted,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 22.sp))
        } else {
            val annotated = buildAnnotatedString {
                message.forEachIndexed { i, ch ->
                    when {
                        playing && i == index -> withStyle(
                            SpanStyle(color = colors.highlightFg, background = colors.highlightBg, fontWeight = FontWeight.ExtraBold)
                        ) { append(if (ch == ' ') '·' else ch) }
                        playing && i < index -> withStyle(SpanStyle(color = colors.muted)) { append(ch) }
                        else -> withStyle(SpanStyle(color = colors.onBg)) { append(ch) }
                    }
                }
            }
            Text(annotated, maxLines = 2, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp))
        }
    }
}

@Composable
private fun ReplyLine(session: DeafblindSession, colors: DbColors) {
    val composed = session.composed
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 4.dp).semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Resposta  ", color = colors.muted, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
        Text(
            if (composed.isEmpty()) "—" else "${composed}_",
            color = colors.onBg, maxLines = 1, overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp),
        )
    }
}

// ───────────────────────── Área de gestos ─────────────────────────

/**
 * Área de gestos (retângulo azul claro do protótipo):
 *   toque = confirmar a letra (ou parar a vibração)
 *   ← apagar · → espaço · ↑ enviar · ↓ repetir a mensagem
 * Ao reconhecer um gesto, a área pisca de leve e o nome do comando aparece no meio.
 * Com TalkBack, os mesmos comandos ficam como ações do elemento.
 */
@Composable
private fun GestureArea(session: DeafblindSession, colors: DbColors, modifier: Modifier) {
    val confirm by rememberUpdatedState(session::confirmLetter)
    val delete by rememberUpdatedState(session::deleteLast)
    val space by rememberUpdatedState(session::addSpace)
    val send by rememberUpdatedState(session::send)
    val repeat by rememberUpdatedState(session::repeatLast)
    val reduce = motionReduced()

    // Pisca a cor quando um comando é reconhecido (azul) ou recusado (vermelho).
    val flash = remember(colors) { Animatable(colors.gesture) }
    LaunchedEffect(session.echo?.key, colors) {
        val echo = session.echo
        if (echo == null || reduce) {
            flash.snapTo(colors.gesture)
            return@LaunchedEffect
        }
        val tint = if (echo.ok) colors.dotOn.copy(alpha = 0.35f) else colors.error.copy(alpha = 0.3f)
        flash.snapTo(tint.over(colors.gesture))
        flash.animateTo(colors.gesture, tween(Motion.SLOW, easing = Motion.SmoothOut))
    }

    Column(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(flash.value)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { confirm() })
            }
            .pointerInput(Unit) {
                val threshold = 56.dp.toPx()
                var total = Offset.Zero
                detectDragGestures(
                    onDragStart = { total = Offset.Zero },
                    onDrag = { change, amount -> change.consume(); total += amount },
                    onDragEnd = {
                        val horizontal = abs(total.x) > abs(total.y)
                        when {
                            horizontal && total.x < -threshold -> delete()
                            horizontal && total.x > threshold -> space()
                            !horizontal && total.y < -threshold -> send()
                            !horizontal && total.y > threshold -> repeat()
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
                    CustomAccessibilityAction("Enviar resposta") { send(); true },
                    CustomAccessibilityAction("Repetir mensagem") { repeat(); true },
                )
            }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EchoLine(session.echo, colors, Modifier.weight(1f))
        if (session.settings.showGestureHints) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                Hint(ContatoIcons.TouchApp, "toque", "confirma", colors)
                Hint(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "esquerda", "apaga", colors)
                Hint(Icons.AutoMirrored.Filled.KeyboardArrowRight, "direita", "espaço", colors)
                Hint(Icons.Default.KeyboardArrowUp, "cima", "envia", colors)
                Hint(Icons.Default.KeyboardArrowDown, "baixo", "repete", colors)
            }
        }
    }
}

/** Cor translúcida aplicada sobre outra, resultando numa cor opaca (para o "flash"). */
private fun Color.over(background: Color): Color = Color(
    red = red * alpha + background.red * (1 - alpha),
    green = green * alpha + background.green * (1 - alpha),
    blue = blue * alpha + background.blue * (1 - alpha),
    alpha = 1f,
)

@Composable
private fun EchoLine(echo: CommandEcho?, colors: DbColors, modifier: Modifier) {
    var visible by remember { mutableStateOf<CommandEcho?>(null) }
    LaunchedEffect(echo?.key) {
        visible = echo
        if (echo != null) {
            delay(2500)
            visible = null
        }
    }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        AnimatedContent(
            targetState = visible,
            transitionSpec = {
                (fadeIn(tween(Motion.FAST)) + scaleIn(tween(Motion.FAST, easing = Motion.Bounce), initialScale = 0.9f)) togetherWith
                        (fadeOut(tween(Motion.QUICK)) + scaleOut(tween(Motion.QUICK), targetScale = 0.98f))
            },
            label = "echo",
        ) { e ->
            Text(
                e?.text ?: "Área de gestos",
                color = when {
                    e == null -> colors.onGesture.copy(alpha = 0.7f)
                    e.ok -> colors.onGesture
                    else -> colors.error
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun Hint(icon: ImageVector, gesture: String, action: String, colors: DbColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clearAndSetSemantics { }) {
        Icon(icon, null, tint = colors.onGesture, modifier = Modifier.size(22.dp))
        Text(gesture, color = colors.onGesture, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold))
        Text(action, color = colors.onGesture.copy(alpha = 0.75f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
    }
}

// ───────────────────────── Menu ─────────────────────────

/** Menu (bottom sheet): Treino, Conversas e Ajustes, com itens grandes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuSheet(colors: DbColors, unread: Int, onDismiss: () -> Unit, onOpen: (DbScreen) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    fun go(target: DbScreen) {
        scope.launch { state.hide() }.invokeOnCompletion { onOpen(target) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = colors.bg) {
        Column(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            Text("Menu", color = colors.onBg, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp).semantics { heading() },
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold))
            MenuItem(ContatoIcons.School, "Treino", "Sinta cada letra e pratique na cela", colors) { go(DbScreen.TRAINING) }
            Spacer(Modifier.height(10.dp))
            MenuItem(ContatoIcons.Chat, "Conversas",
                if (unread > 0) "$unread mensagem(ns) nova(s)" else "Histórico e login na nuvem", colors) { go(DbScreen.CONVERSATIONS) }
            Spacer(Modifier.height(10.dp))
            MenuItem(Icons.Default.Settings, "Ajustes", "Vibração, tamanho do texto e cores", colors) { go(DbScreen.SETTINGS) }
        }
    }
}

@Composable
private fun MenuItem(icon: ImageVector, title: String, subtitle: String, colors: DbColors, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(colors.dotOff), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = colors.accent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onBg, style = MaterialTheme.typography.titleMedium.copy(fontSize = 19.sp, fontWeight = FontWeight.Bold))
            Text(subtitle, color = colors.muted, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp))
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.muted)
    }
}

// ───────────────────────── Permissões ─────────────────────────

@Composable
private fun PermissionScreen(denied: Boolean, colors: DbColors, onRequest: () -> Unit, onOpenSettings: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(colors.bg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(40.dp))
        Box(Modifier.size(96.dp).clip(RoundedCornerShape(32.dp)).background(colors.surface), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Lock, null, tint = colors.accent, modifier = Modifier.size(40.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Pessoa de apoio: permita a conexão", color = colors.onBg, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(12.dp))
        Text(
            "O ConTato usa Bluetooth e Wi-Fi para encontrar o celular do comunicador, sem internet. " +
                    "Em alguns Android a localização também é pedida para isso. Nada é rastreado.",
            color = colors.muted, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        )
        Spacer(Modifier.height(28.dp))
        DbButton(if (denied) "Tentar de novo" else "Permitir", colors, onClick = onRequest)
        if (denied) {
            TextButton(onClick = onOpenSettings, modifier = Modifier.heightIn(min = 48.dp)) {
                Text("Abrir configurações do app", color = colors.accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
