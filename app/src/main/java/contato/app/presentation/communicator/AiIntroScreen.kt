package contato.app.presentation.communicator

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import contato.app.R
import contato.app.presentation.common.Illustration
import contato.app.presentation.common.OnboardingColors
import contato.app.presentation.common.OnboardingPage
import contato.app.presentation.common.OnboardingPager

/**
 * Apresentação da simplificação por IA, mostrada uma vez depois de entrar na conta
 * (e de novo em Ajustes > "Como funciona a IA"). Mesmo formato do onboarding do treino.
 */
@Composable
internal fun AiIntroScreen(aiAvailable: Boolean, onEnable: () -> Unit, onLater: () -> Unit) {
    val c = rememberCommColors()
    val pages = listOf(
        page(
            R.drawable.ill_ai_simplify,
            "Mensagens mais curtas com IA",
            "Antes de enviar, a IA pode sugerir uma versão mais curta da sua frase, com o mesmo sentido.",
        ),
        page(
            R.drawable.ill_ai_light,
            "Menos esforço para sentir",
            "Cada letra é sentida ponto a ponto. Menos letras diminuem a carga cognitiva de quem lê pela vibração.",
        ),
        page(
            R.drawable.ill_ai_time,
            "Conversa mais rápida",
            "Frases enxutas vibram em menos tempo: a mensagem chega antes e a resposta também.",
        ),
        page(
            R.drawable.ill_ai_choice,
            "Você decide, no seu celular",
            "A sugestão nunca é enviada sozinha: toque em Usar ou Manter a minha. A IA roda neste celular, sem internet.",
        ),
    )
    Box(Modifier.fillMaxSize().background(c.bg)) {
    OnboardingPager(
        pages = pages,
        colors = OnboardingColors(
            bg = c.bg, title = c.onSurface, body = c.muted,
            dotActive = c.accent, dotInactive = c.border,
            buttonBg = c.sent, buttonFg = c.onSent,
        ),
        finishLabel = if (aiAvailable) "Ativar a IA" else "Entendi",
        onFinish = onEnable,
        onSkip = onLater,
        lastSecondaryLabel = "Agora não",
        onLastSecondary = onLater,
        modifier = Modifier.statusBarsPadding(),
    )
    }
}

private fun page(@DrawableRes art: Int, title: String, body: String) =
    OnboardingPage(title, body) { Illustration(art, Modifier.fillMaxWidth().height(260.dp)) }
