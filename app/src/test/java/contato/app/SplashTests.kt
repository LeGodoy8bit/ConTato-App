package contato.app

import contato.app.core.config.HapticConfig
import contato.app.domain.model.SplashMode
import contato.app.domain.model.SystemFeedbackEvent
import contato.app.domain.repository.OnboardingRepository
import contato.app.domain.service.SplashModeService
import contato.app.domain.service.SystemHapticPatterns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Repositório falso em memória, só para teste (sem SharedPreferences). */
private class FakeOnboardingRepository(var splashSeen: Boolean = false) : OnboardingRepository {
    override fun hasSeenSplash(): Boolean = splashSeen
    override fun markSplashSeen() {
        splashSeen = true
    }
}

class SplashModeServiceTest {

    @Test
    fun `primeira abertura deve usar a splash completa`() {
        val service = SplashModeService(FakeOnboardingRepository(splashSeen = false))

        assertEquals(SplashMode.FULL, service.resolveSplashMode())
    }

    @Test
    fun `depois de marcada como vista deve usar a splash curta`() {
        val service = SplashModeService(FakeOnboardingRepository(splashSeen = false))

        service.markSplashAsSeen()

        assertEquals(SplashMode.SHORT, service.resolveSplashMode())
    }

    @Test
    fun `se a splash completa nao terminou nao deve ser considerada vista`() {
        val repository = FakeOnboardingRepository(splashSeen = false)
        val service = SplashModeService(repository)

        // Simula o app sendo fechado no meio da animação: ninguém chamou
        // markSplashAsSeen(), então a próxima abertura continua completa.
        service.resolveSplashMode()

        assertEquals(SplashMode.FULL, SplashModeService(repository).resolveSplashMode())
    }
}

class SplashHapticPatternsTest {

    @Test
    fun `o toque por letra deve ser um unico pulso mais curto que os pulsos de sistema`() {
        val pulses = SystemHapticPatterns.getPulsesFor(SystemFeedbackEvent.SPLASH_TYPING_TICK)

        assertEquals(1, pulses.size)
        assertTrue(pulses[0].durationMs < HapticConfig.SYSTEM_SHORT_PULSE)
    }

    @Test
    fun `o pulso do simbolo formado deve ser mais forte que o toque por letra`() {
        val tick = SystemHapticPatterns.getPulsesFor(SystemFeedbackEvent.SPLASH_TYPING_TICK)[0]
        val formed = SystemHapticPatterns.getPulsesFor(SystemFeedbackEvent.SPLASH_SYMBOL_FORMED)[0]

        assertTrue(formed.amplitude > tick.amplitude)
        assertTrue(formed.durationMs > tick.durationMs)
    }
}
