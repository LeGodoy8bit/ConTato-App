package contato.app.data

import android.content.Context
import contato.app.domain.repository.TrainingProgressRepository

class TrainingProgressPreferencesRepository(context: Context) : TrainingProgressRepository {

    private val preferences =
        context.applicationContext.getSharedPreferences("contato_training", Context.MODE_PRIVATE)

    override fun completed(moduleId: String): Int = preferences.getInt("done_$moduleId", 0)

    override fun setCompleted(moduleId: String, count: Int) {
        preferences.edit().putInt("done_$moduleId", count).apply()
    }

    override fun introSeen(): Boolean = preferences.getBoolean("intro_seen_v2", false)

    override fun setIntroSeen() {
        preferences.edit().putBoolean("intro_seen_v2", true).apply()
    }
}
