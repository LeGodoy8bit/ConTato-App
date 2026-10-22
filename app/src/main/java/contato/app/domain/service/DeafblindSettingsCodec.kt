package contato.app.domain.service

import contato.app.domain.model.DeafblindSettings
import contato.app.domain.model.VibrationIntensity
import contato.app.domain.model.VibrationSpeed

/**
 * Converte as preferências do Surdocego para texto (aparelho e nuvem) e de volta.
 * Chave ou valor desconhecido cai no valor atual, sem erro. Aceita também o formato antigo
 * ("STRONG", "SLOW"...), de antes do ajuste personalizado.
 */
object DeafblindSettingsCodec {

    fun toMap(s: DeafblindSettings): Map<String, String> = mapOf(
        "intensity" to s.intensity.toString(),
        "timeScale" to s.timeScale.toString(),
        "textSize" to s.textSize.name,
        "theme" to s.theme.name,
        "gestureHints" to s.showGestureHints.toString(),
        "saveHistory" to s.saveHistory.toString(),
        "displayName" to s.displayName,
    )

    fun fromMap(map: Map<String, String>, current: DeafblindSettings): DeafblindSettings = DeafblindSettings(
        intensity = map["intensity"]?.let(::parseIntensity) ?: current.intensity,
        timeScale = (map["timeScale"]?.toFloatOrNull() ?: map["speed"]?.let(::legacySpeed)) ?: current.timeScale,
        textSize = enumOr(map["textSize"], current.textSize),
        theme = enumOr(map["theme"], current.theme),
        showGestureHints = map["gestureHints"]?.toBooleanStrictOrNull() ?: current.showGestureHints,
        saveHistory = map["saveHistory"]?.toBooleanStrictOrNull() ?: current.saveHistory,
        displayName = map["displayName"] ?: current.displayName,
    ).normalized()

    private fun parseIntensity(v: String): Float? =
        v.toFloatOrNull() ?: VibrationIntensity.entries.firstOrNull { it.name == v }?.factor

    private fun legacySpeed(v: String): Float? = VibrationSpeed.entries.firstOrNull { it.name == v }?.timeScale

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
}
