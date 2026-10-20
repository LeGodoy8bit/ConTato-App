package contato.app.presentation.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Um item do resumo dos termos para o modo Surdocego.
 *
 * @param label texto exibido na tela (para a pessoa de apoio e para o TalkBack).
 * @param hapticText palavra(s) vibradas em Braille. Devem ser CURTAS: cada letra
 *        leva cerca de 2 s para vibrar (6 pontos + pausa), então "PRIVADO" já dura ~15 s.
 *        Use só letras sem acento e espaço (o BrailleTextNormalizer limpa o resto).
 */
internal class HapticSummaryItem(
    val label: String,
    val hapticText: String,
    val icon: ImageVector,
)

/**
 * Versão resumida dos Termos de Uso e da Política de Privacidade, sentida por vibração.
 *
 * Cada toque na tela vibra o PRÓXIMO item. Depois do último, volta ao primeiro.
 * Durações aproximadas: PRIVADO ~15 s, SEM URGENCIA ~24 s, ACEITA ~12 s.
 *
 * Ao mudar estes itens de forma relevante, altere também TERMS_VERSION.
 */
internal object TermsHapticSummary {

    val items = listOf(
        HapticSummaryItem(
            label = "Privado: de celular a celular, sem nome, e-mail ou localização",
            hapticText = "PRIVADO",
            icon = Icons.Default.Lock,
        ),
        HapticSummaryItem(
            label = "Não serve para emergências",
            hapticText = "SEM URGENCIA",
            icon = Icons.Default.Warning,
        ),
        HapticSummaryItem(
            label = "Toque duas vezes para aceitar",
            hapticText = "ACEITA",
            icon = Icons.Default.Check,
        ),
    )
}
