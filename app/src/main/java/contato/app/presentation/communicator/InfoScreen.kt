package contato.app.presentation.communicator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.core.config.HapticConfig
import contato.app.domain.service.BrailleMapper
import contato.app.domain.service.HapticTimelineBuilder
import contato.app.presentation.theme.Motion
import contato.app.presentation.theme.motionReduced

internal enum class InfoTab { HOW, MATRIX }

/**
 * "Sobre o ConTato" com duas partes, trocadas por um seletor deslizante:
 *  - Como funciona: o caminho da mensagem, quanto tempo leva e dicas.
 *  - Matriz Braille: as 26 letras; tocar numa letra mostra os pontos e vibra no próprio celular.
 */
@Composable
internal fun InfoScreen(initialTab: InfoTab, onBack: () -> Unit, onPreviewLetter: (Char) -> Unit) {
    val c = rememberCommColors()
    val reduce = motionReduced()
    var tab by rememberSaveable { mutableStateOf(initialTab) }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding()) {
        // Topo: voltar num círculo + título centralizado (referência "Energy Usage").
        Box(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            IconButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterStart)) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(c.surface).border(1.dp, c.border, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = c.onSurface)
                }
            }
            Text("Sobre o ConTato", color = c.onSurface, modifier = Modifier.align(Alignment.Center).semantics { heading() },
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
        }
        Box(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            SegmentedTabs(listOf("Como funciona", "Matriz Braille"), tab.ordinal, c) { tab = InfoTab.entries[it] }
        }
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                if (reduce) fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                else {
                    val dir = if (targetState.ordinal > initialState.ordinal) 1 else -1
                    (slideInHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) { dir * it / 10 } + fadeIn(tween(Motion.FAST))) togetherWith
                            (slideOutHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) { -dir * it / 10 } + fadeOut(tween(Motion.QUICK)))
                }
            },
            label = "info",
        ) { current ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                when (current) {
                    InfoTab.HOW -> HowItWorks(c)
                    InfoTab.MATRIX -> BrailleMatrix(c, onPreviewLetter)
                }
            }
        }
    }
}

// ───────────────────────── Como funciona ─────────────────────────

@Composable
private fun HowItWorks(c: CommColors) {
    Spacer(Modifier.height(8.dp))
    HeroCard(c)

    SectionLabel("O caminho da mensagem", c)
    CommCard(c) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            TimelineStep(Icons.Default.Create, "Você escreve",
                "Até ${HapticConfig.MAX_MESSAGE_LENGTH} letras. Acentos são removidos; números e pontuação não vibram.", c, last = false)
            TimelineStep(Icons.Default.Phone, "Vira Braille",
                "Cada letra vira uma cela de 6 pontos: em relevo ou lisos.", c, last = false)
            TimelineStep(Icons.Default.Notifications, "O celular tátil vibra",
                "Os 6 pontos vibram em ordem, um depois do outro, letra por letra.", c, last = false) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ToneChip("Recebida", Tone.INFO, c)
                    ToneChip("Vibrando 3 de 9: S", Tone.PROGRESS, c)
                }
                Spacer(Modifier.height(6.dp))
                ToneChip("Sentida", Tone.SUCCESS, c, showCheck = true)
            }
            TimelineStep(Icons.Default.Email, "A resposta chega",
                "A pessoa monta a resposta na cela Braille da tela e ela aparece na sua conversa.", c, last = true)
        }
    }

    SectionLabel("Quanto tempo leva", c)
    ReadingTimeCard(c)

    SectionLabel("Dicas", c)
    CommCard(c) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Tip("Prefira frases curtas e diretas.", c)
            Tip("Espere o status \"Sentida\" antes de mandar a próxima.", c)
            Tip("A pessoa pode deslizar para cima e sentir a mensagem de novo.", c)
        }
    }
}

/** Cartão de abertura com degradê suave e dois celulares (referência: cartões com ilustração). */
@Composable
private fun HeroCard(c: CommColors) {
    val gradient = Brush.linearGradient(
        if (c.dark) listOf(Color(0xFF1F3A7A), Color(0xFF1E2838)) else listOf(Color(0xFFDDE8FF), Color(0xFFFFFFFF)),
    )
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(gradient)
            .border(1.dp, c.border, RoundedCornerShape(28.dp))
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Do texto ao toque", color = c.onSurface,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(6.dp))
            Text("Sua mensagem vira vibração inspirada no Braille, de um celular para o outro, sem internet.",
                color = c.muted, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp))
        }
        Spacer(Modifier.width(12.dp))
        TwoPhones(c)
    }
}

@Composable
private fun TwoPhones(c: CommColors) {
    Canvas(Modifier.size(width = 92.dp, height = 84.dp).clearAndSetSemantics { }) {
        val w = size.width * 0.34f
        val h = size.height * 0.86f
        val r = androidx.compose.ui.geometry.CornerRadius(w * 0.2f)
        // Celular que escreve
        drawRoundRect(c.sent, Offset(0f, size.height - h), androidx.compose.ui.geometry.Size(w, h), r)
        // Celular que vibra, com a cela acesa
        val x2 = size.width - w
        drawRoundRect(Color.White, Offset(x2, 0f), androidx.compose.ui.geometry.Size(w, h), r)
        drawRoundRect(c.border, Offset(x2, 0f), androidx.compose.ui.geometry.Size(w, h), r, style = Stroke(1.5.dp.toPx()))
        val dot = w * 0.11f
        for (i in 0 until 6) {
            val cx = x2 + w * (if (i < 3) 0.36f else 0.64f)
            val cy = h * (0.32f + 0.18f * (i % 3))
            drawCircle(if (i in listOf(1, 2, 3, 4)) c.sent else c.border, dot, Offset(cx, cy))
        }
        // Pontinhos indo de um para o outro
        for (i in 0..2) drawCircle(c.sent.copy(alpha = 0.35f + 0.2f * i), dot * 0.7f, Offset(w + (x2 - w) * (0.25f + 0.25f * i), size.height * 0.55f))
    }
}

/** Passo da linha do tempo: ícone num círculo, linha ligando ao próximo, título e texto. */
@Composable
private fun TimelineStep(
    icon: ImageVector, title: String, body: String, c: CommColors, last: Boolean,
    extra: @Composable ColumnScope.() -> Unit = {},
) {
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(10.dp))
            Box(Modifier.size(36.dp).clip(CircleShape).background(c.accentSoft), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = c.accent, modifier = Modifier.size(18.dp))
            }
            if (!last) Box(Modifier.width(2.dp).weight(1f).background(c.border))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(top = 10.dp, bottom = if (last) 10.dp else 16.dp)) {
            Text(title, color = c.onSurface,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Bold))
            Text(body, color = c.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp))
            extra()
        }
    }
}

/** Número em destaque (referência "12.6 kWh"): tempo médio por letra, calculado com a vibração real. */
@Composable
private fun ReadingTimeCard(c: CommColors) {
    val example = "BOM DIA"
    val seconds = remember { HapticTimelineBuilder().buildTimeline(example).totalMs / 1000f }
    val perLetter = seconds / example.replace(" ", "").length
    CommCard(c) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f s", perLetter), color = c.onSurface,
                    style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 44.sp, fontWeight = FontWeight.Bold))
                Spacer(Modifier.width(8.dp))
                Text("por letra, em média", color = c.muted, modifier = Modifier.padding(bottom = 6.dp),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
            }
            Spacer(Modifier.height(6.dp))
            Text(
                String.format(java.util.Locale.forLanguageTag("pt-BR"), "\"%s\" leva cerca de %.0f segundos para vibrar.", example, seconds),
                color = c.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 14.sp, lineHeight = 20.sp),
            )
            Spacer(Modifier.height(14.dp))
            LegendRow(true, "Ponto em relevo: vibração forte de ${HapticConfig.ACTIVE_DURATION} ms", c)
            Spacer(Modifier.height(8.dp))
            LegendRow(false, "Ponto liso: vibração fraca de ${HapticConfig.INACTIVE_DURATION} ms", c)
        }
    }
}

@Composable
private fun LegendRow(filled: Boolean, text: String, c: CommColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(16.dp).clip(CircleShape)
            .then(if (filled) Modifier.background(c.accent) else Modifier.border(2.dp, c.muted, CircleShape)))
        Spacer(Modifier.width(10.dp))
        Text(text, color = c.onSurface, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp))
    }
}

@Composable
private fun Tip(text: String, c: CommColors) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(c.accent))
        Spacer(Modifier.width(10.dp))
        Text(text, color = c.onSurface, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 21.sp))
    }
}

// ───────────────────────── Matriz Braille ─────────────────────────

@Composable
private fun BrailleMatrix(c: CommColors, onPreviewLetter: (Char) -> Unit) {
    val letters = remember { BrailleMapper.brailleTable.keys.sorted() }
    var selected by rememberSaveable { mutableStateOf('A') }

    Spacer(Modifier.height(8.dp))
    LetterDetail(selected, c, onFeel = { onPreviewLetter(selected) })

    SectionLabel("Toque numa letra para ver e sentir", c)
    BoxWithConstraints {
        val columns = if (maxWidth >= 360.dp) 5 else 4
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            letters.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { letter ->
                        MatrixCell(
                            letter = letter,
                            selected = letter == selected,
                            c = c,
                            modifier = Modifier.weight(1f),
                            onClick = { selected = letter; onPreviewLetter(letter) },
                        )
                    }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

/** Letra escolhida em destaque, com os pontos grandes. Troca com um "pop" suave. */
@Composable
private fun LetterDetail(letter: Char, c: CommColors, onFeel: () -> Unit) {
    CommCard(c) {
        AnimatedContent(
            targetState = letter,
            transitionSpec = {
                (fadeIn(tween(Motion.FAST)) + scaleIn(tween(Motion.FAST, easing = Motion.Bounce), initialScale = 0.92f)) togetherWith
                        fadeOut(tween(Motion.QUICK))
            },
            label = "detail",
        ) { l ->
            val p = BrailleMapper.brailleTable.getValue(l)
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(84.dp).clip(RoundedCornerShape(24.dp)).background(c.sent),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$l", color = Color.White,
                        style = MaterialTheme.typography.displaySmall.copy(fontSize = 44.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(Modifier.width(18.dp))
                CellDrawing(p, c, dot = 9.dp)
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f)) {
                    Text("Pontos", color = c.muted, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
                    Text(p.mapIndexedNotNull { i, on -> if (on) i + 1 else null }.joinToString(", "), color = c.onSurface,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
        Row(
            Modifier
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
                .clip(CircleShape)
                .background(c.accentSoft)
                .clickable(role = Role.Button, onClickLabel = "Sentir a letra $letter", onClick = onFeel)
                .heightIn(min = 44.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Refresh, null, tint = c.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Sentir de novo", color = c.accent, style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun CellDrawing(points: List<Boolean>, c: CommColors, dot: Dp) {
    Canvas(Modifier.size(width = dot * 5, height = dot * 7.4f).clearAndSetSemantics { }) {
        val r = dot.toPx()
        val xs = listOf(r + 1f, size.width - r - 1f)
        val ys = listOf(r + 1f, size.height / 2f, size.height - r - 1f)
        for (i in 0 until 6) {
            val center = Offset(xs[i / 3], ys[i % 3])
            if (points[i]) drawCircle(c.accent, r, center)
            else drawCircle(c.muted.copy(alpha = 0.6f), r - 1f, center, style = Stroke(1.5.dp.toPx()))
        }
    }
}

@Composable
private fun MatrixCell(letter: Char, selected: Boolean, c: CommColors, modifier: Modifier, onClick: () -> Unit) {
    val points = BrailleMapper.brailleTable.getValue(letter)
    val bg by animateColorAsState(if (selected) c.sent else c.surface, tween(Motion.FAST), label = "cellBg")
    val fg by animateColorAsState(if (selected) Color.White else c.onSurface, tween(Motion.FAST), label = "cellFg")
    val active = points.mapIndexedNotNull { i, on -> if (on) i + 1 else null }.joinToString(", ")
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, if (selected) c.sent else c.border, RoundedCornerShape(18.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp)
            .clearAndSetSemantics {
                contentDescription = "Letra $letter, pontos $active. Toque para sentir."
                this.selected = selected
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$letter", color = fg, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(6.dp))
        Canvas(Modifier.size(width = 22.dp, height = 32.dp)) {
            val r = 4.dp.toPx()
            val xs = listOf(r + 1f, size.width - r - 1f)
            val ys = listOf(r + 1f, size.height / 2f, size.height - r - 1f)
            for (i in 0 until 6) {
                val center = Offset(xs[i / 3], ys[i % 3])
                if (points[i]) drawCircle(if (selected) Color.White else c.accent, r, center)
                else drawCircle((if (selected) Color.White else c.muted).copy(alpha = 0.5f), r - 1f, center, style = Stroke(1.2.dp.toPx()))
            }
        }
    }
}
