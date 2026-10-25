package contato.app.core.ai

import android.util.Log

/**
 * Ponte com a biblioteca nativa "contatollm" (app/src/main/cpp, llama.cpp).
 * Os nomes daqui precisam bater com Java_contato_app_core_ai_LlamaNative_* em contato_jni.cpp.
 *
 * Se a biblioteca não existir neste celular (ABI não compilada, build sem NDK), [available] fica false
 * e o app segue funcionando sem a IA.
 */
internal object LlamaNative {

    val available: Boolean by lazy {
        try {
            System.loadLibrary("contatollm")
            true
        } catch (e: Throwable) {
            Log.w("ConTatoIA", "Biblioteca nativa da IA indisponível", e)
            false
        }
    }

    external fun init(modelPath: String, threads: Int): Boolean

    /**
     * Entra e sai em UTF-8 cru (bytes): na entrada, emojis chegam certos ao modelo;
     * na saída, um caractere cortado no fim da geração não derruba o app.
     */
    external fun generate(
        prompt: ByteArray,
        maxTokens: Int,
        temperature: Float,
        topP: Float,
        repeatPenalty: Float,
        frequencyPenalty: Float,
        presencePenalty: Float,
    ): ByteArray

    external fun isLoaded(): Boolean

    /** Para a geração em andamento (chamar de outra thread). */
    external fun abort()

    external fun release()
}
