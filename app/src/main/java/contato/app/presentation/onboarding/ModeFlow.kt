package contato.app.presentation.onboarding

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.R
import contato.app.domain.model.UserMode
import contato.app.presentation.theme.ContatoBlue
import contato.app.presentation.theme.ContatoBlueContainer
import contato.app.presentation.theme.ContatoDarkSurface
import contato.app.presentation.theme.ContatoNavy
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import contato.app.presentation.theme.ContatoIcons

/** TICK: toque leve. CONFIRM: pulso firme. ACCEPTED: pulso duplo (termos aceitos). */
internal enum class HapticKind { TICK, CONFIRM, ACCEPTED }

private enum class Step {
    MODE, TERMS_COMMUNICATOR, TERMS_DEAFBLIND, DOC_TERMS, DOC_PRIVACY, HOWTO_1, HOWTO_2
}

// ── Medidas do Figma: frame de 428 x 926. "k" converte unidades de design em dp. ──
private fun u(n: Number, k: Float): Dp = (n.toFloat() * k).dp
private fun fs(n: Number, k: Float): TextUnit = (n.toFloat() * k).sp

/** Títulos: leve aperto (cerca de -1,3%), comum em apps profissionais. -1sp deixava as letras coladas. */
private val TightSpacing = (-0.4).sp

/** Margem lateral e largura máxima dos botões, iguais em todas as telas. */
private val ButtonsModifier = Modifier.widthIn(max = 420.dp).padding(horizontal = 24.dp)

// ───────────────────────── Cores ─────────────────────────
private class FlowColors(
    val bg: Color, val onBg: Color,
    val primary: Color, val onPrimary: Color, val pressed: Color,
    val secondary: Color, val onSecondary: Color, val secondaryBorder: BorderStroke?,
    val panel: Color, val onPanel: Color, val softText: Color,
    val card: Color, val onCard: Color, val cardIcon: Color,
    val docCard: Color, val onDocCard: Color,
)

private val PressedBlue = Color(0xFFADC2E4)
private val SoftBlue = Color(0xFFCADFFD)
private val CheckBlue = Color(0xFF006FEE)
private val InkOnWhite = Color(0xFF131924)

private val LightFlow = FlowColors(
    bg = ContatoBlue, onBg = Color.White,
    primary = ContatoBlueContainer, onPrimary = InkOnWhite, pressed = PressedBlue,
    secondary = ContatoNavy, onSecondary = Color.White, secondaryBorder = null,
    panel = ContatoNavy, onPanel = Color.White, softText = SoftBlue,
    card = ContatoBlueContainer, onCard = ContatoNavy, cardIcon = SoftBlue,
    docCard = Color.White, onDocCard = ContatoNavy,
)

// Tema escuro: base do Seleção_de_modo_dark.svg; tons de cartão PROVISÓRIOS.
private val DarkFlow = FlowColors(
    bg = ContatoDarkSurface, onBg = Color.White,
    primary = SoftBlue, onPrimary = InkOnWhite, pressed = PressedBlue,
    secondary = Color(0xFF1E2838), onSecondary = Color.White,
    secondaryBorder = BorderStroke(1.dp, SoftBlue.copy(alpha = 0.5f)),
    panel = Color(0xFF1E2838), onPanel = Color.White, softText = SoftBlue,
    card = Color(0xFF2A3547), onCard = Color.White, cardIcon = Color(0xFF3A4A63),
    docCard = Color(0xFF2A3547), onDocCard = Color.White,
)

// ───────────────────────── Fluxo ─────────────────────────

/**
 * Seleção de modo -> (Comunicador) termos | (Surdocego: segurar 5 s) termos por gestos e vibração -> fim.
 * "Saiba como usar" abre o tutorial de 2 páginas. O modo só é salvo DEPOIS do aceite.
 */
@Composable
internal fun ModeFlow(
    startMode: UserMode?,
    onAccepted: (UserMode) -> Unit,
    onHaptic: (HapticKind) -> Unit,
    /** Vibra um texto curto em Braille e devolve a duração em ms (HapticMessagePlaybackService.play). */
    onPlayHapticText: (String) -> Long = { 0L },
    /** Interrompe a vibração em andamento (HapticMessagePlaybackService.stop). */
    onStopHaptic: () -> Unit = {},
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    val colors = if (darkTheme) DarkFlow else LightFlow
    var step by rememberSaveable {
        mutableStateOf(
            when (startMode) {
                UserMode.COMMUNICATOR -> Step.TERMS_COMMUNICATOR
                UserMode.DEAFBLIND -> Step.TERMS_DEAFBLIND
                null -> Step.MODE
            }
        )
    }
    var docBack by rememberSaveable { mutableStateOf(Step.MODE) }
    fun openDoc(doc: Step) { docBack = step; step = doc }

    BackHandler(enabled = step != Step.MODE) {
        step = when (step) {
            Step.DOC_TERMS, Step.DOC_PRIVACY -> docBack
            Step.HOWTO_2 -> Step.HOWTO_1
            else -> Step.MODE
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Escala dos elementos desenhados (cela, logo, ícones). Limitada nas duas pontas:
        // em telas com "tamanho de exibição" grande, k pequeno demais deixava tudo espremido.
        val k = minOf(maxWidth.value / 428f, maxHeight.value / 926f).coerceIn(0.8f, 1.4f)

        Crossfade(targetState = step, animationSpec = tween(250), label = "modeFlow") { s ->
            when (s) {
                Step.MODE -> ModeSelectionScreen(
                    c = colors, k = k, darkTheme = darkTheme,
                    onCommunicator = { step = Step.TERMS_COMMUNICATOR },
                    onHowTo = { step = Step.HOWTO_1 },
                    onDeafblind = { onHaptic(HapticKind.CONFIRM); step = Step.TERMS_DEAFBLIND },
                    onHaptic = onHaptic,
                )
                Step.HOWTO_1 -> HowToScreen(
                    c = colors, communicator = true,
                    onPrimary = { step = Step.HOWTO_2 },
                    onSecondary = { step = Step.TERMS_COMMUNICATOR },
                )
                Step.HOWTO_2 -> HowToScreen(
                    c = colors, communicator = false,
                    onPrimary = { step = Step.MODE },
                    onSecondary = { step = Step.HOWTO_1 },
                )
                Step.TERMS_COMMUNICATOR -> TermsCommunicatorScreen(
                    c = colors, k = k,
                    onBack = { step = Step.MODE },
                    onOpenTerms = { openDoc(Step.DOC_TERMS) },
                    onOpenPrivacy = { openDoc(Step.DOC_PRIVACY) },
                    onStart = { onHaptic(HapticKind.CONFIRM); onAccepted(UserMode.COMMUNICATOR) },
                )
                Step.TERMS_DEAFBLIND -> TermsDeafblindScreen(
                    c = colors, k = k,
                    onPlayHapticText = onPlayHapticText,
                    onStopHaptic = onStopHaptic,
                    onBack = { step = Step.MODE },
                    onOpenTerms = { openDoc(Step.DOC_TERMS) },
                    onOpenPrivacy = { openDoc(Step.DOC_PRIVACY) },
                    onAccept = { onHaptic(HapticKind.ACCEPTED); onAccepted(UserMode.DEAFBLIND) },
                    onDecline = { onHaptic(HapticKind.TICK); step = Step.MODE },
                )
                Step.DOC_TERMS -> DocumentScreen(colors, k, LegalTexts.termsOfUse, ContatoIcons.Info) { step = docBack }
                Step.DOC_PRIVACY -> DocumentScreen(colors, k, LegalTexts.privacyPolicy, ContatoIcons.Lock) { step = docBack }
            }
        }
    }
}

// ───────────────────────── Layout responsivo ─────────────────────────

@Composable
private fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Base de TODAS as telas do fluxo.
 *
 * - Se o conteúdo cabe: ocupa a tela inteira e os espaços elásticos ([Gap]) distribuem a sobra.
 * - Se não cabe (fonte grande, "tamanho de exibição" grande, celular baixo): a tela rola,
 *   em vez de espremer ou cortar o conteúdo.
 *
 * Funciona porque a coluna tem altura MÍNIMA igual à da tela: os pesos (weight) dividem só
 * o espaço que sobra, e somem quando não sobra nada. Por isso, weight vai SÓ em espaçadores,
 * nunca em blocos com conteúdo.
 */
@Composable
private fun ScreenColumn(
    modifier: Modifier,
    content: @Composable ColumnScope.(viewportHeight: Dp) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val viewport = maxHeight
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = viewport),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content(viewport) }
    }
}

/** Espaço com um mínimo garantido + uma parte elástica (some quando a tela precisa rolar). */
@Composable
private fun ColumnScope.Gap(min: Dp, weight: Float = 1f) {
    Spacer(Modifier.height(min))
    Spacer(Modifier.weight(weight))
}

@Composable
private fun navBottom(): Dp {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return maxOf(20.dp, nav + 12.dp)
}

/**
 * Painel escuro do rodapé (cantos de cima arredondados) que sempre vai até o fim da tela,
 * com o logo embaixo. O preenchimento do meio é elástico; o conteúdo nunca é cortado.
 */
@Composable
private fun ColumnScope.BottomPanel(
    c: FlowColors,
    k: Float,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(c.panel, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
            .padding(start = 20.dp, end = 20.dp, top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
    Spacer(Modifier.fillMaxWidth().weight(1f).background(c.panel))
    Column(
        Modifier.fillMaxWidth().background(c.panel).padding(top = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Logo(k)
        Spacer(Modifier.height(navBottom()))
    }
}

// ───────────────────────── Componentes ─────────────────────────

/**
 * Botão em pílula. A altura é MÍNIMA (heightIn), não fixa: com fonte grande o botão cresce
 * em vez de cortar o texto.
 */
@Composable
private fun PillButton(
    text: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    pressedContainer: Color,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    enabled: Boolean = true,
    disabledContainer: Color? = null,
    border: BorderStroke? = null,
    fontSize: TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.SemiBold,
    icon: ImageVector? = null,
    fillWidth: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = (if (fillWidth) modifier.fillMaxWidth() else modifier).heightIn(min = height),
        shape = CircleShape,
        border = border,
        interactionSource = interaction,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (pressed) pressedContainer else container,
            contentColor = content,
            disabledContainerColor = disabledContainer ?: container.copy(alpha = 0.45f),
            disabledContentColor = content.copy(alpha = 0.8f),
        ),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = fontWeight, fontSize = fontSize, letterSpacing = 0.sp, lineHeight = (fontSize.value + 4).sp,
            ),
            textAlign = TextAlign.Center,
        )
    }
}

/** Logo 100% branco (o Figma usa branco). O tint faz "Con" e "Tato" ficarem brancos juntos. */
@Composable
private fun Logo(k: Float) {
    Image(
        painter = painterResource(R.drawable.logo_contato_dark),
        contentDescription = "ConTato",
        colorFilter = ColorFilter.tint(Color.White),
        modifier = Modifier.size(width = u(93, k), height = u(28.6f, k)),
    )
}

@Composable
private fun bottomMargin(k: Float): Dp {
    val nav = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return maxOf(u(32, k), nav + 12.dp)
}

@Composable
private fun BackButton(tint: Color, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        IconButton(onClick = onBack) { // 48dp
            Icon(ContatoIcons.ArrowLeft, contentDescription = "Voltar", tint = tint)
        }
    }
}

/** Título padrão das telas do fluxo. */
@Composable
private fun ScreenTitle(text: String, color: Color, size: Int = 28) {
    Text(
        text,
        color = color,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = size.sp, lineHeight = (size + 8).sp, fontWeight = FontWeight.Bold, letterSpacing = TightSpacing,
        ),
        modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() },
    )
}

/** Texto de apoio padrão (máx. ~2 linhas em tela normal). */
@Composable
private fun ScreenSubtitle(text: String, color: Color) {
    Text(
        text,
        color = color,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
        modifier = Modifier.widthIn(max = 420.dp).padding(horizontal = 32.dp),
    )
}

// ───────────────────────── Tela 1: seleção de modo ─────────────────────────

@Composable
private fun ModeSelectionScreen(
    c: FlowColors,
    k: Float,
    darkTheme: Boolean,
    onCommunicator: () -> Unit,
    onHowTo: () -> Unit,
    onDeafblind: () -> Unit,
    onHaptic: (HapticKind) -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    var secs by remember { mutableStateOf(0) }
    val hapticNow by rememberUpdatedState(onHaptic)
    val deafblindNow by rememberUpdatedState(onDeafblind)

    Box(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            // Segurar 5 s em qualquer ponto livre = modo Surdocego.
            // A vibração começa na hora; a tela "Entrando..." aparece depois de 200 ms
            // (assim um simples arrastar para rolar a tela não faz ela piscar).
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    secs = 0
                    hapticNow(HapticKind.TICK)
                    coroutineScope {
                        val hold = launch {
                            delay(200)
                            pressed = true
                            delay(800)
                            for (s in 1..5) {
                                secs = s
                                if (s < 5) {
                                    hapticNow(HapticKind.TICK)
                                    delay(1000)
                                }
                            }
                            delay(400) // deixa o 5º ponto aparecer
                            deafblindNow()
                        }
                        tryAwaitRelease()
                        hold.cancel()
                    }
                    pressed = false
                    secs = 0
                })
            }
            // Alternativa para TalkBack, onde a exploração por toque atrapalha o "segurar".
            .semantics { onLongClick(label = "Entrar no modo Surdocego") { onDeafblind(); true } },
    ) {
        if (pressed) {
            LoadingLook(c, k, secs)
        } else {
            NormalLook(c, k, darkTheme, onCommunicator = onCommunicator, onHowTo = onHowTo)
        }
    }
}

@Composable
private fun NormalLook(
    c: FlowColors, k: Float, darkTheme: Boolean,
    onCommunicator: () -> Unit, onHowTo: () -> Unit,
) {
    ScreenColumn(Modifier.fillMaxSize().statusBarsPadding()) {
        Gap(24.dp, 0.9f)
        BrailleCellHero(k, darkTheme, frozen = false)
        Spacer(Modifier.height(u(32, k)))
        ScreenTitle("Olá,\nvamos começar?", c.onBg, size = 30)
        Spacer(Modifier.height(12.dp))
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Surdocego") }
                append(", segure a tela por 5 segundos para entrar.")
            },
            color = c.onBg,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
            modifier = Modifier.widthIn(max = 320.dp).padding(horizontal = 32.dp),
        )

        Gap(32.dp, 1f)

        Text(
            "Você é o comunicador?",
            color = c.onBg,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = 0.sp,
            ),
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(14.dp))
        PillButton(
            "Entrar como comunicador", onCommunicator,
            container = c.primary, content = c.onPrimary, pressedContainer = c.pressed,
            modifier = ButtonsModifier, height = 56.dp,
        )
        Spacer(Modifier.height(12.dp))
        PillButton(
            "Saiba como usar o ConTato", onHowTo,
            container = c.secondary, content = c.onSecondary, pressedContainer = c.secondary.copy(alpha = 0.85f),
            border = c.secondaryBorder,
            modifier = ButtonsModifier, height = 52.dp,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(u(36, k)))
        Logo(k)
        Spacer(Modifier.height(bottomMargin(k)))
    }
}

/** Tela "Entrando...": pontos fantasma mostram o caminho; um acende por segundo, até 5. */
@Composable
private fun LoadingLook(c: FlowColors, k: Float, secs: Int) {
    val remaining = (5 - secs).coerceAtLeast(0)
    val scales = List(5) { i ->
        animateFloatAsState(
            targetValue = if (secs > i) 1f else 0f,
            animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
            label = "dot$i",
        ).value
    }
    // 5 pontos em linha reta, bem espaçados (centros, em unidades de design, quadro de 156 x 20).
    val centers = listOf(10f, 44f, 78f, 112f, 146f)

    Column(Modifier.fillMaxSize().statusBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.weight(1f))
        Canvas(Modifier.size(width = u(156, k), height = u(20, k))) {
            val r = 10f * k * density
            centers.forEachIndexed { i, cx ->
                val p = Offset(cx * k * density, 10f * k * density)
                drawCircle(Color.White.copy(alpha = 0.18f), r, p)
                if (scales[i] > 0.01f) drawCircle(Color.White, r * scales[i], p)
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Entrando...",
            color = SoftBlue,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = 30.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold, letterSpacing = TightSpacing,
            ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (remaining == 1) "Segure a tela por mais 1 segundo" else "Segure a tela por mais $remaining segundos",
            color = c.onBg,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(Modifier.weight(1f))
        Logo(k)
        Spacer(Modifier.height(bottomMargin(k)))
    }
}

// ───────────────────────── Tela: como usar (2 páginas) ─────────────────────────

@Composable
private fun HowToScreen(
    c: FlowColors, communicator: Boolean,
    onPrimary: () -> Unit, onSecondary: () -> Unit,
) {
    val page = if (communicator) 0 else 1
    val steps = if (communicator) {
        listOf(
            "Conecte os dispositivos" to "Conecte o seu celular ao dispositivo da pessoa com surdocegueira.",
            "Escreva e envie" to "Digite sua mensagem e envie para o outro dispositivo.",
        )
    } else {
        listOf(
            "Perceba as vibrações" to "O celular recebe a mensagem e reproduz padrões vibratórios inspirados no Braille.",
            "Interaja por gestos" to "Use os gestos definidos no aplicativo para interagir com a interface tátil.",
        )
    }

    ScreenColumn(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) { viewport ->
        Spacer(Modifier.height(16.dp))
        ProgressSegments(current = page, total = 2, modifier = Modifier.padding(horizontal = 24.dp))
        Spacer(Modifier.height(28.dp))
        ScreenTitle(if (communicator) "Modo Comunicador" else "Modo Surdocego", c.onBg)
        Spacer(Modifier.height(8.dp))
        ScreenSubtitle(
            if (communicator) "Envie uma mensagem. A comunicação começa com você."
            else "Sinta a mensagem. A informação chega por estímulos táteis.",
            c.onBg,
        )

        // Altura da ilustração proporcional à tela (nunca some, nunca domina).
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .height((viewport * 0.28f).coerceIn(150.dp, 300.dp))
                .clip(RoundedCornerShape(28.dp))
                .background(Color.White.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            HowToArt(communicator, Modifier.fillMaxSize())
        }

        Spacer(Modifier.weight(1f))

        Column(
            Modifier
                .fillMaxWidth()
                .background(c.panel, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            steps.forEachIndexed { i, (title, body) ->
                if (i > 0) Spacer(Modifier.height(16.dp))
                HowToStepRow(c, i + 1, title, body)
            }
            Spacer(Modifier.height(24.dp))
            PillButton(
                if (communicator) "Próximo" else "Entendi, vamos começar", onPrimary,
                container = c.primary, content = c.onPrimary, pressedContainer = c.pressed,
                height = 56.dp,
            )
            Spacer(Modifier.height(8.dp))
            PillButton(
                if (communicator) "Começar a usar o ConTato" else "Voltar", onSecondary,
                container = Color.Transparent, content = Color.White,
                pressedContainer = Color.White.copy(alpha = 0.14f),
                height = 52.dp,
            )
            Spacer(Modifier.height(navBottom()))
        }
    }
}

@Composable
private fun ProgressSegments(current: Int, total: Int, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Passo ${current + 1} de $total" },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(total) { i ->
            Box(
                Modifier
                    .weight(1f)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = if (i <= current) 1f else 0.3f)),
            )
        }
    }
}

@Composable
private fun HowToStepRow(c: FlowColors, number: Int, title: String, body: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(32.dp).background(Color.White.copy(alpha = 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = c.onPanel,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.onPanel,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(2.dp))
            Text(body, color = c.softText,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp))
        }
    }
}

// Ilustração animada (celular vibrando + ondas). Decorativa para o leitor de tela.
// Com "reduzir animações" ativado, fica parada.

@Composable
private fun HowToArt(communicator: Boolean, modifier: Modifier = Modifier) {
    val reduce = rememberReducedMotion()
    val inf = rememberInfiniteTransition(label = "art")
    val tAnim by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing)),
        label = "t",
    )
    val t = if (reduce) 0.3f else tAnim

    Canvas(modifier.clearAndSetSemantics { }) {
        val s = minOf(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val shake = if (reduce || t > 0.6f) 0f else sin(t * 2f * PI.toFloat() * 9f) * 4f

        if (communicator) {
            val w = s * 0.24f
            val h = s * 0.50f
            val leftX = cx - s * 0.30f
            val rightX = cx + s * 0.30f

            // Celular que envia (mensagem na tela)
            drawPhone(Offset(leftX, cy), w, h, SoftBlue, ContatoNavy, 0f)
            drawRoundRect(
                Color.White,
                Offset(leftX - w * 0.30f, cy - h * 0.10f), Size(w * 0.60f, h * 0.12f),
                CornerRadius(h * 0.06f),
            )
            drawRoundRect(
                SoftBlue.copy(alpha = 0.6f),
                Offset(leftX - w * 0.30f, cy + h * 0.08f), Size(w * 0.42f, h * 0.10f),
                CornerRadius(h * 0.05f),
            )

            // Pontinhos viajando de um celular para o outro
            for (i in 0..2) {
                val p = (((t * 1.5f - i * 0.12f) % 1f) + 1f) % 1f
                val x0 = leftX + w / 2f + s * 0.04f
                val x1 = rightX - w / 2f - s * 0.04f
                drawCircle(
                    Color.White.copy(alpha = sin(p * PI.toFloat()).coerceAtLeast(0f)),
                    s * 0.018f,
                    Offset(x0 + (x1 - x0) * p, cy),
                )
            }

            // Celular que recebe (vibra)
            rotate(shake, Offset(rightX, cy)) {
                drawPhone(Offset(rightX, cy), w, h, Color.White, ContatoNavy, 0f)
                drawBrailleOnScreen(Offset(rightX, cy), w, h, lit = (t * 6f).toInt() % 6)
            }
            drawWaves(rightX, cy, w / 2f, s, t)
        } else {
            val w = s * 0.34f
            val h = s * 0.62f
            rotate(shake, Offset(cx, cy)) {
                drawPhone(Offset(cx, cy), w, h, Color.White, ContatoNavy, 0f)
                drawBrailleOnScreen(Offset(cx, cy), w, h, lit = (t * 6f).toInt() % 6)
            }
            drawWaves(cx, cy, w / 2f, s, t)
        }
    }
}

private fun DrawScope.drawPhone(center: Offset, w: Float, h: Float, body: Color, screen: Color, angle: Float) {
    rotate(angle, center) {
        drawRoundRect(
            body, Offset(center.x - w / 2f, center.y - h / 2f), Size(w, h), CornerRadius(w * 0.18f),
        )
        val m = w * 0.08f
        drawRoundRect(
            screen,
            Offset(center.x - w / 2f + m, center.y - h / 2f + m * 1.6f),
            Size(w - 2f * m, h - m * 3.2f),
            CornerRadius(w * 0.12f),
        )
    }
}

private fun DrawScope.drawBrailleOnScreen(center: Offset, w: Float, h: Float, lit: Int) {
    val r = w * 0.065f
    for (i in 0..5) {
        val col = i / 3
        val row = i % 3
        val x = center.x + (col - 0.5f) * w * 0.30f
        val y = center.y + (row - 1) * h * 0.16f
        drawCircle(if (i == lit) Color.White else SoftBlue.copy(alpha = 0.30f), r, Offset(x, y))
    }
}

private fun DrawScope.drawWaves(cx: Float, cy: Float, halfW: Float, unit: Float, t: Float) {
    val st = Stroke(width = unit * 0.022f, cap = StrokeCap.Round)
    for (i in 0..1) {
        val p = (t * 1.5f + i * 0.5f) % 1f
        val a = (1f - p) * 0.8f
        val r = halfW + unit * 0.04f + p * unit * 0.14f
        val color = Color.White.copy(alpha = a)
        drawArc(color, -38f, 76f, false, Offset(cx - r, cy - r), Size(2f * r, 2f * r), style = st)
        drawArc(color, 142f, 76f, false, Offset(cx - r, cy - r), Size(2f * r, 2f * r), style = st)
    }
}

// ───────────────────────── Tela 2: termos (Comunicador) ─────────────────────────

@Composable
private fun PulsingDot(k: Float) {
    val reduce = rememberReducedMotion()
    val inf = rememberInfiniteTransition(label = "dot")
    val scale by inf.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scale",
    )
    val ring by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing)),
        label = "ring",
    )
    Canvas(Modifier.size(u(72, k)).clearAndSetSemantics { }) {
        val base = size.minDimension * 0.26f
        if (!reduce) {
            drawCircle(Color.White.copy(alpha = (1f - ring) * 0.35f), base * (1f + ring * 0.9f), center)
        }
        drawCircle(Color.White, base * (if (reduce) 1f else scale), center)
    }
}

@Composable
private fun TermsCommunicatorScreen(
    c: FlowColors, k: Float,
    onBack: () -> Unit, onOpenTerms: () -> Unit, onOpenPrivacy: () -> Unit, onStart: () -> Unit,
) {
    var agreed by rememberSaveable { mutableStateOf(false) }

    ScreenColumn(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        BackButton(c.onBg, onBack)
        PulsingDot(k)
        Spacer(Modifier.height(12.dp))
        ScreenTitle("Termos e privacidade", c.onBg)
        Spacer(Modifier.height(10.dp))
        ScreenSubtitle(
            "Antes de começar, conheça como o ConTato funciona e como suas informações são tratadas.",
            c.onBg,
        )
        Spacer(Modifier.height(24.dp))

        BottomPanel(c, k) {
            DocCard(c, ContatoIcons.Info, "Termos de Uso",
                "Conheça as regras de uso do ConTato e os limites do aplicativo.", onOpenTerms)
            Spacer(Modifier.height(12.dp))
            DocCard(c, ContatoIcons.Lock, "Política de Privacidade",
                "Saiba quais informações são tratadas, para quê e quais são os seus direitos.", onOpenPrivacy)
            Spacer(Modifier.height(12.dp))

            // Linha inteira clicável (alvo de toque grande)
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .toggleable(value = agreed, role = Role.Checkbox, onValueChange = { agreed = it })
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .then(
                            if (agreed) Modifier.background(CheckBlue)
                            else Modifier.border(2.dp, SoftBlue, RoundedCornerShape(6.dp))
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (agreed) Icon(ContatoIcons.Check, null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(16.dp))
                Text(
                    "Li e concordo com os Termos de Uso e a Política de Privacidade.",
                    color = c.onPanel,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp),
                )
            }
            Spacer(Modifier.height(12.dp))
            PillButton(
                "Começar", onStart,
                container = Color.White, content = InkOnWhite, pressedContainer = PressedBlue,
                height = 56.dp,
                enabled = agreed, disabledContainer = Color(0xFF8D9BB5),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun DocCard(c: FlowColors, icon: ImageVector, title: String, description: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.card)
            .clickable(onClickLabel = "Abrir $title", role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(c.cardIcon, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = c.onCard, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = c.onCard,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 22.sp))
            Spacer(Modifier.height(4.dp))
            Text(description, color = c.onCard,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp))
        }
        Spacer(Modifier.width(8.dp))
        Icon(ContatoIcons.ChevronRight, contentDescription = null, tint = c.onCard,
            modifier = Modifier.size(24.dp))
    }
}

// ───────────────────────── Tela 2b: termos (Surdocego) ─────────────────────────

private const val DEAFBLIND_ANNOUNCEMENT =
    "Termos e privacidade. Modo Surdocego. " +
            "Resumo: privado, de celular a celular, sem nome, e-mail ou localização. " +
            "Não serve para emergências. " +
            "Toque uma vez na tela para sentir o resumo em vibração. Toque de novo para parar. " +
            "Toque duas vezes para aceitar. Segure a tela para recusar."

/** Pausa entre um item e outro do resumo (maior que a pausa entre palavras, de 1 s). */
private const val SUMMARY_ITEM_GAP_MS = 2000L

/**
 * Termos do modo Surdocego. Diferente da tela do Comunicador de propósito:
 *
 * 1. Em cima, a ÁREA DE TOQUE: um círculo grande mostra que a tela inteira responde ao toque.
 *    Gestos em qualquer ponto livre (não precisa achar botão):
 *      - 1 toque  = vibra o resumo inteiro, item por item; outro toque para;
 *      - 2 toques = aceita (vibração dupla de confirmação);
 *      - segurar  = recusa e volta para a seleção de modo.
 *    Com TalkBack, os mesmos comandos aparecem como ação principal e ações personalizadas.
 *
 * 2. Embaixo, um painel COMPACTO para a pessoa de apoio: ler os documentos e aceitar
 *    durante a configuração inicial. Botões menores, para não competirem com a área de toque.
 */
@Composable
private fun TermsDeafblindScreen(
    c: FlowColors,
    k: Float,
    onPlayHapticText: (String) -> Long,
    onStopHaptic: () -> Unit,
    onBack: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    val items = TermsHapticSummary.items
    val scope = rememberCoroutineScope()
    val view = LocalView.current

    var currentItem by remember { mutableStateOf(-1) } // item vibrando agora (-1 = nenhum)
    var playing by remember { mutableStateOf(false) }
    var playJob by remember { mutableStateOf<Job?>(null) }
    var finished by remember { mutableStateOf(false) }  // evita aceitar/recusar duas vezes

    val playNow by rememberUpdatedState(onPlayHapticText)
    val stopNow by rememberUpdatedState(onStopHaptic)
    val acceptNow by rememberUpdatedState(onAccept)
    val declineNow by rememberUpdatedState(onDecline)

    fun stopSummary() {
        if (playJob?.isActive == true) {
            playJob?.cancel()
            stopNow()
        }
    }

    // 1 toque: vibra o resumo inteiro, um item depois do outro. Se já estiver vibrando, para.
    fun toggleSummary() {
        if (finished) return
        if (playing) {
            stopSummary()
            return
        }
        playJob = scope.launch {
            playing = true
            try {
                for (i in items.indices) {
                    currentItem = i
                    val durationMs = playNow(items[i].hapticText)
                    delay(durationMs + SUMMARY_ITEM_GAP_MS)
                }
            } finally {
                playing = false
                currentItem = -1
            }
        }
    }

    // Para o resumo ANTES de confirmar: assim a vibração de aceite não é cortada.
    fun accept() {
        if (finished) return
        finished = true
        stopSummary()
        acceptNow()
    }

    fun decline() {
        if (finished) return
        finished = true
        stopSummary()
        declineNow()
    }

    // Anuncia o resumo ao abrir (TalkBack e display Braille).
    LaunchedEffect(Unit) {
        delay(700)
        @Suppress("DEPRECATION")
        view.announceForAccessibility(DEAFBLIND_ANNOUNCEMENT)
    }

    // Saiu da tela no meio do resumo (ex.: abriu os Termos de uso): para de vibrar.
    // Só cancela se o RESUMO estiver tocando, para não cortar a vibração de aceite.
    DisposableEffect(Unit) {
        onDispose { if (playJob?.isActive == true) stopNow() }
    }

    ScreenColumn(
        Modifier
            .fillMaxSize()
            .background(c.bg)
            .statusBarsPadding()
            // Gestos em qualquer área livre. Botões consomem o próprio toque.
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { toggleSummary() },
                    onDoubleTap = { accept() },
                    onLongPress = { decline() },
                )
            },
    ) {
        BackButton(c.onBg, onBack)
        Gap(8.dp, 0.5f)

        // ── Área de toque (pessoa surdocega) ──
        Column(
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = DEAFBLIND_ANNOUNCEMENT
                    onClick(label = if (playing) "Parar a vibração" else "Sentir o resumo em vibração") {
                        toggleSummary(); true
                    }
                    customActions = listOf(
                        CustomAccessibilityAction("Aceitar os termos") { accept(); true },
                        CustomAccessibilityAction("Recusar os termos") { decline(); true },
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TouchPad(k, playing)
            Spacer(Modifier.height(12.dp))
            Text(
                if (playing) "Vibrando. Toque para parar." else "Toque para sentir o resumo",
                color = SoftBlue,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(20.dp))
            ScreenTitle("Termos e privacidade", c.onBg, size = 26)
            Spacer(Modifier.height(16.dp))

            Column(
                Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items.forEachIndexed { index, item ->
                    SummaryItemRow(c, item, highlighted = index == currentItem)
                }
            }
            Spacer(Modifier.height(16.dp))

            // Legenda dos gestos (informativa, não clicável).
            Row(
                Modifier.widthIn(max = 420.dp).fillMaxWidth().padding(horizontal = 24.dp).height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                GestureLegend("1 toque", "sentir", c, Modifier.weight(1f))
                GestureLegend("2 toques", "aceitar", c, Modifier.weight(1f), emphasized = true)
                GestureLegend("Segurar", "recusar", c, Modifier.weight(1f))
            }
        }
        Gap(24.dp, 0.5f)

        // ── Painel compacto (pessoa de apoio) ──
        BottomPanel(c, k) {
            Text(
                "Pessoa de apoio",
                color = c.onPanel,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold),
                modifier = Modifier.semantics { heading() },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Leia os termos junto com a pessoa. A decisão de aceitar é dela.",
                color = c.softText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            )
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PillButton(
                    "Termos de uso", onOpenTerms,
                    container = Color.Transparent, content = Color.White,
                    pressedContainer = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                    modifier = Modifier.weight(1f), height = 44.dp, fontSize = 14.sp,
                )
                PillButton(
                    "Privacidade", onOpenPrivacy,
                    container = Color.Transparent, content = Color.White,
                    pressedContainer = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                    modifier = Modifier.weight(1f), height = 44.dp, fontSize = 14.sp,
                )
            }
            Spacer(Modifier.height(10.dp))
            PillButton(
                "Aceitar os termos", { accept() },
                container = Color.White, content = InkOnWhite, pressedContainer = PressedBlue,
                height = 48.dp, fontSize = 15.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}

/**
 * Círculo que representa a área de toque. Parado: anel fino e celular no centro.
 * Vibrando: o celular balança e uma onda sai do círculo. Parado se "reduzir animações".
 */
@Composable
private fun TouchPad(k: Float, active: Boolean) {
    val reduce = rememberReducedMotion()
    val inf = rememberInfiniteTransition(label = "pad")
    val t by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing)),
        label = "t",
    )
    val moving = active && !reduce
    val rot = if (!moving || t > 0.5f) 0f else sin(t * 2f * PI.toFloat() * 8f) * 8f
    val padSize = maxOf(u(128, k), 112.dp)

    Box(Modifier.size(padSize).clearAndSetSemantics { }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(Color.White.copy(alpha = if (active) 0.16f else 0.08f), r * 0.86f, center)
            drawCircle(
                Color.White.copy(alpha = 0.35f), r * 0.86f, center,
                style = Stroke(width = 1.5.dp.toPx()),
            )
            if (moving) {
                drawCircle(
                    Color.White.copy(alpha = (1f - t) * 0.45f), r * (0.86f + t * 0.14f), center,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }
        Box(
            Modifier
                .size(padSize * 0.42f)
                .graphicsLayer { rotationZ = rot }
                .background(Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ContatoIcons.Smartphone, contentDescription = null, tint = ContatoNavy, modifier = Modifier.size(padSize * 0.2f))
        }
    }
}

/** Linha do resumo. Fica destacada enquanto aquele item está vibrando. */
@Composable
private fun SummaryItemRow(c: FlowColors, item: HapticSummaryItem, highlighted: Boolean) {
    val bgAlpha by animateFloatAsState(if (highlighted) 0.18f else 0f, tween(200), label = "rowBg")
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = bgAlpha))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(32.dp).background(Color.White.copy(alpha = if (highlighted) 1f else 0.16f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                item.icon, contentDescription = null,
                tint = if (highlighted) ContatoNavy else Color.White,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            item.label, color = c.onBg, modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 15.sp, lineHeight = 21.sp,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}

/** Legenda de um gesto (não clicável). O gesto principal (aceitar) vem destacado. */
@Composable
private fun GestureLegend(gesture: String, action: String, c: FlowColors, modifier: Modifier, emphasized: Boolean = false) {
    val bg = if (emphasized) Color.White else Color.White.copy(alpha = 0.10f)
    val fg = if (emphasized) InkOnWhite else c.onBg
    Column(
        modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(gesture, color = fg, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp, lineHeight = 18.sp, fontWeight = FontWeight.Bold))
        Text(action, color = fg, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 17.sp))
    }
}

// ───────────────────────── Documento completo (termos, política) ─────────────────────────

@Composable
private fun DocumentScreen(c: FlowColors, k: Float, doc: LegalDocument, icon: ImageVector, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(c.panel).verticalScroll(rememberScrollState())) {
        // Cabeçalho claro, de ponta a ponta (sem borda azul nas laterais)
        Column(
            Modifier
                .fillMaxWidth()
                .background(c.docCard, RoundedCornerShape(bottomStart = u(45, k), bottomEnd = u(45, k)))
                .statusBarsPadding()
                .padding(bottom = u(32, k)),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(8.dp)) {
                IconButton(onClick = onBack) {
                    Icon(ContatoIcons.ArrowLeft, contentDescription = "Voltar", tint = c.onDocCard)
                }
            }
            Box(Modifier.size(u(56, k)).background(ContatoBlue, CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(u(28, k)))
            }
            Spacer(Modifier.height(u(20, k)))
            Text(
                doc.title,
                color = c.onDocCard, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = fs(30, k), fontWeight = FontWeight.Bold, letterSpacing = TightSpacing,
                ),
                modifier = Modifier.padding(horizontal = 24.dp).semantics { heading() },
            )
            Spacer(Modifier.height(u(16, k)))
            Text(
                doc.intro,
                color = c.onDocCard, textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = fs(15, k), lineHeight = fs(23, k)),
                modifier = Modifier.padding(horizontal = u(32, k)),
            )
        }

        Column(Modifier.padding(horizontal = u(32, k))) {
            doc.sections.forEach { s ->
                Text(
                    s.title, color = c.onPanel,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp),
                    modifier = Modifier.padding(top = 28.dp, bottom = 12.dp).semantics { heading() },
                )
                s.blocks.forEach { b ->
                    when (b) {
                        is DocBlock.Paragraph -> Text(
                            b.text, color = c.onPanel,
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 25.sp),
                            modifier = Modifier.padding(bottom = 14.dp),
                        )
                        is DocBlock.Bullets -> b.items.forEach { item ->
                            Row(Modifier.padding(bottom = 8.dp)) {
                                Text("•", color = c.onPanel, modifier = Modifier.padding(start = 8.dp, end = 10.dp),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
                                Text(item, color = c.onPanel, modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 25.sp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
            PillButton("Voltar", onBack,
                container = Color.White, content = Color(0xFF131924), pressedContainer = PressedBlue, height = 56.dp)
            Spacer(Modifier.height(maxOf(32.dp, WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 16.dp)))
        }
    }
}