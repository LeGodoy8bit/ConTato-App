package contato.app.presentation.deafblind

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.domain.service.BrailleMapper
import contato.app.presentation.theme.Motion

/**
 * Treino (como a matriz do Comunicador, mas pensado para quem vai sentir):
 *  - Sentir letras: toque numa letra e o celular vibra a letra, com a força e o ritmo dos Ajustes.
 *  - Praticar: o celular vibra uma letra escondida; a pessoa marca os pontos e confere.
 *    Acerto e erro têm vibrações diferentes. A pessoa de apoio pode mostrar a letra na tela.
 */
@Composable
internal fun DeafblindTrainingScreen(session: DeafblindSession, colors: DbColors, onBack: () -> Unit) {
    var tab by rememberSaveable { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().background(colors.bg).statusBarsPadding().navigationBarsPadding()) {
        DbHeader("Treino", colors, onBack)
        TrainingTabs(tab, colors) { tab = it }
        AnimatedContent(
            targetState = tab,
            transitionSpec = { fadeIn(tween(Motion.FAST)) togetherWith fadeOut(tween(Motion.QUICK)) },
            label = "training",
        ) { t ->
            if (t == 0) FeelLetters(session, colors) else Practice(session, colors)
        }
    }
}

@Composable
private fun TrainingTabs(selected: Int, colors: DbColors, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.surface)
            .border(1.dp, colors.border, CircleShape)
            .padding(4.dp)
            .selectableGroup(),
    ) {
        listOf("Sentir letras", "Praticar").forEachIndexed { i, label ->
            val bg by animateColorAsState(if (i == selected) colors.dotOn else colors.surface, tween(Motion.FAST), label = "tab")
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(CircleShape)
                    .background(bg)
                    .selectable(selected = i == selected, role = Role.Tab) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (i == selected) colors.onDotOn else colors.onBg,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

// ───────────────────────── Sentir letras ─────────────────────────

@Composable
private fun FeelLetters(session: DeafblindSession, colors: DbColors) {
    val letters = remember { BrailleMapper.brailleTable.keys.sorted() }
    var selected by rememberSaveable { mutableStateOf('A') }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        // Letra escolhida em destaque, com a cela.
        DbCard(colors) {
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    (fadeIn(tween(Motion.FAST)) + scaleIn(tween(Motion.FAST, easing = Motion.Bounce), initialScale = 0.92f)) togetherWith
                            fadeOut(tween(Motion.QUICK))
                },
                label = "letter",
            ) { l ->
                val points = BrailleMapper.brailleTable.getValue(l)
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(88.dp).clip(RoundedCornerShape(26.dp)).background(colors.dotOn), contentAlignment = Alignment.Center) {
                        Text("$l", color = colors.onDotOn,
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 48.sp, fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.width(20.dp))
                    MiniCell(points, colors, dot = 16.dp)
                    Spacer(Modifier.width(20.dp))
                    Column {
                        Text("Pontos", color = colors.muted, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp))
                        Text(pointsLabel(points), color = colors.onBg,
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
            DbOutlineButton("Sentir de novo", colors, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                session.playLetter(selected)
            }
        }

        DbSectionLabel("Toque numa letra para sentir", colors)
        BoxWithConstraints {
            val columns = if (maxWidth >= 380.dp) 5 else 4
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                letters.chunked(columns).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { letter ->
                            LetterTile(letter, letter == selected, colors, Modifier.weight(1f)) {
                                selected = letter
                                session.playLetter(letter)
                            }
                        }
                        repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun LetterTile(letter: Char, selected: Boolean, colors: DbColors, modifier: Modifier, onClick: () -> Unit) {
    val points = BrailleMapper.brailleTable.getValue(letter)
    val bg by animateColorAsState(if (selected) colors.dotOn else colors.surface, tween(Motion.FAST), label = "tile")
    Column(
        modifier
            .aspectRatio(0.8f)
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, if (selected) colors.dotOn else colors.border, RoundedCornerShape(20.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = "Letra $letter, pontos ${pointsLabel(points)}. Toque para sentir." },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("$letter", color = if (selected) colors.onDotOn else colors.onBg,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold))
    }
}

// ───────────────────────── Praticar ─────────────────────────

@Composable
private fun Practice(session: DeafblindSession, colors: DbColors) {
    val letters = remember { BrailleMapper.brailleTable.keys.sorted() }
    var target by rememberSaveable { mutableStateOf(letters.random()) }
    var round by rememberSaveable { mutableStateOf(1) }
    var hits by rememberSaveable { mutableStateOf(0) }
    var reveal by rememberSaveable { mutableStateOf(false) }
    var result by remember { mutableStateOf<Boolean?>(null) }
    val points = remember { mutableStateListOf(false, false, false, false, false, false) }

    fun next() {
        target = letters.filter { it != target }.random()
        round++
        result = null
        for (i in points.indices) points[i] = false
        session.playLetter(target)
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Letra $round", color = colors.onBg, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold))
            Text("$hits acerto(s)", color = colors.accent,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
        }
        Spacer(Modifier.height(12.dp))
        DbButton("Sentir a letra", colors) { session.playLetter(target) }
        Spacer(Modifier.height(20.dp))

        // Cela para responder (pontos grandes, mas menores que na tela principal).
        BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            val dot = minOf(maxWidth / (2f + CELL_COL_GAP) * 0.6f, 110.dp)
            BrailleCell(
                points = points,
                dotSize = dot,
                colors = colors,
                dimmed = false,
                onToggle = { i ->
                    points[i] = !points[i]
                    session.dotFeedback(points[i])
                },
            )
        }
        Spacer(Modifier.height(20.dp))

        // Resultado.
        val answer = BrailleMapper.reverseMap(points.toList())
        AnimatedContent(targetState = result, transitionSpec = { fadeIn(tween(Motion.FAST)) togetherWith fadeOut(tween(Motion.QUICK)) }, label = "result") { r ->
            val text = when (r) {
                true -> "Acertou! Era $target."
                false -> "Era $target: pontos ${pointsLabel(BrailleMapper.brailleTable.getValue(target))}." +
                        if (answer != null) " Você marcou $answer." else ""
                null -> "Marque os pontos que sentiu e toque em Conferir."
            }
            Text(
                text,
                color = when (r) { true -> colors.online; false -> colors.error; null -> colors.muted },
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Bold),
            )
        }
        Spacer(Modifier.height(16.dp))

        if (result == null) {
            DbButton("Conferir", colors, enabled = points.any { it }) {
                val correct = answer == target
                result = correct
                if (correct) hits++
                session.trainingResult(correct)
            }
        } else {
            DbButton("Próxima letra", colors) { next() }
        }
        Spacer(Modifier.height(16.dp))

        // Para a pessoa de apoio acompanhar.
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(colors.surface)
                .clickable(role = Role.Switch) { reveal = !reveal }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Mostrar a letra", color = colors.onBg,
                    style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
                Text(if (reveal) "Letra: $target" else "Para a pessoa de apoio acompanhar", color = colors.muted,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp))
            }
            Switch(checked = reveal, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = colors.dotOn))
        }
        Spacer(Modifier.height(24.dp))
    }
}
