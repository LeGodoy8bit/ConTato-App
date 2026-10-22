package contato.app.presentation.common

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import contato.app.data.firebase.FirebaseAuthRepository
import contato.app.data.firebase.FirestoreConversationRepository
import contato.app.domain.model.AccountUser
import contato.app.domain.model.CloudProfile
import contato.app.domain.model.Conversation
import contato.app.domain.model.Message
import contato.app.domain.model.SignInResult
import contato.app.domain.model.UserMode
import contato.app.domain.repository.CloudConversationRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Conta na nuvem, usada pelos dois modos (Comunicador e Surdocego).
 * Guarda o estado que a tela mostra (logado, entrando, erro) e repassa as gravações para o Firestore.
 *
 * Se o Firebase ainda não estiver configurado no projeto, [isConfigured] fica false e
 * as telas mostram o botão desligado com "Disponível em breve". Nada quebra.
 */
internal class CloudAccount(context: Context, localMode: UserMode) {

    private val auth = FirebaseAuthRepository(context)
    val isConfigured: Boolean = auth.isConfigured
    private val cloud: CloudConversationRepository? =
        if (isConfigured) FirestoreConversationRepository(localMode) else null

    var user by mutableStateOf(auth.currentUser())
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    val isSignedIn: Boolean get() = user != null

    /**
     * Escopo próprio: o login continua mesmo se a janela que o pediu fechar no meio
     * (senão o Firebase entrava e o app não ficava sabendo). Encerrado em [release].
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Dispara o login sem depender da tela. [onSuccess] roda depois de entrar. */
    fun launchSignIn(activityContext: Context, onSuccess: () -> Unit) {
        scope.launch { if (signIn(activityContext)) onSuccess() }
    }

    fun launchSignOut(activityContext: Context, onDone: () -> Unit = {}) {
        scope.launch { signOut(activityContext); onDone() }
    }

    fun release() = scope.cancel()

    suspend fun signIn(activityContext: Context): Boolean {
        if (busy) return false
        busy = true
        error = null
        val result = auth.signInWithGoogle(activityContext)
        busy = false
        return when (result) {
            is SignInResult.Success -> { user = result.user; true }
            is SignInResult.Failure -> { error = result.reason; false }
            SignInResult.Cancelled -> false
        }
    }

    suspend fun signOut(activityContext: Context) {
        auth.signOut(activityContext)
        user = null
    }

    /** Busca as conversas da nuvem (chamado ao entrar na conta e ao abrir o app já logado). */
    fun pull(onResult: (List<Conversation>) -> Unit) {
        val u = user ?: return
        cloud?.fetchAll(u.uid) { result ->
            result.onSuccess(onResult).onFailure { error = "Não foi possível baixar as conversas da nuvem." }
        }
    }

    fun push(conversation: Conversation, changed: List<Message>) {
        val u = user ?: return
        cloud?.saveConversation(u.uid, conversation, changed)
    }

    fun saveProfile(displayName: String, preferences: Map<String, String>) {
        val u = user ?: return
        cloud?.saveProfile(u.uid, CloudProfile(displayName, preferences))
    }

    /** Ao entrar: traz o perfil salvo (se houver) para aplicar neste celular. */
    fun fetchProfile(onResult: (CloudProfile?) -> Unit) {
        val u = user ?: return
        val repo = cloud ?: return
        repo.fetchProfile(u.uid) { result -> onResult(result.getOrNull()) }
    }

    fun deleteCloudData(onDone: (Boolean) -> Unit) {
        val u = user ?: return onDone(true)
        val repo = cloud ?: return onDone(true)
        repo.deleteAll(u.uid) { result -> onDone(result.isSuccess) }
    }
}
