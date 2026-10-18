package contato.app.domain.repository

/**
 * Guarda o progresso do usuário no onboarding (primeiros passos do app).
 *
 * O domínio conhece só este contrato, nunca a implementação real
 * (SharedPreferences, hoje). Por enquanto controla apenas a splash; deve
 * crescer junto com o onboarding (ex.: aceite dos termos de uso).
 */
interface OnboardingRepository {

    /** Verdadeiro se a splash completa já foi exibida até o fim alguma vez. */
    fun hasSeenSplash(): Boolean

    /** Registra que a splash completa foi exibida até o fim. */
    fun markSplashSeen()
}
