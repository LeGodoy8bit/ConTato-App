package contato.app.presentation.deafblind

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.domain.model.DeafblindTheme
import contato.app.presentation.theme.Motion
import contato.app.presentation.theme.motionReduced
import contato.app.presentation.theme.ContatoIcons

// ───────────────────────── Cores ─────────────────────────

/**
 * Cores do modo Surdocego.
 * - Claro: fundo branco, bolas azuis grandes (marcada = azul forte, desmarcada = azul clarinho com
 *   contorno azul médio, sempre visível) e a grade quase invisível.
 * - Alto contraste: fundo quase preto e amarelo forte (a combinação mais legível para baixa visão).
 */
internal class DbColors(
    val bg: Color, val surface: Color, val onBg: Color, val muted: Color, val border: Color,
    val squareOff: Color, val squareOn: Color, val squareBorder: Color,
    val dotOn: Color, val onDotOn: Color, val dotOff: Color, val dotOffBorder: Color, val onDotOff: Color,
    val gesture: Color, val onGesture: Color, val accent: Color, val online: Color, val searching: Color,
    val error: Color, val highlightBg: Color, val highlightFg: Color, val dark: Boolean,
    /** Linhas finas da grade da cela (quase invisíveis). */
    val gridLine: Color,
    /** Halo atrás da bola marcada. */
    val dotGlow: Color,
)

internal fun dbColors(theme: DeafblindTheme): DbColors = when (theme) {
    DeafblindTheme.LIGHT -> DbColors(
        bg = Color.White, surface = Color(0xFFF3F7FE), onBg = Color(0xFF0E1726), muted = Color(0xFF55627A),
        border = Color(0xFFD6E4FB),
        squareOff = Color(0xFFEDF3FD), squareOn = Color(0xFFF0F5FF), squareBorder = Color(0xFFC9D9F3),
        dotOn = Color(0xFF0057FF), onDotOn = Color.White, dotOff = Color(0xFFE5EEFD), dotOffBorder = Color(0xFF6E92D8),
        onDotOff = Color(0xFF3F5E96),
        gesture = Color(0xFFCCE0FC), onGesture = Color(0xFF0B2A5B), accent = Color(0xFF0057FF),
        online = Color(0xFF18793A), searching = Color(0xFFB7791F), error = Color(0xFFB3261E),
        highlightBg = Color(0xFF0057FF), highlightFg = Color.White, dark = false,
        gridLine = Color(0xFFE3EAF6), dotGlow = Color(0xFF0057FF).copy(alpha = 0.16f),
    )
    DeafblindTheme.HIGH_CONTRAST -> DbColors(
        bg = Color(0xFF0B0F19), surface = Color(0xFF161C2B), onBg = Color.White, muted = Color(0xFFA9B4C8),
        border = Color(0xFF2A3347),
        squareOff = Color(0xFF131926), squareOn = Color(0xFF1B1A12), squareBorder = Color(0xFF232B3D),
        dotOn = Color(0xFFFFD84D), onDotOn = Color(0xFF0B0F19), dotOff = Color(0xFF1A2133), dotOffBorder = Color(0xFF8C98B2),
        onDotOff = Color(0xFFD5DCEA),
        gesture = Color(0xFF1A2236), onGesture = Color.White, accent = Color(0xFFFFD84D),
        online = Color(0xFF5BD17A), searching = Color(0xFFFFB547), error = Color(0xFFFF8A80),
        highlightBg = Color(0xFFFFD84D), highlightFg = Color(0xFF0B0F19), dark = true,
        gridLine = Color(0xFF232B3E), dotGlow = Color(0xFFFFD84D).copy(alpha = 0.18f),
    )
}

// ───────────────────────── Cela Braille ─────────────────────────

/**
 * Os 6 pontos na posição real da cela (1 2 3 na coluna da esquerda, 4 5 6 na direita), numa grade
 * 3 × 2 que vai até as bordas: cada sexto da área é um alvo de toque inteiro, sem espaços mortos.
 * As divisões são linhas finas quase invisíveis; o que aparece são as bolas, grandes e com contraste.
 */
@Composable
internal fun BrailleCell(
    points: List<Boolean>,
    colors: DbColors,
    dimmed: Boolean,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val line = colors.gridLine
    Column(
        modifier.drawBehind {
            val stroke = 1.dp.toPx()
            val inset = 20.dp.toPx() // as linhas não encostam nas bordas: a grade fica leve
            drawLine(line, Offset(size.width / 2, inset), Offset(size.width / 2, size.height - inset), stroke)
            for (i in 1..2) {
                val y = size.height * i / 3f
                drawLine(line, Offset(inset, y), Offset(size.width - inset, y), stroke)
            }
        },
    ) {
        for (row in 0..2) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                for (col in 0..1) {
                    val index = col * 3 + row
                    BrailleSquare(index, row, col, points[index], colors, dimmed, Modifier.weight(1f).fillMaxHeight()) { onToggle(index) }
                }
            }
        }
    }
}

@Composable
private fun BrailleSquare(
    index: Int, row: Int, col: Int, active: Boolean, colors: DbColors, dimmed: Boolean,
    modifier: Modifier, onToggle: () -> Unit,
) {
    val reduce = motionReduced()
    val fill by animateColorAsState(
        if (active) colors.dotOn else colors.dotOff,
        animationSpec = if (reduce) snap() else tween(Motion.QUICK), label = "fill",
    )
    val ring by animateColorAsState(
        if (active) colors.dotOn else colors.dotOffBorder,
        animationSpec = if (reduce) snap() else tween(Motion.QUICK), label = "ring",
    )
    // Pop ao marcar (curva com leve quique) e halo que aparece atrás da bola marcada.
    val scale by animateFloatAsState(
        if (active) 1f else 0.92f,
        animationSpec = if (reduce) snap() else tween(Motion.FAST, easing = Motion.Bounce), label = "scale",
    )
    val glow by animateFloatAsState(
        if (active) 1f else 0f,
        animationSpec = if (reduce) snap() else tween(Motion.FAST), label = "glow",
    )
    // Bolas puxadas para o centro da grade: juntas parecem uma cela Braille de verdade,
    // mas o quadrado inteiro (invisível) continua sendo a área de toque.
    val align = BiasAlignment(
        horizontalBias = if (col == 0) 0.7f else -0.7f,
        verticalBias = when (row) { 0 -> 0.6f; 2 -> -0.6f; else -> 0f },
    )
    BoxWithConstraints(
        modifier
            // A área inteira é tocável, sem marca de toque (o quadrado é invisível).
            // Sempre ativa: antes da 1ª mensagem a sessão responde com a vibração de erro.
            .toggleable(
                value = active,
                interactionSource = null,
                indication = null,
                role = Role.Checkbox,
                onValueChange = { onToggle() },
            )
            .semantics {
                contentDescription = "Ponto ${index + 1}"
                stateDescription = when {
                    active -> "marcado"
                    dimmed -> "desmarcado, aguarde uma mensagem"
                    else -> "desmarcado"
                }
            },
        contentAlignment = align,
    ) {
        val dot = minOf(maxWidth, maxHeight) * 0.90f
        // Antes da 1ª mensagem só o preenchimento fica mais apagado; o contorno continua visível.
        val fillAlpha = if (dimmed && !active) 0.55f else 1f
        // Halo (cabe dentro do quadrado).
        Box(
            Modifier
                .size(minOf(dot * 1.08f, minOf(maxWidth, maxHeight)))
                .graphicsLayer { this.alpha = glow; scaleX = 0.9f + 0.1f * glow; scaleY = scaleX }
                .clip(CircleShape)
                .background(colors.dotGlow),
        )
        Box(
            Modifier
                .size(dot)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(fill.copy(alpha = fill.alpha * fillAlpha))
                .then(if (active) Modifier else Modifier.border(3.dp, ring, CircleShape)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${index + 1}",
                color = if (active) colors.onDotOn else colors.onDotOff,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = (dot.value * 0.2f).coerceIn(16f, 34f).sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
    }
}

/** Cela pequena só para mostrar (treino, letra atual). */
@Composable
internal fun MiniCell(points: List<Boolean>, colors: DbColors, dot: Dp = 12.dp) {
    Row(Modifier.semantics(mergeDescendants = true) {}) {
        for (col in 0..1) {
            Column(verticalArrangement = Arrangement.spacedBy(dot * 0.35f)) {
                for (row in 0..2) {
                    val on = points[col * 3 + row]
                    Box(
                        Modifier.size(dot).clip(CircleShape)
                            .background(if (on) colors.dotOn else Color.Transparent)
                            .border(1.5.dp, if (on) colors.dotOn else colors.dotOffBorder, CircleShape),
                    )
                }
            }
            if (col == 0) Spacer(Modifier.width(dot * 0.5f))
        }
    }
}

internal fun pointsLabel(points: List<Boolean>): String =
    points.mapIndexedNotNull { i, on -> if (on) i + 1 else null }.joinToString(", ")

// ───────────────────────── Blocos de tela ─────────────────────────

/** Topo das telas secundárias: voltar (quadrado arredondado) + título. */
@Composable
internal fun DbHeader(title: String, colors: DbColors, onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.surface)
                .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                .clickable(role = Role.Button, onClickLabel = "Voltar", onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(ContatoIcons.ChevronLeft, contentDescription = "Voltar", tint = colors.onBg, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(title, color = colors.onBg, modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
internal fun DbSectionLabel(text: String, colors: DbColors) {
    Text(
        text.uppercase(), color = colors.muted,
        modifier = Modifier.padding(start = 4.dp, top = 22.dp, bottom = 8.dp).semantics { heading() },
        style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
    )
}

@Composable
internal fun DbCard(colors: DbColors, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp)),
        content = content,
    )
}

/** Grupo de opções (uma escolhida, ou nenhuma = personalizado), com botões grandes. Leitor de tela anuncia como opções. */
@Composable
internal fun <T> OptionGroup(
    title: String,
    options: List<T>,
    selected: T?,
    label: (T) -> String,
    colors: DbColors,
    onSelect: (T) -> Unit,
) {
    Column(Modifier.padding(16.dp)) {
        Text(title, color = colors.onBg,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { option ->
                val isSelected = option == selected
                val bg by animateColorAsState(if (isSelected) colors.dotOn else colors.bg, tween(Motion.FAST), label = "opt")
                Box(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(bg)
                        .border(1.5.dp, if (isSelected) colors.dotOn else colors.border, RoundedCornerShape(16.dp))
                        .selectable(selected = isSelected, role = Role.RadioButton) { onSelect(option) }
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(label(option), color = if (isSelected) colors.onDotOn else colors.onBg, textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
    }
}

/** Botão principal grande (pílula), no tom da cela. */
@Composable
internal fun DbButton(text: String, colors: DbColors, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clip(CircleShape)
            .background(if (enabled) colors.dotOn else colors.border)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (enabled) colors.onDotOn else colors.muted, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
    }
}

/** Botão secundário (contorno). */
@Composable
internal fun DbOutlineButton(text: String, colors: DbColors, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(CircleShape)
            .border(1.5.dp, colors.dotOn, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = colors.accent, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
    }
}
