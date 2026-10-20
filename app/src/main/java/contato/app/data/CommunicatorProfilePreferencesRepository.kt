package contato.app.data

import android.content.Context
import contato.app.domain.repository.CommunicatorProfileRepository

/** Preferências do Comunicador em SharedPreferences (mesmo arquivo das outras preferências). */
class CommunicatorProfilePreferencesRepository(
    context: Context,
) : CommunicatorProfileRepository {

    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun getDisplayName(): String =
        preferences.getString(KEY_NAME, null)?.takeIf { it.isNotBlank() } ?: DEFAULT_NAME

    override fun saveDisplayName(name: String) {
        preferences.edit().putString(KEY_NAME, name.trim().take(MAX_NAME_LENGTH)).apply()
    }

    override fun isHistoryEnabled(): Boolean = preferences.getBoolean(KEY_HISTORY, false)

    override fun setHistoryEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_HISTORY, enabled).apply()
    }

    override fun isAutoSearchEnabled(): Boolean = preferences.getBoolean(KEY_AUTO_SEARCH, true)

    override fun setAutoSearchEnabled(enabled: Boolean) {
        preferences.edit().putBoolean(KEY_AUTO_SEARCH, enabled).apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "contato_preferences"
        private const val KEY_NAME = "communicator_name"
        private const val KEY_HISTORY = "communicator_history_enabled"
        private const val KEY_AUTO_SEARCH = "communicator_auto_search"
        const val DEFAULT_NAME = "Comunicador"
        const val MAX_NAME_LENGTH = 30
    }
}
