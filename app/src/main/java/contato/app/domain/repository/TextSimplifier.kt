package contato.app.domain.repository

/** Estado da IA de simplificação, para a tela mostrar o que está acontecendo. */
enum class AiStatus {
    /** Desligada pelo usuário. */
    OFF,
    /** Carregando o modelo (na primeira vez também copia o arquivo, leva alguns segundos). */
    PREPARING,
    READY,
    /** O arquivo do modelo não está no app (ver passo a passo da IA). */
    MISSING_MODEL,
    /** Este celular não consegue rodar a IA (biblioteca nativa ausente ou erro ao carregar). */
    UNAVAILABLE,
}

/** Resultado de um pedido de simplificação. */
sealed interface SimplifyResult {
    /** Versão mais curta (vibra menos letras). */
    data class Shorter(val text: String) : SimplifyResult
    /** A IA respondeu, mas não ficou mais curto: a frase já está boa. */
    data object NoGain : SimplifyResult
    /** Erro ou demorou demais: dá para tentar de novo. */
    data object Failed : SimplifyResult
}

/**
 * Simplifica frases antes de enviar. A implementação (llama.cpp, no próprio celular) fica em core/ai;
 * o domínio só conhece este contrato.
 */
interface TextSimplifier {
    /** Prepara o modelo. Devolve o estado final (READY, MISSING_MODEL ou UNAVAILABLE). */
    suspend fun prepare(): AiStatus

    suspend fun simplify(text: String): SimplifyResult

    /** Tira o modelo da memória (a pessoa desligou a IA ou fechou a tela). Pode preparar de novo depois. */
    fun unload()
}
