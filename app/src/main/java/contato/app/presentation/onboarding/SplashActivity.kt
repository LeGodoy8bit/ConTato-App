package contato.app.presentation.onboarding

import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import androidx.compose.runtime.remember
import contato.app.core.config.HapticConfig
import contato.app.data.OnboardingPreferencesRepository
import contato.app.domain.model.SystemFeedbackEvent
import contato.app.domain.service.SplashModeService
import contato.app.presentation.ModeSelectionActivity
import contato.app.presentation.theme.ContatoTheme

/**
 * Activity inicial (LAUNCHER) do app.
 * - 1ª abertura: splash FULL; só é marcada como vista quando termina.
 * - Aberturas seguintes: splash SHORT.
 * No fim, abre a seleção de modo e fecha a si mesma.
 */
class SplashActivity : ComponentActivity() {

    private companion object {
        // true = splash nova (cela Braille); false = splash anterior (cursor e bolinhas).
        const val USE_BRAILLE_VERSION = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Fundo da JANELA igual ao fundo da splash (branco puro no claro). Sem isso, o
        // fundo do tema do app aparece por trás/antes da animação e deixa a tela "acinzentada".
        enableEdgeToEdge()
        val night = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawable(
            ColorDrawable((if (night) DarkPalette else LightPalette).background.toArgb())
        )

        val service = SplashModeService(OnboardingPreferencesRepository(applicationContext))

        setContent {
            ContatoTheme {
                // remember: o modo é decidido UMA vez, antes da animação começar.
                val mode = remember { service.resolveSplashMode() }

                val onFinished = {
                    service.markSplashAsSeen()
                    startActivity(Intent(this, ModeSelectionActivity::class.java))
                    finish()
                    @Suppress("DEPRECATION")
                    overridePendingTransition(0, 0)
                }

                if (USE_BRAILLE_VERSION) {
                    SplashScreenBraille(mode = mode, onFeedback = ::vibrate, onFinished = onFinished)
                } else {
                    SplashScreen(mode = mode, onFeedback = ::vibrate, onFinished = onFinished)
                }
            }
        }
    }

    // Vibração direta com as constantes do HapticConfig. Se você já tem uma classe
    // que toca SystemFeedbackEvent, troque o corpo desta função por uma chamada a ela.
    private fun vibrate(event: SystemFeedbackEvent) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return

        val effect = when (event) {
            SystemFeedbackEvent.SPLASH_TYPING_TICK ->
                VibrationEffect.createOneShot(
                    HapticConfig.SPLASH_TICK_DURATION,
                    HapticConfig.SPLASH_TICK_AMPLITUDE,
                )
            SystemFeedbackEvent.SPLASH_SYMBOL_FORMED ->
                VibrationEffect.createOneShot(
                    HapticConfig.SPLASH_FORMED_DURATION,
                    VibrationEffect.DEFAULT_AMPLITUDE,
                )
            else -> return
        }
        vibrator.vibrate(effect)
    }
}