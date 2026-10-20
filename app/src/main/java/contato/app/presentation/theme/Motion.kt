package contato.app.presentation.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Tokens de movimento do app (adaptados do transitions.dev para Compose).
 * Toda animação nova usa estes valores, para o app ter um ritmo só.
 * Combine sempre com [motionReduced]: com "remover animações" ligado, as telas trocam sem movimento.
 */
object Motion {
    // Durações (ms)
    const val STAGGER = 40
    const val MICRO = 80
    const val QUICK = 150      // fechar, troca de texto
    const val FAST = 250       // abrir, trocar aba, deslizar página
    const val MEDIUM = 350     // fechar painel/aviso
    const val SLOW = 400       // abrir painel/aviso
    const val VERY_SLOW = 500  // momentos de ênfase (conectado, sucesso)

    // Curvas
    val SmoothOut: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)   // abrir/fechar, deslizar
    val InOut: Easing = FastOutSlowInEasing                            // troca de texto/ícone
    val Bounce: Easing = CubicBezierEasing(0.34f, 1.36f, 0.64f, 1f)   // "pop" de confirmação

    // Distâncias
    val DistanceMicro = 4.dp
    val DistanceBase = 8.dp
    val DistanceMedium = 12.dp
}

/** true quando a pessoa desligou as animações no Android (Acessibilidade > Remover animações). */
@Composable
fun motionReduced(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
}
