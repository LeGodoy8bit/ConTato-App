package contato.app.presentation.communicator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.domain.model.MessageStatus
import contato.app.domain.state.ConnectionState
import contato.app.presentation.theme.ContatoBlue
import contato.app.presentation.theme.ContatoDarkSurface
import contato.app.presentation.theme.ContatoNavy
import contato.app.presentation.theme.Motion
import contato.app.presentation.theme.motionReduced
import kotlinx.coroutines.delay
import contato.app.presentation.theme.ContatoIcons
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.platform.LocalAccessibilityManager

// ───────────────────────── Cores do modo Comunicador ─────────────────────────

/**
 * Paleta das telas do Comunicador. Fundo claro levemente azulado (como as referências),
 * azul da marca só no que é ação ou mensagem enviada. Contraste AA em todos os pares.
 */
internal class CommColors(
    val bg: Color, val surface: Color, val onSurface: Color, val muted: Color, val border: Color,
    val sent: Color, val onSent: Color, val received: Color, val onReceived: Color,
    val nav: Color, val onNav: Color, val accent: Color, val accentSoft: Color,
    val online: Color, val searching: Color, val offline: Color,
    val dark: Boolean,
    /** Vermelho de erro/apagar com contraste AA nos dois temas. */
    val danger: Color = DangerRed,
)

internal val InkOnWhite = Color(0xFF131924)
internal val DangerRed = Color(0xFFB3261E)

@Composable
internal fun rememberCommColors(dark: Boolean = isSystemInDarkTheme()): CommColors = remember(dark) {
    if (dark) CommColors(
        bg = ContatoDarkSurface, surface = Color(0xFF1E2838), onSurface = Color.White,
        muted = Color(0xFFB4C0D3), border = Color(0xFF2E3A4F),
        sent = ContatoBlue, onSent = Color.White, received = Color(0xFF2A3547), onReceived = Color.White,
        nav = Color(0xFF0E141E), onNav = Color.White, accent = Color(0xFF80AEFF), accentSoft = Color(0xFF5C97FF).copy(alpha = 0.18f),
        online = Color(0xFF4CC26A), searching = Color(0xFFF2C14E), offline = Color(0xFF8D9BB5),
        dark = true, danger = Color(0xFFFFB4AB),
    ) else CommColors(
        bg = Color(0xFFF1F4FB), surface = Color.White, onSurface = ContatoNavy,
        muted = Color(0xFF5B6678), border = Color(0xFFE3E8F2),
        sent = ContatoBlue, onSent = Color.White, received = Color.White, onReceived = ContatoNavy,
        nav = ContatoNavy, onNav = Color.White, accent = ContatoBlue, accentSoft = Color(0xFFEDF3FF),
        online = Color(0xFF18793A), searching = Color(0xFFB7791F), offline = Color(0xFF6F7C93),
        dark = false,
    )
}

/** Texto curto que descreve a conexão. */
internal fun connectionLabel(state: ConnectionState, peer: String?): String = when (state) {
    ConnectionState.CONNECTED -> "Conectado com ${peer ?: CommunicatorSession.DEFAULT_PEER}"
    ConnectionState.SCANNING -> "Procurando..."
    ConnectionState.DEVICE_FOUND, ConnectionState.CONNECTING -> "Conectando..."
    ConnectionState.RECONNECTING -> "Reconectando..."
    ConnectionState.CONNECTION_ERROR -> "Falha na conexão"
    ConnectionState.DISCONNECTED -> "Sem conexão"
}

internal fun connectionDotColor(state: ConnectionState, c: CommColors): Color = when (state) {
    ConnectionState.CONNECTED -> c.online
    ConnectionState.DISCONNECTED, ConnectionState.CONNECTION_ERROR -> c.offline
    else -> c.searching
}

// ───────────────────────── Transições (tokens do transitions.dev) ─────────────────────────

/** Troca de texto no lugar: o antigo sobe e some, o novo entra de baixo (150 ms). */
internal fun textSwapTransition() =
    (fadeIn(tween(Motion.QUICK, easing = Motion.InOut)) + slideInVertically(tween(Motion.QUICK, easing = Motion.InOut)) { it / 3 }) togetherWith
            (fadeOut(tween(Motion.QUICK, easing = Motion.InOut)) + slideOutVertically(tween(Motion.QUICK, easing = Motion.InOut)) { -it / 3 })

// ───────────────────────── Avatar ─────────────────────────

@Composable
internal fun Avatar(name: String, size: Dp, c: CommColors, statusColor: Color? = null, photo: ImageBitmap? = null) {
    Box(Modifier.size(size)) {
        Box(
            Modifier.fillMaxSize().clip(CircleShape).background(c.accentSoft),
            contentAlignment = Alignment.Center,
        ) {
            if (photo != null) {
                Image(photo, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(
                    name.trim().firstOrNull()?.uppercase() ?: "?",
                    color = if (c.dark) Color.White else ContatoBlue,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Bold),
                )
            }
        }
        if (statusColor != null) {
            val dot by animateColorAsState(statusColor, tween(Motion.FAST), label = "dot")
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(size * 0.3f)
                    .clip(CircleShape)
                    .background(c.bg)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(dot),
            )
        }
    }
}

// ───────────────────────── Etiquetas de status ─────────────────────────

internal class ChipStyle(val bg: Color, val border: Color, val fg: Color)

/** Cores das etiquetas (referência: chips com fundo suave, borda e texto do mesmo tom). */
internal enum class Tone { NEUTRAL, INFO, PROGRESS, SUCCESS, DANGER }

internal fun toneStyle(tone: Tone, dark: Boolean): ChipStyle {
    val (lightBg, lightBorder, lightFg, darkFg) = when (tone) {
        Tone.NEUTRAL -> listOf(Color(0xFFF1F3F6), Color(0xFFDDE2EA), Color(0xFF4B5563), Color(0xFFC3CBD8))
        Tone.INFO -> listOf(Color(0xFFEAF1FF), Color(0xFFC7D8FF), Color(0xFF0B46AA), Color(0xFFA8C4FF))
        Tone.PROGRESS -> listOf(Color(0xFFFFF7E0), Color(0xFFF5DFA0), Color(0xFF6B4E00), Color(0xFFF5D27A))
        Tone.SUCCESS -> listOf(Color(0xFFE8F5E9), Color(0xFFBFE3C2), Color(0xFF1B5E20), Color(0xFF9BD8A0))
        Tone.DANGER -> listOf(Color(0xFFFDECEA), Color(0xFFF6C6C0), Color(0xFF8C1D18), Color(0xFFF2B8B5))
    }
    return if (dark) ChipStyle(darkFg.copy(alpha = 0.14f), darkFg.copy(alpha = 0.35f), darkFg)
    else ChipStyle(lightBg, lightBorder, lightFg)
}

internal fun statusTone(status: MessageStatus) = when (status) {
    MessageStatus.SENDING, MessageStatus.SENT -> Tone.NEUTRAL
    MessageStatus.DELIVERED -> Tone.INFO
    MessageStatus.PLAYING -> Tone.PROGRESS
    MessageStatus.PLAYED -> Tone.SUCCESS
    MessageStatus.FAILED -> Tone.DANGER
}

internal fun connectionTone(state: ConnectionState) = when (state) {
    ConnectionState.CONNECTED -> Tone.SUCCESS
    ConnectionState.DISCONNECTED -> Tone.NEUTRAL
    ConnectionState.CONNECTION_ERROR -> Tone.DANGER
    else -> Tone.PROGRESS
}

internal fun statusLabel(status: MessageStatus, progress: PlaybackProgress?): String = when (status) {
    MessageStatus.SENDING -> "Enviando"
    MessageStatus.SENT -> "Enviada"
    MessageStatus.DELIVERED -> "Recebida no celular tátil"
    MessageStatus.PLAYING -> progress?.let {
        val letter = it.character?.let { ch -> if (ch == ' ') "espaço" else ch.toString() } ?: ""
        "Vibrando ${it.index + 1} de ${it.total}: $letter"
    } ?: "Vibrando"
    MessageStatus.PLAYED -> "Sentida"
    MessageStatus.FAILED -> "Não enviada"
}

/** Etiqueta colorida. Cor e texto trocam com animação; o TalkBack anuncia a mudança. */
@Composable
internal fun ToneChip(text: String, tone: Tone, c: CommColors, showCheck: Boolean = false) {
    val reduce = motionReduced()
    val style = toneStyle(tone, c.dark)
    val spec = if (reduce) snap<Color>() else tween(Motion.FAST)
    val bg by animateColorAsState(style.bg, spec, label = "bg")
    val border by animateColorAsState(style.border, spec, label = "border")
    val fg by animateColorAsState(style.fg, spec, label = "fg")
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showCheck) {
            Icon(ContatoIcons.Check, null, tint = fg, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
        }
        AnimatedContent(targetState = text, transitionSpec = { textSwapTransition() }, label = "chip") { t ->
            Text(t, color = fg,
                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

@Composable
internal fun StatusChip(status: MessageStatus, progress: PlaybackProgress?, c: CommColors) {
    ToneChip(statusLabel(status, progress), statusTone(status), c, showCheck = status == MessageStatus.PLAYED)
}

// ───────────────────────── Aviso no topo (sucesso / erro) ─────────────────────────

/** Pílula de aviso no topo (transitions.dev "toast": entra em 400 ms, sai em 350 ms). Some sozinha. */
@Composable
internal fun BannerHost(banner: Banner?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf<Banner?>(null) }
    // Com TalkBack (ou "tempo para agir" maior nas configurações), o Android pede mais tempo: respeita.
    val a11y = LocalAccessibilityManager.current
    LaunchedEffect(banner?.key) {
        if (banner != null) {
            shown = banner
            val base = if (banner.kind == BannerKind.ERROR) 6000L else 3500L
            delay(a11y?.calculateRecommendedTimeoutMillis(base, containsIcons = true, containsText = true) ?: base)
            onDismiss()
        }
    }
    AnimatedVisibility(
        visible = banner != null,
        modifier = modifier,
        enter = slideInVertically(tween(Motion.SLOW, easing = Motion.SmoothOut)) { -it } + fadeIn(tween(Motion.SLOW)),
        exit = slideOutVertically(tween(Motion.MEDIUM, easing = Motion.SmoothOut)) { -it } + fadeOut(tween(Motion.MEDIUM)),
    ) {
        val b = banner ?: shown ?: return@AnimatedVisibility
        val bg = if (b.kind == BannerKind.SUCCESS) Color(0xFF1E7D34) else Color(0xFFA3261E)
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .clip(CircleShape)
                .background(bg)
                .padding(start = 8.dp, end = 4.dp, top = 8.dp, bottom = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Assertive },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(
                    if (b.kind == BannerKind.SUCCESS) ContatoIcons.Check else ContatoIcons.Warning,
                    contentDescription = null, tint = bg, modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(b.title, color = Color.White,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold))
                Text(b.message, color = Color.White, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
            }
            IconButton(onClick = onDismiss) {
                Icon(ContatoIcons.Close, contentDescription = "Fechar aviso", tint = Color.White)
            }
        }
    }
}

// ───────────────────────── Botões e blocos ─────────────────────────

/** Botão principal em pílula com a seta num círculo branco (referência "Get Started"). */
@Composable
internal fun ArrowPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, container: Color = ContatoBlue) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(CircleShape)
            .background(container)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 24.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, color = Color.White, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            Icon(ContatoIcons.ArrowRight, null, tint = container, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
internal fun SectionLabel(text: String, c: CommColors) {
    Text(
        text,
        color = c.muted,
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp, top = 22.dp).semantics { heading() },
    )
}

/** Cartão com cantos de 24 dp (mesmo raio em todas as telas do Comunicador). */
@Composable
internal fun CommCard(c: CommColors, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(c.surface)
            .border(1.dp, c.border, RoundedCornerShape(24.dp)),
        content = content,
    )
}

@Composable
internal fun ScreenHeading(text: String, c: CommColors, modifier: Modifier = Modifier) {
    Text(
        text,
        color = c.onSurface,
        style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        modifier = modifier.semantics { heading() },
    )
}

/**
 * Seletor de duas ou mais opções com a pílula deslizando até a escolhida
 * (transitions.dev "tabs sliding": 250 ms, curva suave).
 */
@Composable
internal fun SegmentedTabs(options: List<String>, selected: Int, c: CommColors, onSelect: (Int) -> Unit) {
    val reduce = motionReduced()
    val density = LocalDensity.current
    var width by remember { mutableStateOf(0.dp) }
    val itemWidth = if (options.isEmpty()) 0.dp else width / options.size
    // Anima a posição (0, 1, 2...) e não a distância: ao abrir a tela a pílula já nasce no lugar certo.
    val pos by animateFloatAsState(
        selected.toFloat(),
        animationSpec = if (reduce) snap() else tween(Motion.FAST, easing = Motion.SmoothOut), label = "pill",
    )
    val offset = itemWidth * pos
    Box(
        Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(CircleShape)
            .background(c.surface)
            .border(1.dp, c.border, CircleShape)
            .padding(4.dp)
            .onSizeChanged { width = with(density) { it.width.toDp() } },
    ) {
        // Pílula que desliza por trás da opção escolhida.
        Box(
            Modifier
                .fillMaxHeight()
                .width(itemWidth)
                .offset(x = offset)
                .clip(CircleShape)
                .background(c.sent),
        )
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, label ->
                val fg by animateColorAsState(if (i == selected) Color.White else c.onSurface, tween(Motion.FAST), label = "fg")
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(CircleShape)
                        .selectable(selected = i == selected, role = Role.Tab) { onSelect(i) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label, color = fg, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold))
                }
            }
        }
    }
}
