package contato.app.presentation.deafblind

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.presentation.theme.ContatoIcons
import contato.app.presentation.theme.Motion
import contato.app.presentation.theme.motionReduced
import kotlin.math.abs
import androidx.compose.ui.semantics.clearAndSetSemantics

/** Um gesto da área de gestos: o nome curto (dica na tela e leitor de tela) e o que faz. */
internal class PadGesture(val label: String, val run: () -> Unit)

/**
 * Área de gestos do treino, com os MESMOS gestos da tela principal
 * (toque, ← → ↑ ↓), para a pessoa treinar do jeito que vai conversar.
 * Gesto sem função nesta etapa ([PadGesture] null) é ignorado.
 */
@Composable
internal fun TrainingGesturePad(
    colors: DbColors,
    modifier: Modifier = Modifier,
    tap: PadGesture? = null,
    left: PadGesture? = null,
    right: PadGesture? = null,
    up: PadGesture? = null,
    down: PadGesture? = null,
) {
    val reduce = motionReduced()
    val flash = remember(colors) { Animatable(colors.gesture) }
    var hits by remember { mutableIntStateOf(0) }
    LaunchedEffect(hits, colors) {
        if (hits == 0 || reduce) return@LaunchedEffect
        flash.snapTo(colors.dotOn.copy(alpha = 0.35f).overOpaque(colors.gesture))
        flash.animateTo(colors.gesture, tween(Motion.SLOW, easing = Motion.SmoothOut))
    }
    val current by rememberUpdatedState(listOf(tap, left, right, up, down))
    fun fire(g: PadGesture?) {
        if (g == null) return
        hits++
        g.run()
    }

    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(flash.value)
            .pointerInput(Unit) { detectTapGestures(onTap = { fire(current[0]) }) }
            .pointerInput(Unit) {
                val threshold = 48.dp.toPx()
                var total = Offset.Zero
                detectDragGestures(
                    onDragStart = { total = Offset.Zero },
                    onDrag = { change, amount -> change.consume(); total += amount },
                    onDragEnd = {
                        val horizontal = abs(total.x) > abs(total.y)
                        when {
                            horizontal && total.x < -threshold -> fire(current[1])
                            horizontal && total.x > threshold -> fire(current[2])
                            !horizontal && total.y < -threshold -> fire(current[3])
                            !horizontal && total.y > threshold -> fire(current[4])
                        }
                    },
                )
            }
            .semantics {
                contentDescription = "Área de gestos"
                tap?.let { g -> onClick(label = g.label) { fire(g); true } }
                customActions = listOfNotNull(left, right, up, down).map { g ->
                    CustomAccessibilityAction(g.label) { fire(g); true }
                }
            }
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            PadHint(ContatoIcons.TouchApp, "toque", tap, colors)
            PadHint(ContatoIcons.ChevronLeft, "esquerda", left, colors)
            PadHint(ContatoIcons.ChevronRight, "direita", right, colors)
            PadHint(ContatoIcons.ChevronUp, "cima", up, colors)
            PadHint(ContatoIcons.ChevronDown, "baixo", down, colors)
        }
    }
}

@Composable
private fun PadHint(icon: ImageVector, gesture: String, action: PadGesture?, colors: DbColors) {
    if (action == null) return
    // As ações já estão no leitor de tela (customActions da área): aqui é só visual.
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clearAndSetSemantics { }) {
        Icon(icon, contentDescription = null, tint = colors.onGesture.copy(alpha = 0.85f), modifier = Modifier.size(22.dp))
        Text(gesture, color = colors.onGesture.copy(alpha = 0.8f),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp))
        Text(action.label.lowercase(), color = colors.onGesture,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
    }
}

/** Cor translúcida sobre outra, resultando numa cor opaca (para o "flash"). */
private fun Color.overOpaque(background: Color): Color = Color(
    red = red * alpha + background.red * (1 - alpha),
    green = green * alpha + background.green * (1 - alpha),
    blue = blue * alpha + background.blue * (1 - alpha),
    alpha = 1f,
)
