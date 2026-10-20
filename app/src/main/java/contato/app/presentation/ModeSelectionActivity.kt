package contato.app.presentation

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import contato.app.core.config.HapticConfig
import contato.app.core.vibration.HapticMessagePlaybackService
import contato.app.data.UserModePreferencesRepository
import contato.app.domain.model.UserMode
import contato.app.domain.repository.UserModeRepository
import contato.app.presentation.onboarding.HapticKind
import contato.app.presentation.onboarding.ModeFlow
import contato.app.presentation.onboarding.TermsAcceptanceStore
import contato.app.presentation.theme.ContatoBlue
import contato.app.presentation.theme.ContatoDarkSurface
import contato.app.presentation.theme.ContatoTheme

/**
 * Seleção de modo, em Compose (substitui o layout XML activity_mode_selection).
 *
 * Fluxo: escolher o modo -> aceitar os termos -> só então o modo é salvo e a tela do modo abre.
 * Se o modo já foi escolhido e os termos desta versão já foram aceitos, pula direto.
 */
class ModeSelectionActivity : ComponentActivity() {

    private lateinit var userModeRepository: UserModeRepository

    // Vibra o resumo dos termos em Braille (tela de termos do Surdocego).
    private val hapticPlayback by lazy { HapticMessagePlaybackService(this) }

    // Até quando o item do resumo que está tocando vai vibrar (relógio do sistema).
    // Serve para o onPause cancelar SÓ o resumo, e nunca a vibração de aceite.
    private var summaryEndsAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        userModeRepository = UserModePreferencesRepository(this)
        val termsStore = TermsAcceptanceStore(this)

        val existingMode = userModeRepository.getUserMode()
        if (existingMode != null && termsStore.hasAccepted(existingMode)) {
            navigateToMode(existingMode)
            return
        }

        // Fundo da janela igual ao da tela, para não piscar entre a splash e esta tela.
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawable(ColorDrawable((if (night) ContatoDarkSurface else ContatoBlue).toArgb()))

        setContent {
            ContatoTheme {
                ModeFlow(
                    startMode = existingMode,
                    onHaptic = ::vibrate,
                    onPlayHapticText = ::playSummaryItem,
                    onStopHaptic = ::stopSummary,
                    onAccepted = { mode ->
                        termsStore.markAccepted(mode)
                        userModeRepository.saveUserMode(mode)
                        navigateToMode(mode)
                    },
                )
            }
        }
    }

    /**
     * Antes, o onPause chamava stop() sempre. Como aceitar os termos abre outra tela
     * (e isso dispara o onPause), a vibração de confirmação era cortada na hora:
     * por isso "2 toques" parecia não vibrar.
     */
    override fun onPause() {
        super.onPause()
        if (SystemClock.elapsedRealtime() < summaryEndsAt) stopSummary()
    }

    private fun playSummaryItem(text: String): Long {
        val durationMs = hapticPlayback.play(text)
        summaryEndsAt = SystemClock.elapsedRealtime() + durationMs
        return durationMs
    }

    private fun stopSummary() {
        summaryEndsAt = 0L
        hapticPlayback.stop()
    }

    private fun navigateToMode(mode: UserMode) {
        val destination = when (mode) {
            UserMode.COMMUNICATOR -> CommunicatorActivity::class.java
            UserMode.DEAFBLIND -> DeafblindActivity::class.java
        }
        startActivity(Intent(this, destination))
        finish()
    }

    // Mesma vibração direta da SplashActivity. Se existir uma classe central para isso, troque aqui.
    private fun vibrate(kind: HapticKind) {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        if (!vibrator.hasVibrator()) return

        val effect = when (kind) {
            HapticKind.TICK -> VibrationEffect.createOneShot(
                HapticConfig.SPLASH_TICK_DURATION, HapticConfig.SPLASH_TICK_AMPLITUDE,
            )
            HapticKind.CONFIRM -> VibrationEffect.createOneShot(
                HapticConfig.SPLASH_FORMED_DURATION, VibrationEffect.DEFAULT_AMPLITUDE,
            )
            // Dois pulsos firmes: diferente de qualquer letra do resumo, para a pessoa
            // ter certeza de que o aceite foi registrado.
            HapticKind.ACCEPTED -> VibrationEffect.createWaveform(
                longArrayOf(0, 250, 150, 250),
                intArrayOf(0, 255, 0, 255),
                -1,
            )
        }
        vibrator.vibrate(effect)
    }
}