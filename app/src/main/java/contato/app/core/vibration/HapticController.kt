package contato.app.core.vibration

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import contato.app.domain.model.HapticMessage
import contato.app.domain.model.HapticPattern
import contato.app.domain.model.HapticPulse
import contato.app.domain.service.HapticWaveformBuilder
import contato.app.domain.service.SystemHapticWaveformBuilder

class HapticController(context: Context) {

    private val vibrator: Vibrator
    private val waveformBuilder = HapticWaveformBuilder()
    private val systemWaveformBuilder = SystemHapticWaveformBuilder()




    init {
        vibrator =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager

                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
    }


    fun play(pattern: HapticPattern) {
        val waveform = waveformBuilder.buildCharacterWaveform(pattern)
        vibrate(waveform.timings, waveform.amplitudes)
    }

    fun playMessage(message: HapticMessage) {
        val waveform = waveformBuilder.buildMessageWaveform(message)
        vibrate(waveform.timings, waveform.amplitudes)
    }


    /**
     * Reproduz um padrão de vibração de sistema (ver [SystemHapticPatterns]),
     * independente do pipeline de mensagens Braille.
     */
    fun playSystemFeedback(pulses: List<HapticPulse>) {
        val waveform = systemWaveformBuilder.buildSystemWaveform(pulses)
        vibrate(waveform.timings, waveform.amplitudes)
    }

    private fun vibrate(timings: LongArray, amplitudes: IntArray) {
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    fun stop() {
        vibrator.cancel()
    }
}

