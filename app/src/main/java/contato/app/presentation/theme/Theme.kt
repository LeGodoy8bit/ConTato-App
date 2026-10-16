package contato.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = ContatoBlue,
    onPrimary = Color.White,
    primaryContainer = ContatoBlueContainer,
    onPrimaryContainer = ContatoOnBlueContainer,
    background = Color.White,
    onBackground = ContatoNavy,
    surface = Color.White,
    onSurface = ContatoNavy,
)

/**
 * PROVISÓRIO: só os papéis essenciais estão definidos. Os demais usam o
 * padrão do Material 3 até o design system trazer os valores reais.
 *
 * O primary do tema escuro é um azul mais claro (ContatoBlueOnDark, 5,17:1
 * sobre o fundo), porque o #005DFF puro só tem 2,83:1 e reprovaria na WCAG 2.2.
 * Por isso o texto sobre ele é navy (também 5,17:1), e não branco.
 */
private val DarkColors = darkColorScheme(
    primary = ContatoBlueOnDark,
    onPrimary = ContatoNavy,
    background = ContatoDarkSurface,
    onBackground = Color.White,
    surface = ContatoDarkSurface,
    onSurface = Color.White,
)

/**
 * Tema do app. Segue o tema do sistema (claro/escuro) por padrão.
 * Não usa cores dinâmicas do Android: a identidade é a da marca.
 */
@Composable
fun ContatoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content,
    )
}

