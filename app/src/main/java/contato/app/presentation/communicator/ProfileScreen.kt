package contato.app.presentation.communicator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import contato.app.data.CommunicatorProfilePreferencesRepository
import contato.app.domain.model.UserMode
import contato.app.presentation.theme.ContatoIcons
import contato.app.presentation.theme.motionReduced

/**
 * Perfil: foto (escolhida na galeria, fica só neste celular), nome e "Sobre o ConTato".
 * O pareamento agora fica na aba Conexão; preferências e conta, em Ajustes.
 */
@Composable
internal fun ProfileTab(session: CommunicatorSession, c: CommColors, onOpenInfo: (InfoTab) -> Unit) {
    var editingName by remember { mutableStateOf(false) }
    var photoMenu by remember { mutableStateOf(false) }
    // Seletor de fotos do Android: não precisa de permissão de armazenamento.
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) session.setPhoto(uri)
    }
    fun pickPhoto() = picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp),
    ) {
        ScreenHeading("Perfil", c, Modifier.padding(start = 4.dp, top = 16.dp))
        Spacer(Modifier.height(16.dp))

        // ── Identidade: foto grande no centro (referência de perfil) ──
        CommCard(c) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box {
                    Box(
                        Modifier
                            .size(112.dp)
                            .border(3.dp, c.sent, CircleShape)
                            .padding(5.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "Trocar foto") {
                                if (session.photo == null) pickPhoto() else photoMenu = true
                            }
                            .semantics { contentDescription = "Foto de perfil" },
                    ) {
                        Avatar(session.displayName, 102.dp, c, photo = session.photo)
                    }
                    Box(
                        Modifier
                            .align(Alignment.BottomEnd)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(c.sent)
                            .border(3.dp, c.surface, CircleShape)
                            .clickable(role = Role.Button, onClickLabel = "Escolher foto") { pickPhoto() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(ContatoIcons.PhotoCamera, contentDescription = "Escolher foto", tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(session.displayName, color = c.onSurface,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold))
                    IconButton(onClick = { editingName = true }) {
                        Icon(ContatoIcons.Edit, contentDescription = "Editar nome", tint = c.accent, modifier = Modifier.size(20.dp))
                    }
                }
                Text("Comunicador · aparece assim no celular tátil", color = c.muted,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Stat("${session.conversations.size}", "conversas", c)
                    Stat("${session.conversations.sumOf { conv -> conv.messages.count { it.sender == UserMode.COMMUNICATOR } }}", "enviadas", c)
                    Stat(if (session.account.isSignedIn) "Nuvem" else "Aparelho", "salvas em", c)
                }
            }
        }

        // ── Sobre ──
        SectionLabel("Sobre o ConTato", c)
        CommCard(c) {
            LinkRow(ContatoIcons.Info, "Como funciona", "Passo a passo, do texto à vibração.", c) { onOpenInfo(InfoTab.HOW) }
            HorizontalDivider(color = c.border, modifier = Modifier.padding(start = 66.dp))
            LinkRow(ContatoIcons.Smartphone, "Matriz Braille", "As 26 letras. Toque numa para sentir.", c) { onOpenInfo(InfoTab.MATRIX) }
        }
    }

    if (editingName) {
        NameDialog(
            current = session.displayName,
            onDismiss = { editingName = false },
            onSave = { session.rename(it); editingName = false },
        )
    }
    if (photoMenu) {
        AlertDialog(
            onDismissRequest = { photoMenu = false },
            title = { Text("Foto de perfil") },
            text = { Text("A foto fica só neste celular.") },
            confirmButton = { TextButton(onClick = { photoMenu = false; pickPhoto() }) { Text("Trocar foto", fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { photoMenu = false; session.removePhoto() }) { Text("Remover", color = c.danger) } },
        )
    }
}

@Composable
private fun Stat(value: String, label: String, c: CommColors) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) {}) {
        Text(value, color = c.onSurface, style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold))
        Text(label, color = c.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp))
    }
}

@Composable
internal fun LinkRow(icon: ImageVector, title: String, subtitle: String?, c: CommColors, tint: Color = c.accent, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .heightIn(min = 60.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = if (tint == c.accent) c.onSurface else tint,
                style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold))
            if (subtitle != null) {
                Text(subtitle, color = c.muted, style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, lineHeight = 18.sp))
            }
        }
        Icon(ContatoIcons.ChevronRight, null, tint = c.muted)
    }
}

@Composable
private fun NameDialog(current: String, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var value by remember { mutableStateOf(current) }
    val max = CommunicatorProfilePreferencesRepository.MAX_NAME_LENGTH
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seu nome") },
        text = {
            Column {
                Text("Aparece no celular tátil quando vocês se conectam.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = value,
                    onValueChange = { if (it.length <= max) value = it },
                    label = { Text("Nome") },
                    supportingText = { Text("${value.length}/$max") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value) }, enabled = value.isNotBlank()) { Text("Salvar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
