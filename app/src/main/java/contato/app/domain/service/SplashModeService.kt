package contato.app.domain.service

import contato.app.domain.model.SplashMode
import contato.app.domain.repository.OnboardingRepository

/**
 * Decide se a splash roda completa ou curta, conforme a decisão do grupo:
 * completa só na primeira abertura, curta nas seguintes.
 *
 * Kotlin puro: não conhece Android nem a animação em si, só a regra.
 */
class SplashModeService(
    private val onboardingRepository: OnboardingRepository
) {

    /** Primeira vez: FULL. Depois de a splash completa ter terminado: SHORT. */
    fun resolveSplashMode(): SplashMode {
        return if (onboardingRepository.hasSeenSplash()) {
            SplashMode.SHORT
        } else {
            SplashMode.FULL
        }
    }

    /**
     * Deve ser chamado quando a splash completa TERMINA, e não quando começa.
     * Assim, se o app for fechado no meio da primeira animação, ela volta a
     * ser completa na próxima abertura, em vez de o usuário nunca vê-la.
     */
    fun markSplashAsSeen() {
        onboardingRepository.markSplashSeen()
    }
}
