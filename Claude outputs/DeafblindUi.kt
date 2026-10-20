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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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

// ───────────────────────── Cores ─────────────────────────

/**
 * Cores do modo Surdocego.
 * - Claro: igual ao protótipo (fundo branco, ponto marcado azul forte, pontos vazios azul bem claro,
 *   área de gestos azul claro).
 * - Alto contraste: fundo escuro e branco puro, para baixa visão residual (escolhido nos Ajustes).
 */
internal class DbColors(
    val bg: Color, val surface: Color, val onBg: Color, val muted: Color, val border: Color,
    val dotOn: Color, val onDotOn: Color, val dotOff: Color, val dotOffBorder: Color, val onDotOff: Color,
    val gesture: Color, val onGesture: Color, val accent: Color, val online: Color, val searching: Color,
    val error: Color, val highlightBg: Color, val highlightFg: Color, val dark: Boolean,
)

internal fun dbColors(theme: DeafblindTheme): DbColors = when (theme) {
    DeafblindTheme.LIGHT -> DbColors(
        bg = Color.White, surface = Color(0xFFF3F7FE), onBg = Color(0xFF0E1726), muted = Color(0xFF55627A),
        border = Color(0xFFD6E4FB),
        dotOn = Color(0xFF0062FF), onDotOn = Color.White, dotOff = Color(0xFFE4EEFD), dotOffBorder = Color(0xFFCFE0FB),
        onDotOff = Color(0xFF7D93B8),
        gesture = Color(0xFFCCE0FC), onGesture = Color(0xFF0B2A5B), accent = Color(0xFF0062FF),
        online = Color(0xFF1E8E3E), searching = Color(0xFFB7791F), error = Color(0xFFB3261E),
        highlightBg = Color(0xFF0062FF), highlightFg = Color.White, dark = false,
    )
    DeafblindTheme.HIGH_CONTRAST -> DbColors(
        bg = Color(0xFF0D1420), surface = Color(0xFF1A2333), onBg = Color.White, muted = Color(0xFFB4C0D3),
        border = Color(0xFF2C3850),
        dotOn = Color.White, onDotOn = Color(0xFF0D1420), dotOff = Color(0xFF1A2333), dotOffBorder = Color.White,
        onDotOff = Color.White,
        gesture = Color(0xFF1F3A7A), onGesture = Color.White, accent = Color(0xFF5C97FF),
        online = Color(0xFF4CC26A), searching = Color(0xFFF2C14E), error = Color(0xFFF2B8B5),
        highlightBg = Color.White, highlightFg = Color(0xFF0D1420), dark = true,
    )
}

// ───────────────────────── Cela Braille ─────────────────────────

/**
 * Os 6 pontos na posição real da cela (1 2 3 na coluna da esquerda, 4 5 6 na direita).
 * O tamanho vem de quem chama: na tela principal, o maior que couber.
 */
@Composable
internal fun BrailleCell(
    points: List<Boolean>,
    dotSize: Dp,
    colors: DbColors,
    dimmed: Boolean,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.Center) {
        for (col in 0..1) {
            Column(verticalArrangement = Arrangement.spacedBy(dotSize * CELL_ROW_GAP)) {
                for (row in 0..2) {
                    val index = col * 3 + row
                    BrailleDot(index, points[index], dotSize, colors, dimmed) { onToggle(index) }
                }
            }
            if (col == 0) Spacer(Modifier.width(dotSize * CELL_COL_GAP))
        }
    }
}

/** Espaços da cela, em proporção do ponto (usados para calcular o maior ponto que cabe). */
internal const val CELL_ROW_GAP = 0.14f
internal const val CELL_COL_GAP = 0.2f

@Composable
private fun BrailleDot(index: Int, active: Boolean, size: Dp, colors: DbColors, dimmed: Boolean, onToggle: () -> Unit) {
    val reduce = motionReduced()
    val fill by animateColorAsState(
        if (active) colors.dotOn else colors.dotOff,
        animationSpec = if (reduce) snap() else tween(Motion.QUICK), label = "fill",
    )
    // Pop ao marcar (transitions.dev: curva com leve quique).
    val scale by animateFloatAsState(
        if (active) 1f else 0.96f,
        animationSpec = if (reduce) snap() else tween(Motion.FAST, easing = Motion.Bounce), label = "scale",
    )
    Box(
        Modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .background(fill.copy(alpha = if (dimmed && !active) 0.6f else 1f))
            .border(if (colors.dark) 3.dp else 1.5.dp, if (active) colors.dotOn else colors.dotOffBorder, CircleShape)
            // Sempre tocável: antes da primeira mensagem a sessão responde com a vibração de erro.
            .toggleable(value = active, role = Role.Checkbox, onValueChange = { onToggle() })
            .semantics {
                contentDescription = "Ponto ${index + 1}"
                stateDescription = if (active) "marcado" else "desmarcado"
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "${index + 1}",
            color = if (active) colors.onDotOn else colors.onDotOff,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = (size.value * 0.2f).coerceAtLeast(14f).sp, fontWeight = FontWeight.Bold),
        )
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
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Voltar", tint = colors.onBg, modifier = Modifier.size(28.dp))
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

/** Grupo de opções (uma escolhida), com botões grandes. Leitor de tela anuncia como opções. */
@Composable
internal fun <T> OptionGroup(
    title: String,
    options: List<T>,
    selected: T,
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
