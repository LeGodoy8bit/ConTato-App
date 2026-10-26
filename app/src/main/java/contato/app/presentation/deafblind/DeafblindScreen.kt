package contato.app.presentation.deafblind

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
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
    // Ícones da barra de status escuros no tema claro e claros no alto contraste.
    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.let { w ->
            WindowCompat.getInsetsController(w, view).apply {
                isAppearanceLightStatusBars = !colors.dark
                isAppearanceLightNavigationBars = !colors.dark
            }
        }
    }
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
 * Layout do protótipo, pensado para o toque:
 *  - topo bem pequeno (status, mensagem e resposta): é só apoio para quem acompanha;
 *  - a cela: grade 3 × 2 até as bordas, com bolas grandes (cada sexto da área é tocável);
 *  - a área de gestos, larga e discreta;
 *  - o menu embaixo.
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
        // Área de gestos: perto de 30% da tela, nunca menor que um polegar confortável.
        val gestureHeight = (maxHeight * 0.30f).coerceIn(190.dp, 320.dp)

        Column(Modifier.fillMaxSize()) {
            SupportStrip(session, colors)
            BrailleCell(
                points = session.points,
                colors = colors,
                dimmed = !session.canCompose,
                onToggle = session::togglePoint,
                modifier = Modifier.weight(1f).fillMaxWidth().padding(vertical = 4.dp), // até as bordas
            )
            GestureArea(session, colors, Modifier.height(gestureHeight))
            BottomMenuBar(colors, unread = session.unreadTotal, onMenu = { showMenu = true })
        }
    }

    if (showMenu) {
        MenuSheet(
            session = session,
            colors = colors,
            onDismiss = { showMenu = false },
            onOpen = { showMenu = false; onOpen(it) },
        )
    }
}

// ───────────────────────── Faixa de apoio (pequena) ─────────────────────────

/**
 * Status, mensagem e resposta numa faixa pequena e discreta.
 * A pessoa surdocega sente tudo pela vibração; isto é para quem está ajudando acompanhar.
 */
@Composable
private fun SupportStrip(session: DeafblindSession, colors: DbColors) {
    val state = session.connectionState
    val connected = state == ConnectionState.CONNECTED
    val reduce = motionReduced()
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        1f, 0.35f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a",
    )
    val status = when {
        connected -> "Conectado com ${session.peerName ?: "comunicador"}"
        state == ConnectionState.DISCONNECTED || state == ConnectionState.CONNECTION_ERROR -> "Sem conexão · procurando"
        else -> "Procurando o comunicador"
    }
    val message = session.message
    val playing = session.isPlaying
    val index = session.playingIndex

    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .semantics(mergeDescendants = true) { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(7.dp).clip(CircleShape)
                    .background((if (connected) colors.online else colors.searching).copy(alpha = if (connected || reduce) 1f else pulse)),
            )
            Spacer(Modifier.width(6.dp))
            AnimatedContent(targetState = status, transitionSpec = { textSwap() }, label = "status", modifier = Modifier.weight(1f)) { t ->
                Text(t, color = colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
            }
            if (playing && message != null && index >= 0) {
                Text("letra ${index + 1}/${message.length}", color = colors.accent,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
            }
        }
        // Mensagem recebida, com a letra que está vibrando em destaque.
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallLabel("Mensagem", colors)
            if (message == null) {
                Text("—", color = colors.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
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
                Text(annotated, maxLines = 1, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallLabel("Resposta", colors)
            Text(
                if (session.composed.isEmpty()) "—" else "${session.composed}_",
                color = colors.onBg, maxLines = 1, overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            )
        }
    }
}

@Composable
private fun SmallLabel(text: String, colors: DbColors) {
    Text(text, color = colors.muted.copy(alpha = 0.8f), modifier = Modifier.width(70.dp),
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
}

/** Troca de texto no lugar (transitions.dev "text states swap"). */
private fun textSwap() =
    (fadeIn(tween(Motion.QUICK, easing = Motion.InOut)) + slideInVertically(tween(Motion.QUICK)) { it / 4 }) togetherWith
            (fadeOut(tween(Motion.QUICK, easing = Motion.InOut)) + slideOutVertically(tween(Motion.QUICK)) { -it / 4 })

// ───────────────────────── Menu embaixo ─────────────────────────

@Composable
private fun BottomMenuBar(colors: DbColors, unread: Int, onMenu: () -> Unit) {
    Box(Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
        Row(
            Modifier
                .clip(CircleShape)
                .background(colors.surface)
                .clickable(role = Role.Button, onClickLabel = "Abrir menu: treino, conversas, ajustes e conta", onClick = onMenu)
                .heightIn(min = 48.dp)
                .padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box {
                Icon(Icons.Default.Menu, contentDescription = null, tint = colors.onBg, modifier = Modifier.size(22.dp))
                if (unread > 0) {
                    Box(Modifier.align(Alignment.TopEnd).offset(x = 3.dp, y = (-2).dp).size(8.dp).clip(CircleShape).background(Color(0xFFFF5A5F)))
                }
            }
            Spacer(Modifier.width(10.dp))
            Text("Menu", color = colors.onBg, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
        }
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
                    e == null -> colors.onGesture.copy(alpha = 0.22f)
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
        // Bem apagado: é lembrete para a pessoa de apoio, não pode competir com o toque.
        Icon(icon, null, tint = colors.onGesture.copy(alpha = 0.28f), modifier = Modifier.size(20.dp))
        Text(gesture, color = colors.onGesture.copy(alpha = 0.28f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
        Text(action, color = colors.onGesture.copy(alpha = 0.22f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
    }
}

// ───────────────────────── Menu ─────────────────────────

/** Menu (bottom sheet): Treino, Conversas, Ajustes e a conta, com itens grandes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MenuSheet(session: DeafblindSession, colors: DbColors, onDismiss: () -> Unit, onOpen: (DbScreen) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val account = session.account
    val unread = session.unreadTotal
    fun go(target: DbScreen) {
        scope.launch { state.hide() }.invokeOnCompletion { onOpen(target) }
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = state, containerColor = colors.bg) {
        Column(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
            Text("Menu", color = colors.onBg, modifier = Modifier.padding(start = 8.dp, bottom = 12.dp).semantics { heading() },
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold))
            MenuItem(ContatoIcons.School, "Treino", "Letras, palavras e frases, em módulos", colors) { go(DbScreen.TRAINING) }
            Spacer(Modifier.height(10.dp))
            MenuItem(ContatoIcons.Chat, "Conversas",
                if (unread > 0) "$unread mensagem(ns) nova(s)" else "Histórico das conversas", colors) { go(DbScreen.CONVERSATIONS) }
            Spacer(Modifier.height(10.dp))
            MenuItem(Icons.Default.Settings, "Ajustes", "Vibração, nome do celular, texto e cores", colors) { go(DbScreen.SETTINGS) }
            Spacer(Modifier.height(10.dp))
            val user = account.user
            if (user == null) {
                MenuItem(
                    ContatoIcons.Cloud,
                    if (account.busy) "Entrando..." else "Entrar com Google",
                    if (account.isConfigured) account.error ?: "Pessoa de apoio: salve conversas e ajustes na nuvem"
                    else "Disponível em breve",
                    colors,
                ) { if (account.isConfigured && !account.busy) session.signIn(context) }
            } else {
                MenuItem(ContatoIcons.Cloud, user.name ?: "Conta conectada", "Conversas e ajustes salvos na nuvem", colors) {
                    go(DbScreen.SETTINGS)
                }
            }
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
