package contato.app.presentation

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.WindowManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import contato.app.core.connection.NearbyPermissions
import contato.app.presentation.deafblind.DeafblindApp
import contato.app.presentation.deafblind.DeafblindSession
import contato.app.presentation.theme.ContatoTheme

/**
 * Tela do Surdocego, agora em Compose (substitui activity_deafblind.xml).
 * A lógica fica em DeafblindSession; aqui só ciclo de vida e permissões.
 */
class DeafblindActivity : ComponentActivity() {

    private lateinit var session: DeafblindSession
    private var permissionsGranted by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A tela não apaga sozinha: se apagasse, o onPause encerraria a conexão no meio da conversa.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        session = DeafblindSession(applicationContext)
        permissionsGranted = hasAllPermissions()

        setContent {
            ContatoTheme(darkTheme = true) {
                DeafblindApp(
                    session = session,
                    permissionsGranted = permissionsGranted,
                    onPermissionsResult = { permissionsGranted = hasAllPermissions() },
                    onOpenAppSettings = ::openAppSettings,
                )
            }
        }
    }

    /** Volta a procurar se a conexão caiu com o app em segundo plano. */
    override fun onResume() {
        super.onResume()
        permissionsGranted = hasAllPermissions()
        session.setActive(true)
    }

    /** Para a vibração e encerra a conexão quando o app sai de foco. */
    override fun onPause() {
        super.onPause()
        session.setActive(false)
    }

    override fun onDestroy() {
        session.release()
        super.onDestroy()
    }

    private fun hasAllPermissions(): Boolean =
        NearbyPermissions.getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
    }
}
