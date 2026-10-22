package contato.app.domain.service

import contato.app.domain.model.DeafblindSettings

/**
 * Converte as preferências do Surdocego para texto (nuvem) e de volta.
 * Chave ou valor desconhecido (ex.: versão mais nova do app) cai no valor atual, sem erro.
 */
object DeafblindSettingsCodec {

    fun toMap(s: DeafblindSettings): Map<String, String> = mapOf(
        "intensity" to s.intensity.name,
        "speed" to s.speed.name,
        "textSize" to s.textSize.name,
        "theme" to s.theme.name,
        "gestureHints" to s.showGestureHints.toString(),
        "saveHistory" to s.saveHistory.toString(),
    )

    fun fromMap(map: Map<String, String>, current: DeafblindSettings): DeafblindSettings = DeafblindSettings(
        intensity = enumOr(map["intensity"], current.intensity),
        speed = enumOr(map["speed"], current.speed),
        textSize = enumOr(map["textSize"], current.textSize),
        theme = enumOr(map["theme"], current.theme),
        showGestureHints = map["gestureHints"]?.toBooleanStrictOrNull() ?: current.showGestureHints,
        saveHistory = map["saveHistory"]?.toBooleanStrictOrNull() ?: current.saveHistory,
    )

    private inline fun <reified E : Enum<E>> enumOr(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
}
