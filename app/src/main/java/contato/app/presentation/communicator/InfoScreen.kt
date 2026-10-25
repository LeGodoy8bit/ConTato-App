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
import androidx.compose.material.icons.automirrored.filled.Send
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
import contato.app.presentation.common.Mascot
import contato.app.presentation.common.MascotMood
import contato.app.presentation.common.OnboardingColors
import contato.app.presentation.common.OnboardingPage
import contato.app.presentation.common.OnboardingPager
import contato.app.presentation.theme.Motion
import contato.app.presentation.theme.motionReduced

internal enum class InfoTab { HOW, MATRIX }

/**
 * "Sobre o ConTato" com duas partes, trocadas por um seletor deslizante:
 *  - Como funciona: onboarding em páginas, com o mascote (do texto à vibração, tempo e dicas).
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
            when (current) {
                InfoTab.HOW -> HowItWorksPager(c, onFinish = { tab = InfoTab.MATRIX })
                InfoTab.MATRIX -> Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                ) { BrailleMatrix(c, onPreviewLetter) }
            }
        }
    }
}

// ───────────────────────── Como funciona (onboarding) ─────────────────────────

/** Uma tela por passo, como nas referências: ilustração, título grande, texto e bolinhas. */
@Composable
private fun HowItWorksPager(c: CommColors, onFinish: () -> Unit) {
    val example = "BOM DIA"
    val seconds = remember { HapticTimelineBuilder().buildTimeline(example).totalMs / 1000f }
    val perLetter = seconds / example.replace(" ", "").length
    val br = java.util.Locale.forLanguageTag("pt-BR")

    val pages = listOf(
        OnboardingPage(
            "Você escreve",
            "Até ${HapticConfig.MAX_MESSAGE_LENGTH} letras. Acentos são removidos; números e pontuação não vibram.",
        ) { WriteIllustration(c) },
        OnboardingPage(
            "Vira Braille",
            "Cada letra vira uma cela de 6 pontos. Ponto em relevo vibra forte; ponto liso vibra fraco.",
        ) { BrailleIllustration(c) },
        OnboardingPage(
            "O celular tátil vibra",
            "Os 6 pontos vibram em ordem, letra por letra. A pessoa sente a mensagem na mão.",
        ) { Mascot(210.dp, mood = MascotMood.FOCUSED, vibrating = true) },
        OnboardingPage(
            "Você acompanha cada letra",
            "Na conversa aparece em que letra a vibração está. Quando termina, a mensagem fica como Sentida.",
        ) { StatusIllustration(c) },
        OnboardingPage(
            "A resposta volta",
            "A pessoa monta a resposta tocando nos 6 quadrados da tela e desliza para cima para enviar.",
        ) { Mascot(200.dp, mood = MascotMood.EXCITED) },
        OnboardingPage(
            String.format(br, "%.1f s por letra", perLetter),
            String.format(br, "Em média. \"%s\" leva cerca de %.0f segundos. A velocidade muda nos Ajustes do celular tátil.", example, seconds),
        ) { TimeIllustration(c, perLetter) },
        OnboardingPage(
            "Dicas",
            "Frases curtas e diretas. Espere o Sentida antes da próxima. Para sentir de novo, a pessoa desliza para baixo.",
        ) { Mascot(200.dp, mood = MascotMood.CURIOUS) },
    )
    OnboardingPager(
        pages = pages,
        colors = OnboardingColors(
            bg = c.bg, title = c.onSurface, body = c.muted,
            dotActive = c.onSurface, dotInactive = c.border,
            buttonBg = c.sent, buttonFg = Color.White,
        ),
        finishLabel = "Ver a matriz Braille",
        onFinish = onFinish,
    )
}

@Composable
private fun WriteIllustration(c: CommColors) {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(230.dp).clip(CircleShape).background(c.accentSoft))
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Oi! Tudo bem?", color = Color.White,
                modifier = Modifier.clip(RoundedCornerShape(18.dp, 18.dp, 5.dp, 18.dp)).background(c.sent).padding(horizontal = 14.dp, vertical = 10.dp),
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp))
            Row(
                Modifier.clip(CircleShape).background(c.surface).border(1.dp, c.border, CircleShape).padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Escreva uma mensagem", color = c.muted, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(28.dp).clip(CircleShape).background(c.sent), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}

@Composable
private fun BrailleIllustration(c: CommColors) {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(230.dp).clip(CircleShape).background(c.accentSoft))
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf('O', 'I').forEach { l ->
                Column(
                    Modifier.clip(RoundedCornerShape(24.dp)).background(c.surface).border(1.dp, c.border, RoundedCornerShape(24.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("$l", color = c.onSurface, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold))
                    Spacer(Modifier.height(8.dp))
                    CellDrawing(BrailleMapper.brailleTable.getValue(l), c, dot = 9.dp)
                }
            }
        }
    }
}

@Composable
private fun StatusIllustration(c: CommColors) {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(230.dp).clip(CircleShape).background(c.accentSoft))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ToneChip("Recebida", Tone.INFO, c)
            ToneChip("Vibrando 3 de 9: S", Tone.PROGRESS, c)
            ToneChip("Sentida", Tone.SUCCESS, c, showCheck = true)
        }
    }
}

@Composable
private fun TimeIllustration(c: CommColors, perLetter: Float) {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(230.dp).clip(CircleShape).background(c.accentSoft))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Mascot(120.dp, mood = MascotMood.CALM)
            Text(String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f s", perLetter), color = c.accent,
                style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, fontWeight = FontWeight.Bold))
            Text("por letra", color = c.muted, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
        }
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
