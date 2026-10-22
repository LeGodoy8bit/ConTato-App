package contato.app.data

import android.content.Context
import contato.app.domain.model.DeafblindSettings
import contato.app.domain.repository.DeafblindSettingsRepository
import contato.app.domain.service.DeafblindSettingsCodec

/** Guarda as preferências do Surdocego como texto, com o mesmo formato usado na nuvem. */
class DeafblindSettingsPreferencesRepository(context: Context) : DeafblindSettingsRepository {

    private val preferences =
        context.applicationContext.getSharedPreferences("contato_deafblind_settings", Context.MODE_PRIVATE)

    override fun load(): DeafblindSettings {
        val all = preferences.all
        val map = all.mapNotNull { (k, v) -> (v as? String)?.let { k to it } }.toMap().toMutableMap()
        // Chaves da versão anterior (antes do ajuste personalizado), para ninguém perder o que escolheu.
        (all["text_size"] as? String)?.let { map.putIfAbsent("textSize", it) }
        (all["gesture_hints"] as? Boolean)?.let { map.putIfAbsent("gestureHints", it.toString()) }
        (all["save_history"] as? Boolean)?.let { map.putIfAbsent("saveHistory", it.toString()) }
        return DeafblindSettingsCodec.fromMap(map, DeafblindSettings())
    }

    override fun save(settings: DeafblindSettings) {
        val editor = preferences.edit().clear()
        DeafblindSettingsCodec.toMap(settings).forEach { (k, v) -> editor.putString(k, v) }
        editor.apply()
    }
}
