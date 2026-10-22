package contato.app.data.firebase

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import contato.app.domain.model.AccountUser
import contato.app.domain.model.SignInResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Login com Google (Credential Manager) + Firebase Authentication.
 *
 * Segurança:
 *  - o app nunca vê a senha do Google; recebe só um ID token assinado, que o Firebase confere;
 *  - nenhum token é guardado pelo app (o Firebase guarda a sessão de forma segura);
 *  - [isConfigured] evita travar o app se o google-services.json ainda não foi adicionado.
 */
class FirebaseAuthRepository(context: Context) {

    private val appContext = context.applicationContext

    /** true quando o google-services.json está no projeto e o login Google foi ativado no console. */
    val isConfigured: Boolean
        get() = FirebaseApp.getApps(appContext).isNotEmpty() && webClientId() != null

    fun currentUser(): AccountUser? =
        if (!isConfigured) null else FirebaseAuth.getInstance().currentUser?.toAccount()

    /** Precisa do contexto da Activity: a janela do Google abre por cima dela. */
    suspend fun signInWithGoogle(activityContext: Context): SignInResult {
        val clientId = webClientId()
        if (!isConfigured || clientId == null) {
            return SignInResult.Failure("O login ainda não foi configurado neste aplicativo.")
        }
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
            .build()

        return try {
            val credential = CredentialManager.create(activityContext).getCredential(activityContext, request).credential
            if (credential !is CustomCredential ||
                credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                return SignInResult.Failure("Não foi possível usar esta conta.")
            }
            val idToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
            val result = FirebaseAuth.getInstance()
                .signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .await()
            val user = result.user ?: return SignInResult.Failure("Não foi possível entrar.")
            SignInResult.Success(user.toAccount())
        } catch (e: CancellationException) {
            throw e // corrotina cancelada: não é erro de login
        } catch (e: GetCredentialCancellationException) {
            SignInResult.Cancelled
        } catch (e: NoCredentialException) {
            SignInResult.Failure("Nenhuma conta Google neste celular. Adicione uma nas configurações do Android.")
        } catch (e: GoogleIdTokenParsingException) {
            SignInResult.Failure("A resposta do Google veio inválida. Tente de novo.")
        } catch (e: GetCredentialException) {
            SignInResult.Failure("Não foi possível abrir o login do Google.")
        } catch (e: Exception) {
            SignInResult.Failure("Não foi possível entrar. Confira a internet e tente de novo.")
        }
    }

    /** Sai da conta e limpa a escolha de conta (na próxima vez o Google pergunta de novo). */
    suspend fun signOut(activityContext: Context) {
        if (!isConfigured) return
        FirebaseAuth.getInstance().signOut()
        runCatching {
            CredentialManager.create(activityContext).clearCredentialState(ClearCredentialStateRequest())
        }
    }

    /**
     * O plugin google-services cria a string "default_web_client_id" a partir do google-services.json
     * (depois que o login Google é ativado no console). Lida por nome para o app compilar mesmo sem ela.
     */
    private fun webClientId(): String? {
        val id = appContext.resources.getIdentifier("default_web_client_id", "string", appContext.packageName)
        return if (id == 0) null else appContext.getString(id).takeIf { it.isNotBlank() }
    }

    private fun FirebaseUser.toAccount() = AccountUser(uid = uid, name = displayName, email = email)
}

/** Espera uma Task do Google Play Services sem precisar da biblioteca kotlinx-coroutines-play-services. */
internal suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
