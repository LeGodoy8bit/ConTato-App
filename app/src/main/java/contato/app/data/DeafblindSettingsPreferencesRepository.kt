package contato.app.data

import android.content.Context
import contato.app.domain.model.DeafblindSettings
import contato.app.domain.repository.DeafblindSettingsRepository

class DeafblindSettingsPreferencesRepository(context: Context) : DeafblindSettingsRepository {

    private val preferences =
        context.applicationContext.getSharedPreferences("contato_deafblind_settings", Context.MODE_PRIVATE)

    override fun load(): DeafblindSettings {
        val d = DeafblindSettings()
        return DeafblindSettings(
            intensity = enumOr(preferences.getString("intensity", null), d.intensity),
            speed = enumOr(preferences.getString("speed", null), d.speed),
            textSize = enumOr(preferences.getString("text_size", null), d.textSize),
            theme = enumOr(preferences.getString("theme", null), d.theme),
            showGestureHints = preferences.getBoolean("gesture_hints", d.showGestureHints),
            saveHistory = preferences.getBoolean("save_history", d.saveHistory),
        )
    }

    override fun save(settings: DeafblindSettings) {
        preferences.edit()
            .putString("intensity", settings.intensity.name)
            .putString("speed", settings.speed.name)
            .putString("text_size", settings.textSize.name)
            .putString("theme", settings.theme.name)
            .putBoolean("gesture_hints", settings.showGestureHints)
            .putBoolean("save_history", settings.saveHistory)
            .apply()
    }

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { runCatching { enumValueOf<E>(it) }.getOrNull() } ?: default

}
