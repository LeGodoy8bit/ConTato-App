package contato.app.domain.repository

import contato.app.domain.model.UserMode

/**
 * Contrato para salvar e recuperar o modo de usuário escolhido.
 * O domínio conhece apenas ESTA interface — nunca a implementação real
 * (SharedPreferences, banco de dados, etc.), que fica na camada de dados.
 */

interface UserModeRepository {

    fun getUserMode(): UserMode?
    fun saveUserMode(mode: UserMode)

}