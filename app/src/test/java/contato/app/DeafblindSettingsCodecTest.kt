package contato.app

import contato.app.domain.model.DeafblindSettings
import contato.app.domain.model.DeafblindTheme
import contato.app.domain.model.TextSize
import contato.app.domain.model.VibrationSpeed
import contato.app.domain.service.DeafblindSettingsCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeafblindSettingsCodecTest {

    @Test
    fun idaEVoltaMantemTudo() {
        val s = DeafblindSettings(intensity = 0.65f, timeScale = 1.7f, textSize = TextSize.EXTRA_LARGE,
            theme = DeafblindTheme.HIGH_CONTRAST, showGestureHints = false, displayName = "Ana")
        assertEquals(s, DeafblindSettingsCodec.fromMap(DeafblindSettingsCodec.toMap(s), DeafblindSettings()))
    }

    @Test
    fun valorDesconhecidoMantemOAtual() {
        val atual = DeafblindSettings(timeScale = VibrationSpeed.FAST.timeScale)
        val lido = DeafblindSettingsCodec.fromMap(mapOf("timeScale" to "TURBO", "gestureHints" to "talvez"), atual)
        assertEquals(atual, lido)
    }

    @Test
    fun formatoAntigoEValoresForaDoLimite() {
        val lido = DeafblindSettingsCodec.fromMap(mapOf("intensity" to "SOFT", "speed" to "SLOW"), DeafblindSettings())
        assertEquals(0.55f, lido.intensity)
        assertEquals(1.4f, lido.timeScale)
        val limitado = DeafblindSettingsCodec.fromMap(mapOf("intensity" to "0.01", "timeScale" to "9"), DeafblindSettings())
        assertEquals(DeafblindSettings.MIN_INTENSITY, limitado.intensity)
        assertEquals(DeafblindSettings.MAX_TIME_SCALE, limitado.timeScale)
        assertNull(limitado.speedPreset)
    }
}
