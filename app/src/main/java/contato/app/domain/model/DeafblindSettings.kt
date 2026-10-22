package contato.app.domain.model

/** Atalhos de força (os botões "Suave / Média / Forte"). O valor real fica em [DeafblindSettings.intensity]. */
enum class VibrationIntensity(val label: String, val factor: Float) {
    SOFT("Suave", 0.55f),
    MEDIUM("Média", 0.8f),
    STRONG("Forte", 1f),
}

/** Atalhos de ritmo. [timeScale] multiplica cada duração (maior = mais devagar). */
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

/**
 * Preferências do modo Surdocego.
 *
 * [intensity] e [timeScale] são livres dentro dos limites abaixo (ajuste personalizado);
 * os botões de atalho só escolhem valores prontos.
 * [displayName] = como este celular aparece para o Comunicador (vazio = nome padrão).
 */
data class DeafblindSettings(
    val intensity: Float = VibrationIntensity.STRONG.factor,
    val timeScale: Float = VibrationSpeed.NORMAL.timeScale,
    val textSize: TextSize = TextSize.NORMAL,
    val theme: DeafblindTheme = DeafblindTheme.LIGHT,
    val showGestureHints: Boolean = true,
    val saveHistory: Boolean = true,
    val displayName: String = "",
) {
    val tuning: HapticTuning get() = HapticTuning(intensity = intensity, timeScale = timeScale)

    /** Atalho que bate com o valor atual (null = personalizado). */
    val intensityPreset: VibrationIntensity? get() = VibrationIntensity.entries.firstOrNull { near(it.factor, intensity) }
    val speedPreset: VibrationSpeed? get() = VibrationSpeed.entries.firstOrNull { near(it.timeScale, timeScale) }

    fun normalized(): DeafblindSettings = copy(
        intensity = intensity.coerceIn(MIN_INTENSITY, MAX_INTENSITY),
        timeScale = timeScale.coerceIn(MIN_TIME_SCALE, MAX_TIME_SCALE),
        displayName = displayName.trim().take(MAX_NAME),
    )

    companion object {
        /** Abaixo de 30% muitos motores de vibração quase não são sentidos. */
        const val MIN_INTENSITY = 0.3f
        const val MAX_INTENSITY = 1f
        /** 0,6× = ponto em relevo de 150 ms; 2× = 500 ms. */
        const val MIN_TIME_SCALE = 0.6f
        const val MAX_TIME_SCALE = 2f
        const val MAX_NAME = 30

        private fun near(a: Float, b: Float) = kotlin.math.abs(a - b) < 0.02f
    }
}

/** Ajuste aplicado em cima da vibração Braille (ver HapticWaveformTuner). */
data class HapticTuning(val intensity: Float = 1f, val timeScale: Float = 1f) {
    companion object {
        val DEFAULT = HapticTuning()
    }
}
