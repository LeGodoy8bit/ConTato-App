package contato.app.presentation.common

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import contato.app.presentation.theme.motionReduced
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Expressões do mascote, iguais ao estudo do ConTato:
 * Feliz, Curioso, Concentrado, Animado, Surpreso e Tranquilo.
 */
internal enum class MascotMood { HAPPY, CURIOUS, FOCUSED, EXCITED, SURPRISED, CALM }

/** Desenho da barriga como no estudo do mascote (alguns azuis, alguns cinzas). */
internal val MascotDefaultDots = listOf(true, false, true, false, true, true)

private val Ink = Color(0xFF1B2A4A)
private val Blue = Color(0xFF1F5BD8)
private val DotGray = Color(0xFFB9C3D2)
private val Sparkle = Color(0xFFF5C518)
private val AidBlue = Color(0xFF6E85AA)
private val AidDark = Color(0xFF4D6286)

/**
 * O mascote do ConTato: uma bolinha branca (o toque), com a cela Braille 2x3 na barriga
 * e um aparelho auditivo discreto do lado esquerdo. Desenhado em código para poder animar
 * e ficar nítido em qualquer tamanho.
 *
 * Animações (todas param com "remover animações"):
 *  - sempre: flutua devagar; olhos abertos piscam de vez em quando;
 *  - Animado: pula e acena; Tranquilo: respira devagar; Surpreso: o "!" pulsa;
 *  - [vibrating]: ondas dos dois lados, como no "Em vibração".
 * Só decorativo: o leitor de tela ignora.
 */
@Composable
internal fun Mascot(
    size: Dp,
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.HAPPY,
    vibrating: Boolean = false,
    dots: List<Boolean> = MascotDefaultDots,
) {
    val reduce = motionReduced()
    val t = rememberInfiniteTransition(label = "mascot")
    val bobPeriod = when (mood) { MascotMood.EXCITED -> 520; MascotMood.CALM -> 2600; else -> 1600 }
    val bob = if (reduce) 0f else t.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(bobPeriod, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "bob",
    ).value
    val blink = if (reduce) 1f else t.animateFloat(
        1f, 1f,
        infiniteRepeatable(keyframes {
            durationMillis = 3800
            1f at 0
            1f at 3400
            0.1f at 3500
            1f at 3620
        }),
        label = "blink",
    ).value
    val wave = if (reduce) 0f else t.animateFloat(
        0f, 1f, infiniteRepeatable(tween(1100, easing = LinearEasing)), label = "wave",
    ).value

    Canvas(modifier.size(size).clearAndSetSemantics { }) {
        val w = this.size.width
        val r = w * 0.34f
        val lift = when (mood) {
            MascotMood.EXCITED -> -kotlin.math.abs(bob) * w * 0.05f
            else -> bob * w * 0.018f
        }
        val breathe = if (mood == MascotMood.CALM) 1f + bob * 0.015f else 1f
        val c = Offset(w / 2f, w * 0.48f + lift)

        // Sombra no chão: menor quando ele sobe.
        val shadowW = r * (1.55f + lift / w * 2f)
        drawOval(Color.Black.copy(alpha = 0.10f), topLeft = Offset(w / 2f - shadowW / 2f, w * 0.86f), size = Size(shadowW, w * 0.06f))

        if (vibrating) drawVibration(c, r, wave)

        // Aparelho auditivo (atrás da cabeça, lado esquerdo).
        drawArc(
            AidBlue, startAngle = 148f, sweepAngle = 68f, useCenter = false,
            topLeft = Offset(c.x - r * 1.1f, c.y - r * 1.1f), size = Size(r * 2.2f, r * 2.2f),
            style = Stroke(width = r * 0.2f, cap = StrokeCap.Round),
        )
        drawOval(
            Brush.verticalGradient(listOf(AidBlue, AidDark), startY = c.y - r * 0.25f, endY = c.y + r * 0.35f),
            topLeft = Offset(c.x - r * 1.22f, c.y - r * 0.22f), size = Size(r * 0.32f, r * 0.56f),
        )

        // Bracinhos (o direito acena quando Animado ou Feliz).
        val armWave = when (mood) {
            MascotMood.EXCITED -> sin(wave * 2 * PI).toFloat() * 18f
            MascotMood.HAPPY -> sin(wave * 2 * PI).toFloat() * 6f
            else -> 0f
        }
        drawArm(Offset(c.x - r * 0.92f, c.y + r * 0.42f), r, -20f)
        rotate(armWave, pivot = Offset(c.x + r * 0.85f, c.y + r * 0.2f)) {
            drawArm(Offset(c.x + r * 0.95f, c.y + (if (mood == MascotMood.EXCITED) 0.05f else 0.35f) * r), r, if (mood == MascotMood.EXCITED) 35f else 20f)
        }

        // Corpo: branco com luz de cima à esquerda.
        drawCircle(
            Brush.radialGradient(
                listOf(Color.White, Color(0xFFF1F4F8), Color(0xFFD5DCE6)),
                center = Offset(c.x - r * 0.35f, c.y - r * 0.45f), radius = r * 1.75f,
            ),
            radius = r * breathe, center = c,
        )
        drawOval(Color.White.copy(alpha = 0.7f), topLeft = Offset(c.x - r * 0.6f, c.y - r * 0.78f), size = Size(r * 0.45f, r * 0.22f))

        drawFace(c, r, mood, blink)
        drawCell(c, r, dots)
        drawAccents(c, r, mood, wave, bob)
    }
}

private fun DrawScope.drawArm(at: Offset, r: Float, angle: Float) {
    rotate(angle, pivot = at) {
        drawOval(
            Brush.radialGradient(listOf(Color.White, Color(0xFFDCE2EA)), center = Offset(at.x - r * 0.05f, at.y - r * 0.08f), radius = r * 0.35f),
            topLeft = Offset(at.x - r * 0.2f, at.y - r * 0.15f), size = Size(r * 0.4f, r * 0.32f),
        )
    }
}

private fun DrawScope.drawFace(c: Offset, r: Float, mood: MascotMood, blink: Float) {
    val eyeY = c.y - r * 0.22f
    val dx = r * 0.32f
    val stroke = Stroke(width = r * 0.075f, cap = StrokeCap.Round)
    val eyeW = r * 0.3f

    for (side in listOf(-1f, 1f)) {
        val ex = c.x + side * dx
        when (mood) {
            // ^ ^ (olhos sorrindo)
            MascotMood.HAPPY, MascotMood.EXCITED -> drawArc(
                Ink, 200f, 140f, false, topLeft = Offset(ex - eyeW / 2f, eyeY - eyeW * 0.25f), size = Size(eyeW, eyeW * 0.7f), style = stroke,
            )
            // ‿ ‿ (olhos fechados)
            MascotMood.FOCUSED, MascotMood.CALM -> drawArc(
                Ink, 20f, 140f, false, topLeft = Offset(ex - eyeW / 2f, eyeY - eyeW * 0.45f), size = Size(eyeW, eyeW * 0.6f), style = stroke,
            )
            // olhos abertos (piscam)
            MascotMood.CURIOUS, MascotMood.SURPRISED -> {
                val big = if (mood == MascotMood.CURIOUS && side > 0) 1.25f else 1f
                val ew = r * 0.13f * big
                val eh = r * 0.18f * big * blink
                drawOval(Ink, topLeft = Offset(ex - ew / 2f, eyeY - eh / 2f), size = Size(ew, eh))
                drawCircle(Color.White, radius = ew * 0.18f, center = Offset(ex + ew * 0.15f, eyeY - eh * 0.2f))
            }
        }
    }

    val my = c.y + r * 0.02f
    when (mood) {
        MascotMood.HAPPY, MascotMood.CALM -> drawArc(
            Ink, 20f, 140f, false, topLeft = Offset(c.x - r * 0.13f, my - r * 0.1f), size = Size(r * 0.26f, r * 0.17f), style = stroke,
        )
        MascotMood.EXCITED -> drawArc(
            Ink, 0f, 180f, true, topLeft = Offset(c.x - r * 0.13f, my - r * 0.08f), size = Size(r * 0.26f, r * 0.2f),
        )
        MascotMood.FOCUSED -> drawLine(Ink, Offset(c.x - r * 0.06f, my), Offset(c.x + r * 0.06f, my), strokeWidth = r * 0.05f, cap = StrokeCap.Round)
        MascotMood.CURIOUS -> drawCircle(Ink, radius = r * 0.04f, center = Offset(c.x + r * 0.03f, my))
        MascotMood.SURPRISED -> drawOval(Ink, topLeft = Offset(c.x - r * 0.05f, my - r * 0.05f), size = Size(r * 0.1f, r * 0.12f))
    }
}

/** Cela Braille na barriga (deitada: 3 em cima, 3 embaixo, como no estudo): azuis marcados, cinzas lisos. */
private fun DrawScope.drawCell(c: Offset, r: Float, dots: List<Boolean>) {
    val dotR = r * 0.1f
    val gapX = r * 0.3f
    val gapY = r * 0.26f
    val midY = c.y + r * 0.42f
    for (i in 0 until 6) {
        val col = i % 3
        val row = i / 3
        val center = Offset(c.x + (col - 1) * gapX, midY + (row - 0.5f) * gapY)
        val on = dots.getOrElse(i) { false }
        drawCircle(
            if (on) Brush.radialGradient(listOf(Color(0xFF4A82F0), Blue), center = Offset(center.x - dotR * 0.3f, center.y - dotR * 0.3f), radius = dotR * 1.4f)
            else Brush.radialGradient(listOf(Color(0xFFD3DAE4), DotGray), center = Offset(center.x - dotR * 0.3f, center.y - dotR * 0.3f), radius = dotR * 1.4f),
            radius = dotR, center = center,
        )
    }
}

private fun DrawScope.drawAccents(c: Offset, r: Float, mood: MascotMood, wave: Float, bob: Float) {
    when (mood) {
        MascotMood.HAPPY, MascotMood.EXCITED -> {
            // Tracinhos amarelos de alegria, dos dois lados da cabeça.
            val pulse = if (mood == MascotMood.EXCITED) 0.85f + 0.15f * sin(wave * 2 * PI).toFloat() else 1f
            for (side in listOf(-1f, 1f)) {
                for ((k, ang) in listOf(-60f, -40f, -20f).withIndex()) {
                    val a = Math.toRadians((if (side < 0) 180f - ang else ang).toDouble())
                    val start = Offset(c.x + (cos(a) * r * 1.12f).toFloat(), c.y - r * 0.55f + (sin(a) * r * 0.5f).toFloat() - k * r * 0.02f)
                    val end = Offset(start.x + (cos(a) * r * 0.16f * pulse).toFloat(), start.y + (sin(a) * r * 0.16f * pulse).toFloat())
                    drawLine(Sparkle, start, end, strokeWidth = r * 0.07f, cap = StrokeCap.Round)
                }
            }
            if (mood == MascotMood.EXCITED) drawMotionLines(c, r)
        }
        MascotMood.CURIOUS -> drawQuestion(Offset(c.x + r * 0.85f, c.y - r * 1.05f + bob * r * 0.03f), r * 0.3f)
        MascotMood.SURPRISED -> {
            val s = 1f + 0.12f * sin(wave * 2 * PI).toFloat()
            val x = c.x + r * 0.2f
            val y = c.y - r * 1.25f
            drawLine(Ink, Offset(x, y - r * 0.18f * s), Offset(x, y + r * 0.06f * s), strokeWidth = r * 0.09f, cap = StrokeCap.Round)
            drawCircle(Ink, radius = r * 0.055f, center = Offset(x, y + r * 0.2f * s))
        }
        else -> Unit
    }
}

/** Arquinhos ( ) ao lado, mostrando movimento. */
private fun DrawScope.drawMotionLines(c: Offset, r: Float) {
    val stroke = Stroke(width = r * 0.05f, cap = StrokeCap.Round)
    drawArc(Ink.copy(alpha = 0.7f), 150f, 60f, false, topLeft = Offset(c.x - r * 1.45f, c.y - r * 0.8f), size = Size(r * 0.6f, r * 0.9f), style = stroke)
    drawArc(Ink.copy(alpha = 0.7f), -30f, 60f, false, topLeft = Offset(c.x + r * 0.85f, c.y + r * 0.1f), size = Size(r * 0.6f, r * 0.9f), style = stroke)
}

/** Ondas de vibração saindo dos dois lados, crescendo e sumindo. */
private fun DrawScope.drawVibration(c: Offset, r: Float, wave: Float) {
    for (i in 0..1) {
        val p = (wave + i * 0.5f) % 1f
        val alpha = (1f - p) * 0.8f
        val grow = r * (1.25f + p * 0.35f)
        val stroke = Stroke(width = r * 0.06f, cap = StrokeCap.Round)
        drawArc(Blue.copy(alpha = alpha), 150f, 60f, false,
            topLeft = Offset(c.x - grow, c.y - grow), size = Size(grow * 2f, grow * 2f), style = stroke)
        drawArc(Blue.copy(alpha = alpha), -30f, 60f, false,
            topLeft = Offset(c.x - grow, c.y - grow), size = Size(grow * 2f, grow * 2f), style = stroke)
    }
}

/** "?" desenhado com traço (sem fonte, para ficar igual em todo celular). */
private fun DrawScope.drawQuestion(at: Offset, s: Float) {
    val path = Path().apply {
        moveTo(at.x - s * 0.35f, at.y - s * 0.35f)
        cubicTo(at.x - s * 0.35f, at.y - s * 0.85f, at.x + s * 0.45f, at.y - s * 0.85f, at.x + s * 0.4f, at.y - s * 0.3f)
        cubicTo(at.x + s * 0.36f, at.y, at.x, at.y, at.x, at.y + s * 0.3f)
    }
    drawPath(path, Ink, style = Stroke(width = s * 0.22f, cap = StrokeCap.Round))
    drawCircle(Ink, radius = s * 0.13f, center = Offset(at.x, at.y + s * 0.72f))
}
