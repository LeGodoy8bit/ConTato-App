package contato.app.presentation.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import contato.app.R
import contato.app.domain.model.SplashMode
import contato.app.domain.model.SystemFeedbackEvent
import contato.app.presentation.theme.ContatoBlue
import contato.app.presentation.theme.ContatoBlueOnDark
import contato.app.presentation.theme.ContatoDarkSurface
import contato.app.presentation.theme.ContatoNavy
import contato.app.presentation.theme.SplashTitleStyle
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// MEDIDAS DO FIGMA (frame iPhone 14 Plus, 428 x 926). Tudo em "unidades de design".
// A tela escala tudo por igual (largura da tela / 428), então fica igual ao Figma.
// Se algo ficar 1 ou 2 px fora, ajuste aqui, em um único lugar.
// ─────────────────────────────────────────────────────────────────────────────
internal object SplashSpecs {
    const val DESIGN_W = 428f
    const val DESIGN_H = 926f

    // Folga entre a última letra e a bolinha-cursor (frames 1 e 2: ~13,4)
    const val CURSOR_GAP = 13.5f

    // Logo 130x40 (frames 3, 4 e 5)
    const val LOGO_X = 149f
    const val LOGO_Y = 821f
    const val LOGO_W = 130f
    const val LOGO_H = 40f

    // Círculo do frame 5
    const val CIRCLE_CX = 214f
    const val CIRCLE_CY = 463f
    const val CIRCLE_R = 155f

    /** Uma linha de texto digitada. left = X do primeiro glifo; baseline = linha de base. */
    class Line(
        val text: String,
        val left: Float,
        val baseline: Float,
        val textAccent: Boolean,   // true = texto azul, false = tinta (navy)
        val cursorY: Float,
        val cursorR: Float,
        val cursorAccent: Boolean,
    )

    // Frame 1: "Conectando" navy, cursor navy
    // Frame 2: "pelo toque" azul, cursor azul
    // Frame 3: "Tecnologia" navy, cursor azul
    val LINE_1 = Line("Conectando", 102.7f, 463.6f, false, 453.34f, 20.34f, false)
    val LINE_2 = Line("pelo toque", 105.0f, 515.0f, true, 503.0f, 20.0f, true)
    val LINE_3 = Line("Tecnologia", 96.3f, 415.0f, false, 407.5f, 19.5f, true)

    /** Posição/tamanho de uma bolinha do símbolo. */
    class DotKey(val x: Float, val y: Float, val rx: Float, val ry: Float)

    // Frame 2 (2 bolinhas navy), frame 3 (3 bolinhas), frame 4 (símbolo final, 4 bolinhas)
    val D1_P2 = DotKey(190f, 463f, 20f, 20f)
    val D2_P2 = DotKey(237.2f, 463f, 20f, 20f)

    val D1_P3 = DotKey(190f, 454f, 20f, 20f)
    val D2_P3 = DotKey(237.2f, 454f, 20f, 20f)
    val D3_P3 = DotKey(194f, 513f, 20f, 20f) // Figma: 507; +6 para afastar das bolinhas de cima

    val D1_P4 = DotKey(188f, 462.954f, 22f, 21.651f)
    val D2_P4 = DotKey(239f, 462.954f, 22f, 21.651f)
    val D3_P4 = DotKey(188f, 516.257f, 22f, 21.651f)
    val D4_P4 = DotKey(239f, 409.651f, 22f, 21.651f)

    // Tempos (ms)
    const val TYPE_MS = 28L
    const val ERASE_MS = 14L
    const val HOLD_MS = 220L
    const val FLY_MS = 320
}

/**
 * Cores da splash, derivadas das cores do tema (Color.kt).
 *
 * Tema escuro: o fundo da splash é um tom PROVISÓRIO mais escuro que o
 * ContatoDarkSurface, de propósito. O círculo final usa ContatoDarkSurface
 * (o fundo das telas seguintes); se o fundo da splash fosse igual, o círculo
 * ficaria invisível enquanto cresce. As bolinhas "navy" viram brancas (como o
 * "Con" do logo escuro) e as azuis seguem #005DFF (símbolo/logotipo é isento
 * de contraste na WCAG, e mesmo assim dá ~3,6:1 sobre esse fundo). O texto
 * azul usa ContatoBlueOnDark, porque texto precisa de 4,5:1.
 */
internal class SplashPalette(
    val background: Color,
    val ink: Color,        // bolinhas e texto "navy" (branco no escuro)
    val accentDot: Color,  // bolinhas azuis e cursor azul
    val accentText: Color, // texto azul ("pelo toque")
    val circle: Color,
)

internal val LightPalette = SplashPalette(
    background = Color.White,
    ink = ContatoNavy,
    accentDot = ContatoBlue,
    accentText = ContatoBlue,
    circle = ContatoBlue,
)

internal val DarkPalette = SplashPalette(
    background = Color(0xFF0B111B), // PROVISÓRIO, só da splash
    ink = Color.White,
    accentDot = ContatoBlue,
    accentText = ContatoBlueOnDark,
    circle = ContatoDarkSurface,
)

internal class DotAnim(val accent: Boolean) {
    val x = Animatable(0f)
    val y = Animatable(0f)
    val rx = Animatable(0f)
    val ry = Animatable(0f)
    val alpha = Animatable(0f)

    suspend fun place(k: SplashSpecs.DotKey, visible: Boolean = true) {
        x.snapTo(k.x); y.snapTo(k.y); rx.snapTo(k.rx); ry.snapTo(k.ry)
        alpha.snapTo(if (visible) 1f else 0f)
    }

    /** Nasce do nada (tamanho 0) e "pula" até o tamanho final, com um pequeno quique. */
    suspend fun pop(k: SplashSpecs.DotKey) = coroutineScope {
        x.snapTo(k.x); y.snapTo(k.y); rx.snapTo(0f); ry.snapTo(0f); alpha.snapTo(1f)
        val bounce = spring<Float>(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium)
        launch { rx.animateTo(k.rx, bounce) }
        launch { ry.animateTo(k.ry, bounce) }
    }

    /** Um "respirar" rápido: cresce um pouco e volta. */
    suspend fun pulse(k: SplashSpecs.DotKey, scale: Float) = coroutineScope {
        launch { rx.animateTo(k.rx * scale, tween(110, easing = FastOutSlowInEasing)); rx.animateTo(k.rx, tween(180)) }
        launch { ry.animateTo(k.ry * scale, tween(110, easing = FastOutSlowInEasing)); ry.animateTo(k.ry, tween(180)) }
    }

    suspend fun move(k: SplashSpecs.DotKey, ms: Int) = coroutineScope {
        val spec = tween<Float>(ms, easing = FastOutSlowInEasing)
        launch { x.animateTo(k.x, spec) }
        launch { y.animateTo(k.y, spec) }
        launch { rx.animateTo(k.rx, spec) }
        launch { ry.animateTo(k.ry, spec) }
    }
}

private class CursorAnim {
    val x = Animatable(0f)
    val y = Animatable(0f)
    val r = Animatable(0f)
    val alpha = Animatable(0f)
    var accent by mutableStateOf(false)
}

/**
 * Splash do ConTato.
 *
 * @param mode FULL (animação completa, primeira abertura) ou SHORT (só símbolo + círculo).
 * @param onFeedback recebe SPLASH_TYPING_TICK a cada letra e SPLASH_SYMBOL_FORMED no final.
 * @param onFinished chamado quando o círculo já cobriu a tela; aqui você marca a splash
 *        como vista e navega para a seleção de modo.
 */
@Composable
fun SplashScreen(
    mode: SplashMode,
    onFeedback: (SystemFeedbackEvent) -> Unit,
    onFinished: () -> Unit,
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val measurer = rememberTextMeasurer()
    val designDensity = remember { Density(density = 1f, fontScale = 1f) }
    val titleStyle = SplashTitleStyle // já está em 32sp no Type.kt

    val cursor = remember { CursorAnim() }
    val dots = remember { listOf(DotAnim(false), DotAnim(false), DotAnim(true), DotAnim(true)) }
    val logoAlpha = remember { Animatable(0f) }
    val logoSlide = remember { Animatable(0f) } // deslocamento vertical em unidades de design
    val circleR = remember { Animatable(0f) }
    var typed by remember { mutableStateOf("") }
    var line by remember { mutableStateOf(SplashSpecs.LINE_1) }

    val currentFeedback by rememberUpdatedState(onFeedback)
    val currentFinished by rememberUpdatedState(onFinished)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        // Escala pela largura; o conteúdo fica centralizado na vertical.
        val k = widthPx / SplashSpecs.DESIGN_W
        val offsetY = (heightPx - SplashSpecs.DESIGN_H * k) / 2f
        val fullRadius = hypot(widthPx, heightPx) / k  // em unidades de design

        LaunchedEffect(mode) {
            fun widthOf(text: String): Float =
                if (text.isEmpty()) 0f
                else measurer.measure(AnnotatedString(text), titleStyle, density = designDensity)
                    .size.width.toFloat()

            suspend fun placeCursor(l: SplashSpecs.Line, text: String, animate: Boolean) {
                val cx = l.left + widthOf(text) + SplashSpecs.CURSOR_GAP + l.cursorR
                cursor.r.snapTo(l.cursorR)
                cursor.y.snapTo(l.cursorY)
                if (animate) cursor.x.animateTo(cx, tween(30)) else cursor.x.snapTo(cx)
            }

            suspend fun typeLine(l: SplashSpecs.Line) {
                line = l
                typed = ""
                cursor.accent = l.cursorAccent
                placeCursor(l, "", animate = false)
                cursor.alpha.snapTo(1f)
                delay(120)
                for (i in 1..l.text.length) {
                    typed = l.text.take(i)
                    currentFeedback(SystemFeedbackEvent.SPLASH_TYPING_TICK)
                    placeCursor(l, typed, animate = true)
                    delay(SplashSpecs.TYPE_MS)
                }
                delay(SplashSpecs.HOLD_MS)
                for (i in l.text.length - 1 downTo 0) {
                    typed = l.text.take(i)
                    currentFeedback(SystemFeedbackEvent.SPLASH_TYPING_TICK)
                    placeCursor(l, typed, animate = true)
                    delay(SplashSpecs.ERASE_MS)
                }
            }

            /** A bolinha-cursor voa até a posição da bolinha do símbolo e vira ela. */
            suspend fun cursorBecomes(dot: DotAnim, target: SplashSpecs.DotKey) = coroutineScope {
                val spec = tween<Float>(SplashSpecs.FLY_MS, easing = FastOutSlowInEasing)
                launch { cursor.x.animateTo(target.x, spec) }
                launch { cursor.y.animateTo(target.y, spec) }
                launch { cursor.r.animateTo(target.rx, spec) }
            }.also {
                dot.place(target)
                cursor.alpha.snapTo(0f)
            }

            suspend fun finishWithCircle() {
                delay(300)
                currentFeedback(SystemFeedbackEvent.SPLASH_SYMBOL_FORMED)
                circleR.animateTo(SplashSpecs.CIRCLE_R, tween(350, easing = FastOutSlowInEasing))
                circleR.animateTo(fullRadius, tween(550, easing = FastOutSlowInEasing))
                delay(100)
                currentFinished()
            }

            if (mode == SplashMode.SHORT) {
                // Splash curta: as 4 bolinhas "pulam" uma a uma, na mesma ordem em que o
                // símbolo se forma na splash completa, o símbolo "respira" e o logo sobe
                // suavemente. Termina com o pulso forte e o círculo.
                val keys = listOf(
                    SplashSpecs.D1_P4, SplashSpecs.D2_P4, SplashSpecs.D3_P4, SplashSpecs.D4_P4,
                )
                delay(150)
                coroutineScope {
                    keys.forEachIndexed { i, key ->
                        launch {
                            delay(i * 110L)
                            currentFeedback(SystemFeedbackEvent.SPLASH_TYPING_TICK)
                            dots[i].pop(key)
                        }
                    }
                    launch {
                        delay(330L)
                        logoSlide.snapTo(14f)
                        launch { logoAlpha.animateTo(1f, tween(380)) }
                        logoSlide.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
                    }
                }
                delay(120)
                coroutineScope {
                    keys.forEachIndexed { i, key -> launch { dots[i].pulse(key, 1.08f) } }
                }
                delay(120)
                finishWithCircle()
                return@LaunchedEffect
            }

            // Fase 1 (frame 1): "Conectando"; o cursor navy vira as 2 bolinhas do meio
            typeLine(SplashSpecs.LINE_1)
            cursorBecomes(dots[0], SplashSpecs.D1_P2)
            dots[1].place(SplashSpecs.D1_P2)
            dots[1].move(SplashSpecs.D2_P2, 280)

            // Fase 2 (frame 2): "pelo toque"; o cursor azul vira a 3ª bolinha
            typeLine(SplashSpecs.LINE_2)
            coroutineScope {
                launch { logoAlpha.animateTo(1f, tween(400)) }
                launch { dots[0].move(SplashSpecs.D1_P3, 320) }
                launch { dots[1].move(SplashSpecs.D2_P3, 320) }
                launch { cursorBecomes(dots[2], SplashSpecs.D3_P3) }
            }

            // Fase 3 (frame 3): "Tecnologia"; o cursor azul vira a 4ª bolinha e o símbolo se completa
            typeLine(SplashSpecs.LINE_3)
            coroutineScope {
                launch { dots[0].move(SplashSpecs.D1_P4, 400) }
                launch { dots[1].move(SplashSpecs.D2_P4, 400) }
                launch { dots[2].move(SplashSpecs.D3_P4, 400) }
                launch { cursorBecomes(dots[3], SplashSpecs.D4_P4) }
            }

            // Fase 4 (frame 5): pulso forte + círculo que preenche a tela
            finishWithCircle()
        }

        val logoRes = if (darkTheme) R.drawable.logo_contato_dark else R.drawable.logo_contato_light
        val density = LocalDensity.current

        // Símbolo, texto e cursor
        Canvas(Modifier.fillMaxSize()) {
            withTransform({
                translate(0f, offsetY)
                scale(k, k, Offset.Zero)
            }) {
                val l = line
                if (typed.isNotEmpty()) {
                    val layout = measurer.measure(
                        AnnotatedString(typed),
                        titleStyle.copy(color = if (l.textAccent) palette.accentText else palette.ink),
                        density = designDensity,
                    )
                    drawText(layout, topLeft = Offset(l.left, l.baseline - layout.firstBaseline))
                }
                dots.forEach { d ->
                    if (d.alpha.value > 0f) {
                        val c = (if (d.accent) palette.accentDot else palette.ink).copy(alpha = d.alpha.value)
                        drawOval(
                            c,
                            topLeft = Offset(d.x.value - d.rx.value, d.y.value - d.ry.value),
                            size = Size(d.rx.value * 2f, d.ry.value * 2f),
                        )
                    }
                }
                if (cursor.alpha.value > 0f) {
                    val c = if (cursor.accent) palette.accentDot else palette.ink
                    drawCircle(c, cursor.r.value, Offset(cursor.x.value, cursor.y.value))
                }
            }
        }

        // Logo 130x40 (aparece a partir da fase 3 do Figma)
        Image(
            painter = painterResource(logoRes),
            contentDescription = null,
            modifier = Modifier
                .offset {
                    IntOffset(
                        (SplashSpecs.LOGO_X * k).roundToInt(),
                        (offsetY + (SplashSpecs.LOGO_Y + logoSlide.value) * k).roundToInt(),
                    )
                }
                .size(
                    width = with(density) { (SplashSpecs.LOGO_W * k).toDp() },
                    height = with(density) { (SplashSpecs.LOGO_H * k).toDp() },
                )
                .alpha(logoAlpha.value),
        )

        // Círculo do frame 5, que depois cresce até cobrir a tela
        Canvas(Modifier.fillMaxSize()) {
            if (circleR.value > 0f) {
                withTransform({
                    translate(0f, offsetY)
                    scale(k, k, Offset.Zero)
                }) {
                    drawCircle(
                        palette.circle,
                        circleR.value,
                        Offset(SplashSpecs.CIRCLE_CX, SplashSpecs.CIRCLE_CY),
                    )
                }
            }
        }
    }
}