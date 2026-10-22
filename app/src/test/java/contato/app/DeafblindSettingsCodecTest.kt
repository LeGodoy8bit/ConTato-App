package contato.app

import contato.app.domain.model.DeafblindSettings
import contato.app.domain.model.DeafblindTheme
import contato.app.domain.model.TextSize
import contato.app.domain.model.VibrationSpeed
import contato.app.domain.service.DeafblindSettingsCodec
import org.junit.Assert.assertEquals
import org.junit.Test

class DeafblindSettingsCodecTest {

    @Test
    fun idaEVoltaMantemTudo() {
        val s = DeafblindSettings(speed = VibrationSpeed.SLOW, textSize = TextSize.EXTRA_LARGE,
            theme = DeafblindTheme.HIGH_CONTRAST, showGestureHints = false)
        assertEquals(s, DeafblindSettingsCodec.fromMap(DeafblindSettingsCodec.toMap(s), DeafblindSettings()))
    }

    @Test
    fun valorDesconhecidoMantemOAtual() {
        val atual = DeafblindSettings(speed = VibrationSpeed.FAST)
        val lido = DeafblindSettingsCodec.fromMap(mapOf("speed" to "TURBO", "gestureHints" to "talvez"), atual)
        assertEquals(atual, lido)
    }
}
