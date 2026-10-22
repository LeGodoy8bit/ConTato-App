package contato.app.presentation.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.presentation.theme.Motion
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/** Uma página do onboarding: ilustração em cima, título e texto embaixo. */
internal class OnboardingPage(
    val title: String,
    val body: String,
    val illustration: @Composable () -> Unit,
)

internal class OnboardingColors(
    val bg: Color, val title: Color, val body: Color,
    val dotActive: Color, val dotInactive: Color,
    val buttonBg: Color, val buttonFg: Color,
)

/**
 * Onboarding em páginas (referência: telas com ilustração, título grande, bolinhas e botão em pílula).
 * Desliza com o dedo ou pelo botão; a ilustração entra com leve paralaxe.
 * [onSkip] mostra "Pular" no topo (null = sem pular).
 */
@Composable
internal fun OnboardingPager(
    pages: List<OnboardingPage>,
    colors: OnboardingColors,
    finishLabel: String,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    onSkip: (() -> Unit)? = null,
) {
    val state = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    val last = state.currentPage == pages.lastIndex

    Column(modifier.fillMaxSize().background(colors.bg)) {
        Box(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 8.dp), contentAlignment = Alignment.CenterEnd) {
            if (onSkip != null && !last) {
                TextButton(onClick = onSkip) { Text("Pular", color = colors.body, fontWeight = FontWeight.SemiBold) }
            }
        }
        HorizontalPager(state = state, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
            val offset = ((state.currentPage - page) + state.currentPageOffsetFraction)
            val p = pages[page]
            Column(
                Modifier.fillMaxSize().padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Paralaxe: a ilustração anda um pouco mais devagar que o texto.
                            translationX = offset * size.width * 0.25f
                            alpha = 1f - offset.absoluteValue.coerceIn(0f, 1f) * 0.6f
                        },
                    contentAlignment = Alignment.Center,
                ) { p.illustration() }
                Text(
                    p.title, color = colors.title, textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    p.body, color = colors.body, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 24.sp),
                )
                Spacer(Modifier.height(24.dp))
            }
        }
        PageDots(pages.size, state.currentPage, colors, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(24.dp))
        Box(
            Modifier
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .heightIn(min = 60.dp)
                .clip(CircleShape)
                .background(colors.buttonBg)
                .clickable(role = Role.Button) {
                    if (last) onFinish() else scope.launch { state.animateScrollToPage(state.currentPage + 1) }
                }
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = if (last) finishLabel else "Próximo",
                transitionSpec = { fadeIn(tween(Motion.QUICK)) togetherWith fadeOut(tween(Motion.QUICK)) },
                label = "cta",
            ) { label ->
                Text(label, color = colors.buttonFg, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
            }
        }
        Spacer(Modifier.navigationBarsPadding().height(20.dp))
    }
}

/** Bolinhas da página: a atual vira um traço comprido (referência). */
@Composable
internal fun PageDots(count: Int, current: Int, colors: OnboardingColors, modifier: Modifier = Modifier) {
    Row(
        modifier.semantics(mergeDescendants = true) { contentDescription = "Página ${current + 1} de $count" },
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 28.dp else 8.dp, tween(Motion.FAST, easing = Motion.SmoothOut), label = "dotW")
            val color by animateColorAsState(if (i == current) colors.dotActive else colors.dotInactive, tween(Motion.FAST), label = "dotC")
            Box(Modifier.width(width).height(8.dp).clip(CircleShape).background(color))
        }
    }
}
