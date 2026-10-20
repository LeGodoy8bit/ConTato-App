package contato.app.presentation.onboarding

import androidx.compose.animation.core.Animatable
import android.provider.Settings
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
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
import contato.app.presentation.theme.SplashTitleStyle
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
// VERSÃO "BRAILLE" DA SPLASH
//
// Ideia: o símbolo do ConTato são 4 pontos de uma cela Braille (pontos 2, 3, 4 e 5),
// que é a letra "t" de "toque". A splash mostra uma cela vazia (6 pontos fantasma)
// e, a cada palavra que termina de ser digitada, pontos "levantam" na cela:
//   "Conectando"           -> os 2 pontos do meio (2 e 5)
//   "pelo toque"           -> o ponto de baixo à esquerda (3)
//   "sem barreiras"        -> o ponto de cima à direita (4) e o símbolo fecha
// Uma bolinha (cursor) acompanha o fim do texto: fica acesa enquanto digita e
// pisca enquanto a palavra está parada.
// No final os pontos fantasma somem, o símbolo "respira" e o círculo preenche a tela.
//
// Numeração dos pontos:   1 4
//                         2 5
//                         3 6
// ─────────────────────────────────────────────────────────────────────────────

private object Cell {
    // Mesmas posições do símbolo final do Figma (frame 4)
    const val COL_L = 188f
    const val COL_R = 239f
    const val ROW_T = 409.651f
    const val ROW_M = 462.954f
    const val ROW_B = 516.257f
    const val R = 21.8f

    // Texto fica abaixo da cela, centralizado
    const val CENTER_X = 214f
    const val TEXT_BASELINE = 620f

    // Cursor: bolinha logo depois da última letra, na altura do meio das minúsculas.
    const val CURSOR_R = 6.5f
    const val CURSOR_GAP = 9f
    const val CURSOR_LIFT = 11f

    fun x(dot: Int) = if (dot <= 3) COL_L else COL_R
    fun y(dot: Int) = when (dot) {
        1, 4 -> ROW_T
        2, 5 -> ROW_M
        else -> ROW_B
    }
}

private val FINAL_ORDER = intArrayOf(2, 5, 3, 4) // ordem em que o símbolo se forma

@Composable
fun SplashScreenBraille(
    mode: SplashMode,
    onFeedback: (SystemFeedbackEvent) -> Unit,
    onFinished: () -> Unit,
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val measurer = rememberTextMeasurer()
    val designDensity = remember { Density(density = 1f, fontScale = 1f) }
    val titleStyle = SplashTitleStyle

    val lit = remember { List(6) { Animatable(0f) } }   // 0 = fantasma, 1 = ponto levantado
    val ghost = remember { Animatable(0f) }             // alpha da cela vazia
    val symbolScale = remember { Animatable(1f) }
    val logoAlpha = remember { Animatable(0f) }
    val logoSlide = remember { Animatable(0f) }
    val circleR = remember { Animatable(0f) }

    var typed by remember { mutableStateOf("") }
    var lineAccent by remember { mutableStateOf(false) } // cor do texto da linha atual
    var lineLeft by remember { mutableStateOf(0f) }
    var cursorOn by remember { mutableStateOf(false) } // cursor visível
    var typing by remember { mutableStateOf(false) }   // digitando/apagando: cursor fixo, sem piscar

    // Piscar do cursor (parado se "reduzir animações" estiver ativo).
    val context = LocalContext.current
    val reduceMotion = remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val blink by rememberInfiniteTransition(label = "cursor").animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(530), RepeatMode.Reverse),
        label = "blink",
    )
    val cursorAlpha = if (typing || reduceMotion) 1f else blink

    val currentFeedback by rememberUpdatedState(onFeedback)
    val currentFinished by rememberUpdatedState(onFinished)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.background),
    ) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()
        val k = widthPx / SplashSpecs.DESIGN_W
        val offsetY = (heightPx - SplashSpecs.DESIGN_H * k) / 2f
        val fullRadius = hypot(widthPx, heightPx) / k

        LaunchedEffect(mode) {
            val scope = this

            fun widthOf(text: String): Float =
                measurer.measure(AnnotatedString(text), titleStyle, density = designDensity)
                    .size.width.toFloat()

            /** Os pontos "pulam" na cela (crescem do zero, com um pequeno quique). */
            fun popDots(dots: IntArray, staggerMs: Long) {
                dots.forEachIndexed { i, d ->
                    scope.launch {
                        delay(i * staggerMs)
                        currentFeedback(SystemFeedbackEvent.SPLASH_TYPING_TICK)
                        lit[d - 1].animateTo(
                            1f,
                            spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                        )
                    }
                }
            }

            /** Digita a palavra; ao terminar, levanta os pontos da fase; depois apaga a palavra. */
            suspend fun typeLine(text: String, accent: Boolean, dots: IntArray) {
                lineAccent = accent
                typed = ""
                lineLeft = Cell.CENTER_X - widthOf(text) / 2f
                cursorOn = true
                delay(120)
                typing = true
                for (i in 1..text.length) {
                    typed = text.take(i)
                    currentFeedback(SystemFeedbackEvent.SPLASH_TYPING_TICK)
                    delay(SplashSpecs.TYPE_MS)
                }
                typing = false
                popDots(dots, staggerMs = 90L)
                delay(SplashSpecs.HOLD_MS + 120L)
                typing = true
                for (i in text.length - 1 downTo 0) {
                    typed = text.take(i)
                    currentFeedback(SystemFeedbackEvent.SPLASH_TYPING_TICK)
                    delay(SplashSpecs.ERASE_MS)
                }
                typing = false
                delay(120)
            }

            /** Os pontos fantasma somem e o símbolo "respira". */
            suspend fun settleSymbol() {
                cursorOn = false
                coroutineScope {
                    launch { ghost.animateTo(0f, tween(450)) }
                    launch {
                        symbolScale.animateTo(1.08f, tween(110, easing = FastOutSlowInEasing))
                        symbolScale.animateTo(1f, tween(180))
                    }
                }
            }

            suspend fun finishWithCircle() {
                delay(300)
                currentFeedback(SystemFeedbackEvent.SPLASH_SYMBOL_FORMED)
                circleR.animateTo(SplashSpecs.CIRCLE_R, tween(350, easing = FastOutSlowInEasing))
                circleR.animateTo(fullRadius, tween(550, easing = FastOutSlowInEasing))
                delay(100)
                currentFinished()
            }

            suspend fun logoIn() {
                logoSlide.snapTo(14f)
                scope.launch { logoAlpha.animateTo(1f, tween(380)) }
                logoSlide.animateTo(0f, tween(420, easing = FastOutSlowInEasing))
            }

            if (mode == SplashMode.SHORT) {
                ghost.animateTo(1f, tween(250))
                scope.launch { delay(200); logoIn() }
                delay(120)
                popDots(FINAL_ORDER, staggerMs = 110L)
                delay(110L * FINAL_ORDER.size + 350L)
                settleSymbol()
                delay(120)
                finishWithCircle()
                return@LaunchedEffect
            }

            ghost.animateTo(1f, tween(350))
            typeLine("Conectando", accent = false, dots = intArrayOf(2, 5))
            scope.launch { logoIn() }
            typeLine("pelo toque", accent = true, dots = intArrayOf(3))
            typeLine("sem barreiras", accent = false, dots = intArrayOf(4))
            settleSymbol()
            finishWithCircle()
        }

        val logoRes = if (darkTheme) R.drawable.logo_contato_dark else R.drawable.logo_contato_light
        val density = LocalDensity.current

        Canvas(Modifier.fillMaxSize()) {
            withTransform({
                translate(0f, offsetY)
                scale(k, k, Offset.Zero)
            }) {
                for (d in 1..6) {
                    val cx = Cell.x(d)
                    val cy = Cell.y(d)
                    // cela fantasma
                    if (ghost.value > 0f) {
                        drawCircle(
                            palette.ink.copy(alpha = 0.14f * ghost.value),
                            Cell.R,
                            Offset(cx, cy),
                        )
                    }
                    // ponto levantado (cores do símbolo: 3 e 4 azuis, 2 e 5 tinta)
                    val l = lit[d - 1].value
                    if (l > 0.01f) {
                        val color = when (d) {
                            3, 4 -> palette.accentDot
                            else -> palette.ink
                        }
                        drawCircle(
                            color,
                            Cell.R * l.coerceAtLeast(0f) * symbolScale.value,
                            Offset(cx, cy),
                        )
                    }
                }

                val textColor = if (lineAccent) palette.accentText else palette.ink
                var textWidth = 0f
                if (typed.isNotEmpty()) {
                    val layout = measurer.measure(
                        AnnotatedString(typed),
                        titleStyle.copy(color = textColor),
                        density = designDensity,
                    )
                    drawText(
                        layout,
                        topLeft = Offset(lineLeft, Cell.TEXT_BASELINE - layout.firstBaseline),
                    )
                    textWidth = layout.size.width.toFloat()
                }

                // Cursor em forma de bolinha, logo depois da última letra.
                if (cursorOn) {
                    drawCircle(
                        textColor.copy(alpha = textColor.alpha * cursorAlpha),
                        Cell.CURSOR_R,
                        Offset(
                            lineLeft + textWidth + Cell.CURSOR_GAP + Cell.CURSOR_R,
                            Cell.TEXT_BASELINE - Cell.CURSOR_LIFT,
                        ),
                    )
                }
            }
        }

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