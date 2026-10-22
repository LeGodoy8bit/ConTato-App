package contato.app.core.vibration

import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import contato.app.domain.model.HapticMessage
import contato.app.domain.model.HapticPattern
import contato.app.domain.model.HapticTuning
import contato.app.domain.model.HapticPulse
import contato.app.domain.model.HapticWaveform
import contato.app.domain.service.HapticWaveformBuilder
import contato.app.domain.service.HapticWaveformTuner
import contato.app.domain.service.SystemHapticWaveformBuilder

class HapticController(context: Context) {

    private val vibrator: Vibrator =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

    private val waveformBuilder = HapticWaveformBuilder()
    private val systemWaveformBuilder = SystemHapticWaveformBuilder()

    /**
     * Força e ritmo escolhidos nos Ajustes do Surdocego. Vale só para as letras Braille;
     * as vibrações de sistema (conectado, erro...) ficam sempre iguais para serem reconhecíveis.
     */
    var tuning: HapticTuning = HapticTuning.DEFAULT

    fun play(pattern: HapticPattern) {
        vibrate(HapticWaveformTuner.tune(waveformBuilder.buildCharacterWaveform(pattern), tuning))
    }

    /**
     * Toca uma mensagem inteira.
     * @return duração total da vibração em milissegundos (0 se não houver o que tocar).
     *         Usado pela tela de termos para saber quando o item terminou de vibrar.
     */
    fun playMessage(message: HapticMessage): Long {
        return vibrate(HapticWaveformTuner.tune(waveformBuilder.buildMessageWaveform(message), tuning))
    }

    /**
     * Reproduz um padrão de vibração de sistema (ver SystemHapticPatterns),
     * independente do pipeline de mensagens Braille.
     */
    fun playSystemFeedback(pulses: List<HapticPulse>) {
        vibrate(systemWaveformBuilder.buildSystemWaveform(pulses))
    }

    /**
     * Uma nova vibração substitui a anterior (comportamento do próprio Vibrator).
     * Waveform vazio (texto só com caracteres não suportados) é ignorado:
     * createWaveform lançaria exceção com listas vazias.
     */
    private fun vibrate(waveform: HapticWaveform): Long {
        if (waveform.timings.isEmpty() || !vibrator.hasVibrator()) return 0L
        val effect = VibrationEffect.createWaveform(waveform.timings, waveform.amplitudes, -1)
        vibrator.vibrate(effect)
        return waveform.timings.sum()
    }

    fun stop() {
        vibrator.cancel()
    }
}
