package contato.app.presentation.common

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import contato.app.presentation.theme.motionReduced

internal enum class MascotMood { HAPPY, THINKING, CELEBRATING }

/**
 * "Pontinho", o mascote do ConTato: um ponto Braille com rosto.
 * Flutua devagar e pisca de vez em quando. Com "remover animações" ligado, fica parado.
 * Só decorativo: o leitor de tela ignora.
 */
@Composable
internal fun Mascot(
    size: Dp,
    body: Color,
    modifier: Modifier = Modifier,
    mood: MascotMood = MascotMood.HAPPY,
) {
    val reduce = motionReduced()
    val transition = rememberInfiniteTransition(label = "mascot")
    val bob = if (reduce) 0f else transition.animateFloat(
        initialValue = -1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Reverse), label = "bob",
    ).value
    val blink = if (reduce) 1f else transition.animateFloat(
        initialValue = 1f, targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 3600
            1f at 0
            1f at 3300
            0.1f at 3400
            1f at 3500
        }),
        label = "blink",
    ).value

    Canvas(modifier.size(size).clearAndSetSemantics { }) {
        val w = this.size.width
        val r = w * 0.36f
        val cx = w / 2f
        val cy = w * 0.46f + bob * w * 0.025f

        // Sombra no chão (diminui quando ele sobe).
        val shadowW = r * (1.5f - bob * 0.08f)
        drawOval(
            Color.Black.copy(alpha = 0.10f),
            topLeft = Offset(cx - shadowW / 2f, w * 0.88f),
            size = Size(shadowW, w * 0.07f),
        )

        // Corpo com luz vindo de cima à esquerda.
        drawCircle(
            Brush.radialGradient(
                listOf(lighten(body, 0.35f), body, darken(body, 0.18f)),
                center = Offset(cx - r * 0.35f, cy - r * 0.4f),
                radius = r * 1.6f,
            ),
            radius = r, center = Offset(cx, cy),
        )
        // Brilho.
        drawOval(
            Color.White.copy(alpha = 0.45f),
            topLeft = Offset(cx - r * 0.62f, cy - r * 0.72f),
            size = Size(r * 0.5f, r * 0.3f),
        )

        // Olhos (piscam encolhendo na vertical).
        val eyeW = r * 0.24f
        val eyeH = r * 0.32f * blink
        for (side in listOf(-1f, 1f)) {
            val ex = cx + side * r * 0.32f
            val ey = cy - r * 0.05f
            drawOval(Color.White, topLeft = Offset(ex - eyeW / 2f, ey - eyeH / 2f), size = Size(eyeW, eyeH))
            val pupil = eyeW * 0.55f
            val look = if (mood == MascotMood.THINKING) r * 0.05f else 0f
            drawOval(
                Color(0xFF111827),
                topLeft = Offset(ex - pupil / 2f + look, ey - (eyeH * 0.6f) / 2f - look),
                size = Size(pupil, eyeH * 0.6f),
            )
        }
        // Bochechas.
        for (side in listOf(-1f, 1f)) {
            drawCircle(Color(0xFFFF8FA3).copy(alpha = 0.45f), radius = r * 0.11f, center = Offset(cx + side * r * 0.58f, cy + r * 0.25f))
        }
        // Boca.
        translate(cx, cy + r * 0.3f) {
            when (mood) {
                MascotMood.HAPPY -> drawArc(
                    Color(0xFF111827), startAngle = 20f, sweepAngle = 140f, useCenter = false,
                    topLeft = Offset(-r * 0.2f, -r * 0.16f), size = Size(r * 0.4f, r * 0.26f),
                    style = Stroke(width = r * 0.06f, cap = StrokeCap.Round),
                )
                MascotMood.THINKING -> drawCircle(Color(0xFF111827), radius = r * 0.06f, center = Offset(r * 0.06f, 0f))
                MascotMood.CELEBRATING -> drawArc(
                    Color(0xFF111827), startAngle = 0f, sweepAngle = 180f, useCenter = true,
                    topLeft = Offset(-r * 0.24f, -r * 0.18f), size = Size(r * 0.48f, r * 0.36f),
                )
            }
        }
    }
}

private fun lighten(c: Color, f: Float) = Color(c.red + (1 - c.red) * f, c.green + (1 - c.green) * f, c.blue + (1 - c.blue) * f, c.alpha)
private fun darken(c: Color, f: Float) = Color(c.red * (1 - f), c.green * (1 - f), c.blue * (1 - f), c.alpha)
