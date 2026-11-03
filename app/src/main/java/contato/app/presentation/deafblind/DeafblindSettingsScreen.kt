package contato.app.presentation.deafblind

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.core.config.HapticConfig
import contato.app.domain.model.DeafblindSettings
import contato.app.domain.model.DeafblindTheme
import contato.app.domain.model.TextSize
import contato.app.domain.model.VibrationIntensity
import contato.app.domain.model.VibrationSpeed
import contato.app.presentation.theme.ContatoIcons
import contato.app.presentation.theme.Motion
import kotlin.math.roundToInt
import androidx.compose.foundation.selection.toggleable

/**
 * Ajustes do Surdocego (feitos com a pessoa de apoio). Tudo vale na hora:
 *  - Conta: entrar com Google para salvar conversas e ajustes na nuvem.
 *  - Nome deste celular: é o que o Comunicador vê na conexão e nas conversas.
 *  - Vibração: atalhos (Suave/Média/Forte, Lenta/Normal/Rápida) ou ajuste personalizado,
 *    com o mínimo e o máximo explicados.
 *  - Tela e conversas.
 */
@Composable
internal fun DeafblindSettingsScreen(session: DeafblindSession, colors: DbColors, onBack: () -> Unit) {
    val s = session.settings
    var editName by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf(s.intensityPreset == null || s.speedPreset == null) }

    Column(Modifier.fillMaxSize().background(colors.bg).statusBarsPadding().navigationBarsPadding()) {
        DbHeader("Ajustes", colors, onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {

            AccountCard(session, colors)

            DbSectionLabel("Este celular", colors)
            DbCard(colors) {
                Row(
                    Modifier.fillMaxWidth().clickable(role = Role.Button, onClickLabel = "Trocar o nome") { editName = true }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Nome na conexão", color = colors.muted, style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
                        Text(session.localName, color = colors.onBg,
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
                        Text("É o nome que o Comunicador vê.", color = colors.muted,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
                    }
                    Icon(ContatoIcons.Edit, contentDescription = null, tint = colors.accent)
                }
            }

            DbSectionLabel("Vibração", colors)
            DbCard(colors) {
                OptionGroup("Força", VibrationIntensity.entries, if (custom) null else s.intensityPreset, { it.label }, colors) {
                    custom = false
                    session.updateSettings(s.copy(intensity = it.factor))
                    session.previewVibration()
                }
                HorizontalDivider(color = colors.border)
                OptionGroup("Velocidade das letras", VibrationSpeed.entries, if (custom) null else s.speedPreset, { it.label }, colors) {
                    custom = false
                    session.updateSettings(s.copy(timeScale = it.timeScale))
                    session.previewVibration()
                }
                HorizontalDivider(color = colors.border)
                SettingSwitch(
                    title = "Ajuste personalizado",
                    subtitle = "Escolha a força e o tempo exatos, dentro dos limites seguros.",
                    checked = custom,
                    colors = colors,
                ) { custom = !custom }
                AnimatedVisibility(
                    visible = custom,
                    enter = expandVertically(tween(Motion.MEDIUM, easing = Motion.SmoothOut)) + fadeIn(tween(Motion.MEDIUM)),
                    exit = shrinkVertically(tween(Motion.FAST, easing = Motion.SmoothOut)) + fadeOut(tween(Motion.QUICK)),
                ) {
                    CustomVibration(session, s, colors)
                }
                DbOutlineButton("Testar vibração", colors, Modifier.padding(16.dp)) { session.previewVibration() }
            }

            DbSectionLabel("Tela", colors)
            DbCard(colors) {
                OptionGroup("Tamanho do texto", TextSize.entries, s.textSize, { it.label }, colors) {
                    session.updateSettings(s.copy(textSize = it))
                }
                HorizontalDivider(color = colors.border)
                OptionGroup("Cores", DeafblindTheme.entries, s.theme, { it.label }, colors) {
                    session.updateSettings(s.copy(theme = it))
                }
                HorizontalDivider(color = colors.border)
                SettingSwitch(
                    title = "Mostrar dicas de gestos",
                    subtitle = "Setas bem clarinhas na área de gestos.",
                    checked = s.showGestureHints,
                    colors = colors,
                ) { session.updateSettings(s.copy(showGestureHints = !s.showGestureHints)) }
            }

            DbSectionLabel("Conversas", colors)
            DbCard(colors) {
                SettingSwitch(
                    title = "Guardar conversas",
                    subtitle = if (session.account.isSignedIn) "Neste celular e na conta, na nuvem." else "Ficam só neste celular.",
                    checked = s.saveHistory,
                    colors = colors,
                ) { session.updateSettings(s.copy(saveHistory = !s.saveHistory)) }
            }
        }
    }

    if (editName) {
        NameDialog(
            current = s.displayName,
            placeholder = session.defaultName,
            onDismiss = { editName = false },
            onSave = { session.updateSettings(s.copy(displayName = it)); editName = false },
        )
    }
}

// ───────────────────────── Conta ─────────────────────────

@Composable
private fun AccountCard(session: DeafblindSession, colors: DbColors) {
    val context = LocalContext.current
    val account = session.account
    val user = account.user
    var confirmSignOut by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(if (user == null) colors.gesture else colors.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(52.dp).clip(CircleShape).background(colors.bg), contentAlignment = Alignment.Center) {
            Icon(ContatoIcons.Cloud, null, tint = colors.accent, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            if (user == null) {
                Text("Você está sem login", color = colors.onGesture,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
                Text(
                    account.error ?: if (account.isConfigured) "A pessoa de apoio entra com Google para salvar conversas e ajustes."
                    else "Login disponível em breve.",
                    color = if (account.error != null) colors.error else colors.onGesture.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp),
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .clip(CircleShape)
                        .background(colors.bg)
                        .border(1.dp, colors.border, CircleShape)
                        .clickable(enabled = account.isConfigured && !account.busy, role = Role.Button) { session.signIn(context) }
                        .heightIn(min = 48.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (account.busy) {
                        CircularProgressIndicator(Modifier.size(18.dp), color = colors.accent, strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("Entrando...", color = colors.onBg, fontWeight = FontWeight.Bold)
                    } else {
                        Text("G", color = Color(0xFF4285F4), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                        Spacer(Modifier.width(8.dp))
                        Text("Continuar com Google", color = colors.onBg, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Text(user.name ?: "Conta conectada", color = colors.onBg,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
                if (user.email != null) {
                    Text(user.email, color = colors.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
                }
                Text("Conversas e ajustes salvos na nuvem", color = colors.online,
                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
            }
        }
        if (user != null) {
            Box(
                Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button, onClickLabel = "Sair da conta") { confirmSignOut = true },
                contentAlignment = Alignment.Center,
            ) {
                Icon(ContatoIcons.LogOut, contentDescription = "Sair da conta", tint = colors.muted)
            }
        }
    }

    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sair da conta?") },
            text = { Text("As conversas e os ajustes continuam neste celular.") },
            confirmButton = {
                TextButton(onClick = { confirmSignOut = false; session.signOut(context) }) { Text("Sair", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancelar") } },
        )
    }
}

// ───────────────────────── Vibração personalizada ─────────────────────────

/**
 * Dois controles deslizantes, cada um com o valor atual, o mínimo e o máximo.
 * A amostra só vibra ao soltar o dedo (senão vibraria sem parar enquanto arrasta).
 */
@Composable
private fun CustomVibration(session: DeafblindSession, s: DeafblindSettings, colors: DbColors) {
    var intensity by remember(s.intensity) { mutableStateOf(s.intensity) }
    var timeScale by remember(s.timeScale) { mutableStateOf(s.timeScale) }
    val activeMs = (HapticConfig.ACTIVE_DURATION * timeScale).roundToInt()
    val inactiveMs = (HapticConfig.INACTIVE_DURATION * timeScale).roundToInt()
    val minActive = (HapticConfig.ACTIVE_DURATION * DeafblindSettings.MIN_TIME_SCALE).roundToInt()
    val maxActive = (HapticConfig.ACTIVE_DURATION * DeafblindSettings.MAX_TIME_SCALE).roundToInt()
    // Uma letra = 6 pontos + pausas; conta média com metade dos pontos em relevo.
    val letterSeconds = ((3 * HapticConfig.ACTIVE_DURATION + 3 * HapticConfig.INACTIVE_DURATION + 6 * HapticConfig.POINT_PAUSE +
            HapticConfig.CHARACTER_PAUSE) * timeScale) / 1000f

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        SliderBlock(
            title = "Força",
            value = "${(intensity * 100).roundToInt()}%",
            min = "${(DeafblindSettings.MIN_INTENSITY * 100).roundToInt()}% (mínimo)",
            max = "100% (máximo)",
            explain = "Abaixo de 30% muitos celulares quase não vibram. 100% é a força máxima do motor.",
            sliderValue = intensity,
            range = DeafblindSettings.MIN_INTENSITY..DeafblindSettings.MAX_INTENSITY,
            colors = colors,
            onChange = { intensity = it },
            onDone = { session.updateSettings(s.copy(intensity = intensity, timeScale = timeScale)); session.previewVibration() },
        )
        Spacer(Modifier.height(16.dp))
        SliderBlock(
            title = "Tempo de cada ponto",
            value = "$activeMs ms",
            min = "$minActive ms (mais rápido)",
            max = "$maxActive ms (mais devagar)",
            explain = "Ponto em relevo: $activeMs ms · ponto liso: $inactiveMs ms. Uma letra leva cerca de " +
                    String.format(java.util.Locale.forLanguageTag("pt-BR"), "%.1f", letterSeconds) + " s.",
            sliderValue = timeScale,
            range = DeafblindSettings.MIN_TIME_SCALE..DeafblindSettings.MAX_TIME_SCALE,
            colors = colors,
            onChange = { timeScale = it },
            onDone = { session.updateSettings(s.copy(intensity = intensity, timeScale = timeScale)); session.previewVibration() },
        )
    }
}

@Composable
private fun SliderBlock(
    title: String,
    value: String,
    min: String,
    max: String,
    explain: String,
    sliderValue: Float,
    range: ClosedFloatingPointRange<Float>,
    colors: DbColors,
    onChange: (Float) -> Unit,
    onDone: () -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(title, color = colors.onBg, modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
            Text(value, color = colors.accent, style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold))
        }
        Slider(
            value = sliderValue,
            onValueChange = onChange,
            onValueChangeFinished = onDone,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = colors.dotOn, activeTrackColor = colors.dotOn, inactiveTrackColor = colors.dotOff,
            ),
            modifier = Modifier.semantics { stateDescription = value },
        )
        Row {
            Text(min, color = colors.muted, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp))
            Text(max, color = colors.muted, style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp))
        }
        Spacer(Modifier.height(6.dp))
        Text(explain, color = colors.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
    }
}

// ───────────────────────── Nome ─────────────────────────

@Composable
private fun NameDialog(current: String, placeholder: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by remember { mutableStateOf(current) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nome deste celular") },
        text = {
            Column {
                Text("Aparece para o Comunicador. Deixe vazio para usar \"$placeholder\".")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= DeafblindSettings.MAX_NAME) text = it },
                    singleLine = true,
                    placeholder = { Text(placeholder) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text.trim()) }) { Text("Salvar", fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
internal fun SettingSwitch(title: String, subtitle: String, checked: Boolean, colors: DbColors, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            // toggleable: o leitor de tela diz "ativado/desativado" (com clickable não dizia).
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.onBg, style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp, fontWeight = FontWeight.Bold))
            Text(subtitle, color = colors.muted, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp))
        }
        Spacer(Modifier.width(12.dp))
        Switch(checked = checked, onCheckedChange = null, colors = SwitchDefaults.colors(checkedTrackColor = colors.dotOn))
    }
}
