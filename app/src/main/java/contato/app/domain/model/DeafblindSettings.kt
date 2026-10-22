package contato.app.domain.model

/** Força da vibração das letras. [factor] multiplica a amplitude de cada ponto. */
enum class VibrationIntensity(val label: String, val factor: Float) {
    SOFT("Suave", 0.55f),
    MEDIUM("Média", 0.8f),
    STRONG("Forte", 1f),
}

/** Ritmo das letras. [timeScale] multiplica cada duração (maior = mais devagar). */
enum class VibrationSpeed(val label: String, val timeScale: Float) {
    SLOW("Lenta", 1.4f),
    NORMAL("Normal", 1f),
    FAST("Rápida", 0.8f),
}

/** Tamanho dos textos na tela do Surdocego (para quem tem baixa visão residual). */
enum class TextSize(val label: String, val scale: Float) {
    NORMAL("Normal", 1f),
    LARGE("Grande", 1.25f),
    EXTRA_LARGE("Muito grande", 1.5f),
}

enum class DeafblindTheme(val label: String) {
    LIGHT("Claro"),
    HIGH_CONTRAST("Alto contraste"),
}

/** Preferências do modo Surdocego. Os padrões seguem o protótipo (claro, vibração forte e normal). */
data class DeafblindSettings(
    val intensity: VibrationIntensity = VibrationIntensity.STRONG,
    val speed: VibrationSpeed = VibrationSpeed.NORMAL,
    val textSize: TextSize = TextSize.NORMAL,
    val theme: DeafblindTheme = DeafblindTheme.LIGHT,
    val showGestureHints: Boolean = true,
    val saveHistory: Boolean = true,
) {
    val tuning: HapticTuning get() = HapticTuning(intensity = intensity.factor, timeScale = speed.timeScale)
}

/** Ajuste aplicado em cima da vibração Braille (ver HapticWaveformTuner). */
data class HapticTuning(val intensity: Float = 1f, val timeScale: Float = 1f) {
    companion object {
        val DEFAULT = HapticTuning()
    }
}
