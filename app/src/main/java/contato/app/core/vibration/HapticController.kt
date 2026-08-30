package contato.app.core.vibration


import android.content.Context
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import contato.app.core.config.HapticConfig
import contato.app.domain.model.HapticPattern

class HapticController(context: Context) {

    private val vibrator: Vibrator //objeto responsável por controlar a vibração

    init {
        vibrator =
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) { //retorna o número da API do Android que está rodando.
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager //serviço responsável pelo gerenciamento dos vibradores.

                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION") // usando essa API antiga de propósito porque preciso suportar Android 11
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

        // "Crie um controlador de vibração que recebe acesso ao Android. Quando ele for criado, verifique a versão do Android.
        // Se for Android 12 ou superior, obtenha o VibratorManager e dele pegue o vibrador padrão. Se for Android 11, obtenha diretamente o Vibrator,
        // mesmo sendo uma API considerada antiga, porque precisamos manter a compatibilidade."
    }

    fun play(pattern: HapticPattern) {

        val timings = mutableListOf<Long>() //quanto tempo cada etapa da sequência vai durar.
        val amplitudes = mutableListOf<Int>() //

        for (event in pattern.events) {  //Crie uma vibração única com determinada duração e amplitude

            timings.add(event.duration) //para cada ponto ele adiciona
            amplitudes.add(event.amplitude)

            timings.add(HapticConfig.POINT_PAUSE)
            amplitudes.add(0)

            val effect = VibrationEffect.createWaveform(
                timings.toLongArray(),
                amplitudes.toIntArray(),
                -1 //Não repetir a sequência

            )

            vibrator.vibrate(effect)
        }

    }

}