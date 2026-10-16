package contato.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import contato.app.R

/**
 * Inter empacotada dentro do app (res/font), para funcionar offline.
 * Os nomes dos arquivos precisam bater com os de res/font.
 */
val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private val base = Typography()

private fun TextStyle.inter(): TextStyle = copy(fontFamily = InterFamily)

/**
 * Escala do Material 3 (tamanhos padrão) usando Inter.
 * O design system do Figma usa a escala padrão do M3; se algum tamanho
 * específico for customizado lá, ajustar aqui.
 */
val AppTypography = Typography(
    displayLarge = base.displayLarge.inter(),
    displayMedium = base.displayMedium.inter(),
    displaySmall = base.displaySmall.inter(),
    headlineLarge = base.headlineLarge.inter(),
    headlineMedium = base.headlineMedium.inter(),
    headlineSmall = base.headlineSmall.inter(),
    titleLarge = base.titleLarge.inter(),
    titleMedium = base.titleMedium.inter(),
    titleSmall = base.titleSmall.inter(),
    bodyLarge = base.bodyLarge.inter(),
    bodyMedium = base.bodyMedium.inter(),
    bodySmall = base.bodySmall.inter(),
    labelLarge = base.labelLarge.inter(),
    labelMedium = base.labelMedium.inter(),
    labelSmall = base.labelSmall.inter(),
)

/**
 * Título da splash (no Figma: SF Pro Bold, tamanho 32 na versão atual).
 *
 * - letterSpacing de -1sp aproxima o visual do SF Pro, que aperta o espaçamento
 *   sozinho em tamanhos grandes (a Inter não tem versão condensada).
 * - lineHeight de 36sp (cerca de 1,1x a fonte): evita cortar acentos e
 *   letras como "q" e "g", que a altura de linha apertada do Figma cortaria
 *   no Android.
 */
val SplashTitleStyle = TextStyle(
    fontFamily = InterFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 32.sp,
    lineHeight = 36.sp,
    letterSpacing = (-1).sp,
)
