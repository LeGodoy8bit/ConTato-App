package contato.app.presentation.onboarding

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.domain.service.BrailleMapper
import contato.app.presentation.theme.ContatoBlue
import contato.app.presentation.theme.ContatoNavy
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val LetterColor = Color(0xFFCADFFD)

// Sinal de número do Braille: pontos 3, 4, 5 e 6. Depois dele, as letras A-J viram 1-9 e 0.
private val NUMBER_SIGN = listOf(false, false, true, true, true, true)
private val ALL_OFF = List(6) { false }
private val POOL: List<Char> = ('A'..'Z').toList()
// ── Tempos da animação (ms). Aumente para ficar mais lento. ──
private const val HOLD_LETTER_MS = 2600L     // quanto tempo a letra fica na tela
private const val ERASE_STAGGER_MS = 70L     // intervalo entre cada ponto que apaga
private const val PAUSE_EMPTY_MS = 450L      // a cela fica vazia antes da próxima letra
private const val LIGHT_STAGGER_MS = 140L    // intervalo entre cada ponto que acende
private const val HOLD_NUMBER_SIGN_MS = 1000L

// ── Medidas do Figma (frame 428 x 926) ──
private const val LETTER_FONT = 62f  // altura da letra "A" no Figma = 45 px

private fun patternOf(c: Char): List<Boolean> = BrailleMapper.brailleTable[c] ?: ALL_OFF

private fun digitToLetter(d: Char): Char = if (d == '0') 'J' else 'A' + (d - '1')

@Composable
private fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}

/**
 * Célula Braille centralizada + letra à direita (posições do Figma, escala k = largura/428).
 *
 * A cada ciclo: a letra é APAGADA (os pontos apagam um a um), a cela fica vazia um instante,
 * e a próxima letra é ESCRITA (os pontos acendem um a um). Números mostram primeiro o sinal
 * de número (#), apagam, e depois o dígito. Os pontos são os reais, do BrailleMapper.
 *
 * @param frozen true = estado "pressionado" do Figma: cela parada, sem letra (só o ponto 1).
 */
@Composable
internal fun BrailleCellHero(k: Float, darkTheme: Boolean, frozen: Boolean, modifier: Modifier = Modifier) {
    val reduceMotion = rememberReduceMotion()
    val inactive = Color.White
    val active = if (darkTheme) ContatoBlue else ContatoNavy

    val progress = remember { List(6) { Animatable(if (it == 0) 1f else 0f) } }
    var label by remember { mutableStateOf("A") }

    LaunchedEffect(frozen, reduceMotion) {
        suspend fun animateTo(pattern: List<Boolean>, staggerMs: Long) = coroutineScope {
            var order = 0
            pattern.forEachIndexed { idx, on ->
                val target = if (on) 1f else 0f
                if (progress[idx].value != target) {
                    val wait = order++ * staggerMs
                    launch { delay(wait); progress[idx].animateTo(target, tween(160)) }
                }
            }
        }

        // Estado inicial/parado: letra A (ponto 1).
        patternOf('A').forEachIndexed { i, on -> progress[i].snapTo(if (on) 1f else 0f) }
        if (frozen) { label = ""; return@LaunchedEffect }
        label = "A"
        if (reduceMotion) return@LaunchedEffect

        var last = 'A'
        while (true) {
            delay(HOLD_LETTER_MS)
            var next: Char
            do { next = POOL.random() } while (next == last)
            last = next

            val frames = if (next.isDigit()) {
                listOf("#" to NUMBER_SIGN, next.toString() to patternOf(digitToLetter(next)))
            } else {
                listOf(next.toString() to patternOf(next))
            }

            // apaga a letra atual
            label = ""
            animateTo(ALL_OFF, ERASE_STAGGER_MS)
            delay(PAUSE_EMPTY_MS)

            frames.forEachIndexed { i, (text, pattern) ->
                label = text
                animateTo(pattern, LIGHT_STAGGER_MS)
                if (i < frames.lastIndex) {
                    delay(HOLD_NUMBER_SIGN_MS)
                    label = ""
                    animateTo(ALL_OFF, ERASE_STAGGER_MS)
                    delay(PAUSE_EMPTY_MS)
                }
            }
        }
    }

    val cursorAlpha = if (reduceMotion) {
        1f
    } else {
        rememberInfiniteTransition(label = "cursor").animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "cursorAlpha",
        ).value
    }

    Box(
        modifier
            .width((428 * k).dp)
            .height((132 * k).dp)
            .clearAndSetSemantics { }, // decorativa para o leitor de tela
    ) {
        // Cela: 6 pontos de 38, colunas em x=172 e 218, linhas em y=0, 47 e 94 (no frame: 106, 153, 200)
        Canvas(
            Modifier
                .offset(x = (172 * k).dp)
                .size(width = (84 * k).dp, height = (132 * k).dp),
        ) {
            val r = 19 * k * density
            for (i in 0..5) {
                val col = if (i < 3) 0 else 1
                val row = i % 3
                drawCircle(
                    color = lerp(inactive, active, progress[i].value),
                    radius = r,
                    center = Offset(r + col * 46 * k * density, r + row * 47 * k * density),
                )
            }
        }
        // Letra (x=279) e cursor, logo à direita da cela
        Row(
            Modifier
                .offset(x = (279 * k).dp)
                .fillMaxHeight(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = LetterColor,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = (LETTER_FONT * k).sp,
                    lineHeight = (LETTER_FONT * 1.2f * k).sp,
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.width((11 * k).dp))
            if (!frozen) {
                Box(
                    Modifier
                        .width((1.5f * k).dp)
                        .height((64 * k).dp)
                        .alpha(cursorAlpha)
                        .background(LetterColor),
                )
            }
        }
    }
}