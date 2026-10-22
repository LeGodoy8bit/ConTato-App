package contato.app

import contato.app.domain.model.HapticTuning
import contato.app.domain.model.HapticWaveform
import contato.app.domain.service.HapticWaveformTuner
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertSame
import org.junit.Test

class HapticWaveformTunerTest {

    private val base = HapticWaveform(longArrayOf(250, 80, 140, 80), intArrayOf(255, 0, 100, 0))

    @Test
    fun padraoNaoMudaNada() {
        assertSame(base, HapticWaveformTuner.tune(base, HapticTuning.DEFAULT))
    }

    @Test
    fun lentoAlongaTudoEMantemPausas() {
        val tuned = HapticWaveformTuner.tune(base, HapticTuning(intensity = 1f, timeScale = 1.4f))
        assertArrayEquals(longArrayOf(350, 112, 196, 112), tuned.timings)
        assertArrayEquals(intArrayOf(255, 0, 100, 0), tuned.amplitudes)
    }

    @Test
    fun suaveReduzForcaSemZerarPontoLiso() {
        val tuned = HapticWaveformTuner.tune(base, HapticTuning(intensity = 0.005f, timeScale = 1f))
        assertArrayEquals(intArrayOf(1, 0, 1, 0), tuned.amplitudes)
    }
}
