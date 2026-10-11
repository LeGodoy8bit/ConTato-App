package contato.app.data

import android.content.Context
import contato.app.domain.model.UserMode
import contato.app.domain.repository.UserModeRepository

/**
 * Implementação de UserModeRepository usando SharedPreferences.
 * Fica na camada de persistência porque depende diretamente do Android.
 */
class UserModePreferencesRepository(
    context: Context
) : UserModeRepository {

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getUserMode(): UserMode? {
        val savedName = preferences.getString(KEY_USER_MODE, null) ?: return null
        return UserMode.valueOf(savedName)
    }

    override fun saveUserMode(mode: UserMode) {
        preferences.edit()
            .putString(KEY_USER_MODE, mode.name)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "contato_preferences"
        private const val KEY_USER_MODE = "user_mode"
    }
}
