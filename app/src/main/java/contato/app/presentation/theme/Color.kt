package contato.app.presentation.theme

import androidx.compose.ui.graphics.Color

// --- Cores oficiais da marca ---
// Confirmadas no SVG do logo e nos frames da splash.

/** Azul oficial (decisão do grupo). O botão antigo do design system usava #006FEE. */
val ContatoBlue = Color(0xFF005DFF)

/** Navy do "Con" no logo e dos pontos do símbolo. */
val ContatoNavy = Color(0xFF1E2838)

// --- Lidas do botão "Iniciar Comunicação" no Figma (variante padrão) ---

/** Fundo do botão em estado padrão. */
val ContatoBlueContainer = Color(0xFFEDF4FD)

/** Texto do botão em estado padrão. */
val ContatoOnBlueContainer = Color(0xFF0B46AA)

// --- Tema escuro ---

/**
 * Azul para texto, ícones e botões sobre fundo escuro. Mesmo matiz (218°) do
 * azul da marca, mais claro: contraste de 5,17:1 sobre o navy (AA exige 4,5:1
 * para texto e 3:1 para elementos de interface). O #005DFF puro dá só 2,83:1.
 *
 * O logo e os pontos do símbolo continuam em #005DFF: a WCAG isenta
 * logotipos da exigência de contraste.
 */
val ContatoBlueOnDark = Color(0xFF5C97FF)

// --- PROVISÓRIO ---

/**
 * Fundo do tema escuro. Ainda não foi definido no Figma: as variáveis do
 * arquivo estão no roxo padrão do Material 3. Por enquanto reaproveita o navy
 * da marca; trocar aqui quando o valor oficial existir.
 */
val ContatoDarkSurface = ContatoNavy
