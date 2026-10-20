package contato.app.presentation

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import contato.app.core.connection.NearbyPermissions
import contato.app.data.CommunicatorProfilePreferencesRepository
import contato.app.data.PreferencesConversationArchive
import contato.app.presentation.communicator.CommunicatorApp
import contato.app.presentation.communicator.CommunicatorSession
import contato.app.presentation.theme.ContatoTheme

/**
 * Tela do Comunicador, agora em Compose (substitui activity_communicator.xml + MessageAdapter).
 *
 * Fluxo: permissões -> conexão (busca automática) -> conversa / perfil.
 * Toda a lógica fica em CommunicatorSession; esta Activity só cria a sessão,
 * desenha a interface e encerra a conexão no fim.
 */
class CommunicatorActivity : ComponentActivity() {

    private lateinit var session: CommunicatorSession

    // Estado observado pelo Compose: muda quando a pessoa permite (no diálogo ou nas configurações).
    private var permissionsGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        session = CommunicatorSession(
            context = applicationContext,
            profile = CommunicatorProfilePreferencesRepository(this),
            archive = PreferencesConversationArchive(this),
        )

        permissionsGranted = hasAllPermissions()

        setContent {
            ContatoTheme {
                CommunicatorApp(
                    session = session,
                    permissionsGranted = permissionsGranted,
                    onPermissionsResult = { permissionsGranted = hasAllPermissions() },
                    onOpenAppSettings = ::openAppSettings,
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        permissionsGranted = hasAllPermissions()
    }

    override fun onDestroy() {
        session.release()
        super.onDestroy()
    }

    private fun hasAllPermissions(): Boolean =
        NearbyPermissions.getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    /** Quando a pessoa negou de vez, só dá para liberar pelas configurações do Android. */
    private fun openAppSettings() {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
        )
    }
}
