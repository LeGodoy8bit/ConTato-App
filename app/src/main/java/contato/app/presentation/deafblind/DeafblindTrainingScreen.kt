package contato.app.presentation.deafblind

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.data.TrainingProgressPreferencesRepository
import contato.app.domain.model.TrainingModule
import contato.app.domain.service.BrailleMapper
import contato.app.domain.service.TrainingCatalog
import contato.app.presentation.common.Mascot
import contato.app.presentation.common.MascotMood
import contato.app.presentation.common.OnboardingColors
import contato.app.presentation.common.OnboardingPage
import contato.app.presentation.common.OnboardingPager
import contato.app.presentation.theme.Motion
import contato.app.presentation.theme.ContatoIcons
import androidx.compose.foundation.selection.toggleable

/**
 * Treino em módulos:
 *  1. Introdução em páginas (para a pessoa de apoio entender), só na primeira vez.
 *  2. Lista de módulos: Letras → Palavras → Frases, com progresso salvo.
 *  3. Lição: sentir o item (vibra sozinho) → responder na cela → resultado com o mascote.
 */
@Composable
internal fun DeafblindTrainingScreen(session: DeafblindSession, colors: DbColors, onBack: () -> Unit) {
    val context = LocalContext.current
    val progress = remember { TrainingProgressPreferencesRepository(context) }
    var showIntro by rememberSaveable { mutableStateOf(!progress.introSeen()) }
    var moduleId by rememberSaveable { mutableStateOf<String?>(null) }
    // Contador para a lista reler o progresso ao voltar de uma lição.
    var refresh by remember { mutableStateOf(0) }

    BackHandler(enabled = moduleId != null || showIntro) {
        if (moduleId != null) { moduleId = null; refresh++ } else { showIntro = false; progress.setIntroSeen() }
    }

    val target = when {
        showIntro -> "intro"
        moduleId != null -> "lesson"
        else -> "modules"
    }
    AnimatedContent(
        targetState = target,
        transitionSpec = {
            val forward = targetState == "lesson" || initialState == "intro"
            (slideInHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) { if (forward) it / 5 else -it / 5 } + fadeIn(tween(Motion.FAST))) togetherWith
                    (slideOutHorizontally(tween(Motion.FAST, easing = Motion.SmoothOut)) { if (forward) -it / 5 else it / 5 } + fadeOut(tween(Motion.QUICK)))
        },
        label = "trainingNav",
    ) { t ->
        when (t) {
            "intro" -> TrainingIntro(colors, onDone = { progress.setIntroSeen(); showIntro = false })
            "lesson" -> {
                val module = TrainingCatalog.module(moduleId ?: "")
                if (module == null) {
                    LaunchedEffect(Unit) { moduleId = null }
                } else {
                    Lesson(
                        session = session,
                        module = module,
                        startAt = progress.completed(module.id).let { if (it >= module.items.size) 0 else it },
                        colors = colors,
                        onProgress = { done -> if (done > progress.completed(module.id)) progress.setCompleted(module.id, done) },
                        onBack = { moduleId = null; refresh++ },
                    )
                }
            }
            else -> ModuleList(
                colors = colors,
                completed = remember(refresh) { TrainingCatalog.modules.associate { it.id to progress.completed(it.id) } },
                onBack = onBack,
                onOpen = { moduleId = it },
                onIntro = { showIntro = true },
            )
        }
    }
}

// ───────────────────────── Introdução (onboarding) ─────────────────────────

@Composable
private fun TrainingIntro(colors: DbColors, onDone: () -> Unit) {
    val pages = listOf(
        OnboardingPage(
            "Vamos treinar juntos",
            "O treino ensina a sentir o Braille pela vibração, no ritmo da pessoa. Você, pessoa de apoio, acompanha pela tela.",
        ) { Mascot(180.dp, mood = MascotMood.HAPPY) },
        OnboardingPage(
            "Faça junto com a pessoa de apoio",
            "O treino é feito em dupla. Você é a pessoa de apoio: lê a tela e usa os botões quando precisar. A pessoa surdocega sente a vibração e responde na cela e com os gestos.",
        ) { Mascot(170.dp, mood = MascotMood.CALM) },
        OnboardingPage(
            "Forte é ponto, fraco é liso",
            "Cada letra vibra os 6 pontos em ordem, de 1 a 6. Vibração forte: ponto em relevo. Vibração fraca: ponto liso.",
        ) { IntroCell(colors) },
        OnboardingPage(
            "Responda na cela",
            "Depois de sentir, a pessoa marca os pontos e usa os mesmos gestos da conversa: toque confirma, ← apaga, → espaço, ↑ confere e ↓ repete. Acerto e erro têm vibrações diferentes.",
        ) { Mascot(170.dp, mood = MascotMood.FOCUSED) },
        OnboardingPage(
            "Três módulos",
            "Primeiro as letras, depois palavras e, por fim, frases. O progresso fica salvo neste celular.",
        ) { IntroModules(colors) },
    )
    OnboardingPager(
        pages = pages,
        colors = OnboardingColors(
            bg = colors.bg, title = colors.onBg, body = colors.muted,
            dotActive = colors.onBg, dotInactive = colors.border,
            buttonBg = if (colors.dark) colors.dotOn else colors.gesture,
            buttonFg = if (colors.dark) colors.onDotOn else colors.onGesture,
        ),
        finishLabel = "Começar",
        onFinish = onDone,
        onSkip = onDone,
        modifier = Modifier.statusBarsPadding(),
    )
}

@Composable
private fun IntroCell(colors: DbColors) {
    Box(contentAlignment = Alignment.Center) {
        Box(Modifier.size(220.dp).clip(CircleShape).background(colors.surface))
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiniCell(BrailleMapper.brailleTable.getValue('A'), colors, dot = 22.dp)
            Spacer(Modifier.width(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                IntroLegend("forte", true, colors)
                IntroLegend("fraco", false, colors)
            }
        }
    }
}

@Composable
private fun IntroLegend(label: String, strong: Boolean, colors: DbColors) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(if (strong) 34.dp else 16.dp).height(8.dp).clip(CircleShape)
            .background(if (strong) colors.dotOn else colors.dotOffBorder))
        Spacer(Modifier.width(8.dp))
        Text(label, color = colors.muted, style = MaterialTheme.typography.labelLarge.copy(fontSize = 14.sp))
    }
}

@Composable
private fun IntroModules(colors: DbColors) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        TrainingCatalog.modules.forEachIndexed { i, m ->
            Row(
                Modifier
                    .padding(start = (i * 18).dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (i == 0) colors.bg else colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                NumberBadge(m.number, colors, filled = i == 0)
                Spacer(Modifier.width(12.dp))
                Text(m.title, color = colors.onBg, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
            }
        }
    }
}

@Composable
private fun NumberBadge(n: Int, colors: DbColors, filled: Boolean) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(if (filled) colors.dotOn else colors.dotOff),
        contentAlignment = Alignment.Center,
    ) {
        Text("$n", color = if (filled) colors.onDotOn else colors.onBg,
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, fontWeight = FontWeight.Bold))
    }
}

// ───────────────────────── Lista de módulos ─────────────────────────

@Composable
private fun ModuleList(
    colors: DbColors,
    completed: Map<String, Int>,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onIntro: () -> Unit,
) {
    Column(Modifier.fillMaxSize().background(colors.bg).statusBarsPadding().navigationBarsPadding()) {
        DbHeader("Treino", colors, onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            // Boas-vindas com o mascote.
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(colors.surface).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Mascot(84.dp, mood = MascotMood.CURIOUS)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Escolha um módulo", color = colors.onBg,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold))
                    Text("Faça com uma pessoa de apoio. Comece pelas letras: sentir, responder e conferir.", color = colors.muted,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp))
                }
            }
            Spacer(Modifier.height(16.dp))
            TrainingCatalog.modules.forEach { m ->
                ModuleCard(m, completed[m.id] ?: 0, colors) { onOpen(m.id) }
                Spacer(Modifier.height(12.dp))
            }
            TextButton(onClick = onIntro, modifier = Modifier.align(Alignment.CenterHorizontally).heightIn(min = 48.dp)) {
                Text("Ver a introdução de novo", color = colors.accent, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ModuleCard(module: TrainingModule, done: Int, colors: DbColors, onClick: () -> Unit) {
    val total = module.items.size
    val fraction by animateFloatAsState((done.toFloat() / total).coerceIn(0f, 1f), tween(Motion.SLOW, easing = Motion.SmoothOut), label = "progress")
    val status = when {
        done >= total -> "Concluído"
        done > 0 -> "$done de $total"
        else -> "Novo"
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(colors.bg)
            .border(1.dp, colors.border, RoundedCornerShape(24.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .clearAndSetSemantics { contentDescription = "Módulo ${module.number}, ${module.title}. ${module.subtitle} $status." }
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NumberBadge(module.number, colors, filled = done < total)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Módulo ${module.number}", color = colors.muted, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp))
                Text(module.title, color = colors.onBg,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
            }
            if (done >= total) {
                Icon(ContatoIcons.Check, null, tint = colors.online)
            } else {
                Icon(ContatoIcons.ChevronRight, null, tint = colors.muted)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(module.subtitle, color = colors.muted, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp))
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(colors.dotOff)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(CircleShape).background(if (done >= total) colors.online else colors.dotOn))
            }
            Spacer(Modifier.width(10.dp))
            Text(status, color = colors.muted, style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
        }
    }
}

// ───────────────────────── Lição ─────────────────────────

private enum class Phase { FEEL, ANSWER, RESULT, DONE }

@Composable
private fun Lesson(
    session: DeafblindSession,
    module: TrainingModule,
    startAt: Int,
    colors: DbColors,
    onProgress: (Int) -> Unit,
    onBack: () -> Unit,
) {
    var index by rememberSaveable(module.id) { mutableStateOf(startAt) }
    var phase by rememberSaveable(module.id) { mutableStateOf(Phase.FEEL) }
    var correct by rememberSaveable { mutableStateOf(false) }
    var answer by rememberSaveable { mutableStateOf("") }
    var reveal by rememberSaveable { mutableStateOf(false) }
    val points = remember { mutableStateListOf(false, false, false, false, false, false) }
    val item = module.items.getOrElse(index) { module.items.last() }
    val singleLetter = item.length == 1

    fun clearCell() { for (i in points.indices) points[i] = false }

    // Ao mostrar um item novo, ele vibra sozinho.
    LaunchedEffect(index, phase) {
        if (phase == Phase.FEEL) session.playText(item)
    }

    Column(Modifier.fillMaxSize().background(colors.bg).statusBarsPadding().navigationBarsPadding()) {
        DbHeader("Módulo ${module.number}", colors, onBack)
        // Progresso do módulo.
        Row(Modifier.padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            val fraction by animateFloatAsState((index.toFloat() / module.items.size), tween(Motion.SLOW, easing = Motion.SmoothOut), label = "lp")
            Box(Modifier.weight(1f).height(8.dp).clip(CircleShape).background(colors.dotOff)) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(CircleShape).background(colors.dotOn))
            }
            Spacer(Modifier.width(10.dp))
            Text("${(index + 1).coerceAtMost(module.items.size)} de ${module.items.size}", color = colors.muted,
                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp, fontWeight = FontWeight.SemiBold))
        }
        Spacer(Modifier.height(8.dp))

        AnimatedContent(
            targetState = phase,
            transitionSpec = { fadeIn(tween(Motion.FAST)) + scaleIn(tween(Motion.FAST), initialScale = 0.98f) togetherWith fadeOut(tween(Motion.QUICK)) },
            label = "phase",
            modifier = Modifier.weight(1f),
        ) { p ->
            when (p) {
                Phase.FEEL -> FeelStep(item, colors,
                    onFeelAgain = { session.playText(item) },
                    onAnswer = { clearCell(); answer = ""; phase = Phase.ANSWER })
                Phase.ANSWER -> AnswerStep(
                    item = item,
                    singleLetter = singleLetter,
                    points = points,
                    answer = answer,
                    reveal = reveal,
                    colors = colors,
                    onToggle = { i -> points[i] = !points[i]; session.dotFeedback(points[i]) },
                    onConfirmLetter = {
                        val l = BrailleMapper.reverseMap(points.toList())
                        if (l != null) { answer += l; session.commandFeedback(true) } else session.commandFeedback(false)
                        clearCell()
                    },
                    onSpace = { if (answer.isNotEmpty() && !answer.endsWith(' ')) { answer += " "; session.commandFeedback(true) } else session.commandFeedback(false) },
                    onDelete = { if (answer.isNotEmpty()) { answer = answer.dropLast(1); session.commandFeedback(true) } else session.commandFeedback(false) },
                    onReveal = { reveal = !reveal },
                    onFeelAgain = { session.playText(item) },
                    onCheck = {
                        // Pontos marcados sem "Confirmar letra" contam como a última letra.
                        val pending = BrailleMapper.reverseMap(points.toList())?.toString() ?: ""
                        val given = if (singleLetter) pending
                        else (answer + if (points.any { it }) pending else "").trim()
                        correct = given == item
                        session.trainingResult(correct)
                        if (correct) onProgress(index + 1)
                        answer = given
                        phase = Phase.RESULT
                    },
                )
                Phase.RESULT -> ResultStep(
                    correct = correct,
                    item = item,
                    given = answer,
                    colors = colors,
                    onRetry = { clearCell(); answer = ""; phase = Phase.FEEL },
                    onNext = {
                        clearCell(); answer = ""
                        if (index + 1 >= module.items.size) { onProgress(module.items.size); phase = Phase.DONE }
                        else { index++; phase = Phase.FEEL }
                    },
                )
                Phase.DONE -> DoneStep(module, colors, onBack = onBack)
            }
        }
    }
}

@Composable
private fun FeelStep(item: String, colors: DbColors, onFeelAgain: () -> Unit, onAnswer: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(12.dp))
        Text("Sinta", color = colors.muted, style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold))
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(colors.surface).padding(vertical = 28.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(item, color = colors.onBg, textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.displaySmall.copy(
                        fontSize = if (item.length == 1) 72.sp else 36.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp))
                if (item.length == 1) {
                    val p = BrailleMapper.brailleTable.getValue(item[0])
                    Spacer(Modifier.height(16.dp))
                    MiniCell(p, colors, dot = 18.dp)
                    Spacer(Modifier.height(10.dp))
                    Text("Pontos ${pointsLabel(p)}", color = colors.muted, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
                } else {
                    Spacer(Modifier.height(10.dp))
                    Text("${item.replace(" ", "").length} letras", color = colors.muted, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        // Mesmos gestos da conversa: ↓ repete (como "repetir a mensagem"), toque ou ↑ vai responder.
        TrainingGesturePad(
            colors = colors,
            modifier = Modifier.heightIn(min = 104.dp),
            tap = PadGesture("Responder", onAnswer),
            up = PadGesture("Responder", onAnswer),
            down = PadGesture("Repete", onFeelAgain),
        )
        Spacer(Modifier.height(12.dp))
        DbOutlineButton("Sentir de novo", colors, onClick = onFeelAgain)
        Spacer(Modifier.height(10.dp))
        DbButton("Responder na cela", colors, onClick = onAnswer)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AnswerStep(
    item: String,
    singleLetter: Boolean,
    points: List<Boolean>,
    answer: String,
    reveal: Boolean,
    colors: DbColors,
    onToggle: (Int) -> Unit,
    onConfirmLetter: () -> Unit,
    onSpace: () -> Unit,
    onDelete: () -> Unit,
    onReveal: () -> Unit,
    onFeelAgain: () -> Unit,
    onCheck: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Linha de apoio: o que já foi montado e (opcional) a resposta certa.
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(colors.surface).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite }) {
                Text(if (singleLetter) "Marque os pontos da letra" else "Resposta: ${answer.ifEmpty { "—" }}_",
                    color = colors.onBg, style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
                if (reveal) Text("Certo: $item", color = colors.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
            }
            // Texto e chave juntos: o leitor de tela diz "Mostrar resposta, ativado/desativado".
            Row(
                Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .toggleable(value = reveal, role = Role.Switch, onValueChange = { onReveal() })
                    .padding(start = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("mostrar", color = colors.muted, modifier = Modifier.semantics { contentDescription = "Mostrar resposta" },
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp))
                Switch(checked = reveal, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = colors.dotOn))
            }
        }
        Spacer(Modifier.height(10.dp))
        BrailleCell(points, colors, dimmed = false, onToggle = onToggle, modifier = Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        // Os mesmos gestos da conversa. Letra sozinha: o toque já confere.
        val canCheck = if (singleLetter) points.any { it } else answer.isNotBlank() || points.any { it }
        TrainingGesturePad(
            colors = colors,
            modifier = Modifier.heightIn(min = 96.dp),
            tap = if (singleLetter) PadGesture("Confere") { if (canCheck) onCheck() else onConfirmLetter() }
                  else PadGesture("Confirma", onConfirmLetter),
            left = if (singleLetter) null else PadGesture("Apaga", onDelete),
            right = if (singleLetter) null else PadGesture("Espaço", onSpace),
            up = PadGesture("Confere") { if (canCheck) onCheck() else onConfirmLetter() },
            down = PadGesture("Repete", onFeelAgain),
        )
        Spacer(Modifier.height(8.dp))
        if (!singleLetter) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallAction("Apagar", colors, Modifier.weight(1f), onDelete)
                SmallAction("Espaço", colors, Modifier.weight(1f), onSpace)
                SmallAction("Confirmar letra", colors, Modifier.weight(1.4f), onConfirmLetter, primary = true)
            }
            Spacer(Modifier.height(8.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DbOutlineButton("Sentir de novo", colors, Modifier.weight(1f), onClick = onFeelAgain)
            DbButton("Conferir", colors, Modifier.weight(1f), enabled = if (singleLetter) points.any { it } else answer.isNotBlank(), onClick = onCheck)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun SmallAction(text: String, colors: DbColors, modifier: Modifier, onClick: () -> Unit, primary: Boolean = false) {
    Box(
        modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (primary) colors.gesture else colors.surface)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, color = if (primary) colors.onGesture else colors.onBg, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun ResultStep(correct: Boolean, item: String, given: String, colors: DbColors, onRetry: () -> Unit, onNext: () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(20.dp))
        Mascot(160.dp, mood = if (correct) MascotMood.EXCITED else MascotMood.CURIOUS)
        Spacer(Modifier.height(16.dp))
        Text(
            if (correct) "Acertou!" else "Quase!",
            color = colors.onBg, textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading(); liveRegion = LiveRegionMode.Polite },
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (correct) "Era $item." else "Era $item" + (if (given.isNotBlank()) ", a resposta foi $given." else ".") +
                    if (item.length == 1) " Pontos ${pointsLabel(BrailleMapper.brailleTable.getValue(item[0]))}." else "",
            color = colors.muted, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
        )
        Spacer(Modifier.height(28.dp))
        if (!correct) {
            DbOutlineButton("Tentar de novo", colors, onClick = onRetry)
            Spacer(Modifier.height(10.dp))
        }
        DbButton("Próximo", colors, onClick = onNext)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DoneStep(module: TrainingModule, colors: DbColors, onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Mascot(180.dp, mood = MascotMood.EXCITED)
        Spacer(Modifier.height(16.dp))
        Text("Módulo ${module.number} concluído!", color = colors.onBg, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(8.dp))
        Text(
            TrainingCatalog.modules.firstOrNull { it.number == module.number + 1 }?.let { "Próximo passo: ${it.title}." }
                ?: "Todos os módulos feitos. Dá para repetir quando quiser.",
            color = colors.muted, textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 24.sp),
        )
        Spacer(Modifier.height(28.dp))
        DbButton("Voltar aos módulos", colors, onClick = onBack)
    }
}
