package contato.app.core.vibration

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import contato.app.domain.model.HapticMessage
import contato.app.domain.model.HapticPattern
import contato.app.domain.service.HapticWaveformBuilder

class HapticController(context: Context) {

    private val vibrator: Vibrator
    private val waveformBuilder = HapticWaveformBuilder()

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

    private fun vibrate(timings: LongArray, amplitudes: IntArray) {
        val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
        vibrator.vibrate(effect)
    }

    fun stop() {
        vibrator.cancel()
    }
}

