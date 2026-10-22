package contato.app.domain.model

/** Conta logada (Firebase). Só o necessário para a tela: nada de token aqui. */
data class AccountUser(
    val uid: String,
    val name: String?,
    val email: String?,
)

/** Resultado do "Continuar com Google". */
sealed interface SignInResult {
    data class Success(val user: AccountUser) : SignInResult
    /** A pessoa fechou a janela do Google: não é erro, não mostra aviso. */
    data object Cancelled : SignInResult
    data class Failure(val reason: String) : SignInResult
}

/**
 * Perfil de um modo na nuvem: o nome e as preferências daquele celular
 * (Comunicador: guardar conversas, busca automática; Surdocego: vibração, texto, cores...).
 * Valores sempre como texto, para o modelo ser o mesmo nos dois modos.
 */
data class CloudProfile(
    val displayName: String,
    val preferences: Map<String, String>,
)
