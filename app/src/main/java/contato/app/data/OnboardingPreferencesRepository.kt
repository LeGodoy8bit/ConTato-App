package contato.app.data

import android.content.Context
import contato.app.domain.repository.OnboardingRepository

/**
 * Implementação de OnboardingRepository com SharedPreferences.
 * Fica na camada de dados porque depende diretamente do Android.
 *
 * Usa o mesmo arquivo de preferências do UserModePreferencesRepository
 * ("contato_preferences"), com chaves diferentes, então os dois convivem.
 */
class OnboardingPreferencesRepository(
    context: Context
) : OnboardingRepository {

    private val preferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override fun hasSeenSplash(): Boolean {
        return preferences.getBoolean(KEY_SPLASH_SEEN, false)
    }

    override fun markSplashSeen() {
        preferences.edit()
            .putBoolean(KEY_SPLASH_SEEN, true)
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "contato_preferences"
        private const val KEY_SPLASH_SEEN = "splash_seen"
    }
}
