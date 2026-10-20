package contato.app.presentation.deafblind

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.domain.model.DeafblindTheme
import contato.app.domain.model.TextSize
import contato.app.domain.model.VibrationIntensity
import contato.app.domain.model.VibrationSpeed

/**
 * Ajustes do Surdocego. Tudo vale na hora:
 *  - Vibração: força e ritmo das letras (ao mudar, o celular vibra a letra A para comparar).
 *  - Tela: tamanho do texto, tema (claro do protótipo ou alto contraste) e dicas de gestos.
 *  - Conversas: guardar o histórico neste aparelho.
 */
@Composable
internal fun DeafblindSettingsScreen(session: DeafblindSession, colors: DbColors, onBack: () -> Unit) {
    val s = session.settings
    Column(Modifier.fillMaxSize().background(colors.bg).statusBarsPadding().navigationBarsPadding()) {
        DbHeader("Ajustes", colors, onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            DbSectionLabel("Vibração", colors)
            DbCard(colors) {
                OptionGroup("Força", VibrationIntensity.entries, s.intensity, { it.label }, colors) {
                    session.updateSettings(s.copy(intensity = it))
                }
                HorizontalDivider(color = colors.border)
                OptionGroup("Velocidade das letras", VibrationSpeed.entries, s.speed, { it.label }, colors) {
                    session.updateSettings(s.copy(speed = it))
                }
                DbOutlineButton("Testar vibração", colors, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                    session.playLetter('A')
                }
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
                    subtitle = "Setas e nomes dos comandos na área de gestos.",
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
}

@Composable
internal fun SettingSwitch(title: String, subtitle: String, checked: Boolean, colors: DbColors, onToggle: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch, onClick = onToggle)
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
