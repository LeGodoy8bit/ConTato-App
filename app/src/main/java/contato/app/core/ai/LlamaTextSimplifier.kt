package contato.app.core.ai

import android.content.Context
import android.util.Log
import contato.app.domain.repository.AiStatus
import contato.app.domain.repository.SimplifyResult
import contato.app.domain.repository.TextSimplifier
import contato.app.domain.service.SimplificationRules
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * IA de simplificação rodando no próprio celular (SmolLM2-360M ajustado pelo grupo, formato GGUF).
 *
 *  - O modelo vem em app/src/main/assets/[MODEL_ASSET]. Na primeira vez (e a cada atualização do app)
 *    é copiado para a memória interna do app, porque o llama.cpp precisa de um arquivo de verdade.
 *  - Uma instância só no app inteiro ([get]) com UMA thread dedicada: o modelo nativo é único,
 *    então carregar, gerar e liberar sempre acontecem em fila, nunca ao mesmo tempo.
 *  - Nada sai do celular: não usa internet.
 */
internal class LlamaTextSimplifier private constructor(context: Context) : TextSimplifier {

    private val app = context.applicationContext
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "contato-ia").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    private val dispatcher = executor.asCoroutineDispatcher()

    /** Só é lido e escrito na thread da IA. */
    private var status = AiStatus.OFF

    override suspend fun prepare(): AiStatus = withContext(dispatcher) {
        if (status != AiStatus.READY || !LlamaNative.isLoaded()) status = load()
        status
    }

    private fun load(): AiStatus {
        if (!LlamaNative.available) return AiStatus.UNAVAILABLE
        if (app.assets.list("")?.contains(MODEL_ASSET) != true) {
            Log.w(TAG, "Modelo não encontrado em assets/$MODEL_ASSET")
            return AiStatus.MISSING_MODEL
        }
        val file = try {
            copyModelIfNeeded()
        } catch (e: Exception) {
            Log.e(TAG, "Falha ao copiar o modelo", e)
            return AiStatus.UNAVAILABLE
        }
        // Nunca mais threads que núcleos: no teste, passar disso deixou a IA dezenas de vezes mais lenta.
        val threads = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
        return try {
            if (LlamaNative.init(file.absolutePath, threads)) AiStatus.READY else AiStatus.UNAVAILABLE
        } catch (e: Throwable) {
            Log.e(TAG, "Falha ao carregar o modelo", e)
            AiStatus.UNAVAILABLE
        }
    }

    /**
     * Copia só quando o modelo muda: a "etiqueta" junta [MODEL_VERSION] e o tamanho do arquivo.
     * Trocou o modelo? Aumente MODEL_VERSION. Grava num .tmp e renomeia (nunca fica pela metade).
     */
    private fun copyModelIfNeeded(): File {
        val dir = File(app.filesDir, "models").apply { mkdirs() }
        val target = File(dir, MODEL_ASSET)
        val stamp = File(dir, "$MODEL_ASSET.stamp")
        val size = try {
            app.assets.openFd(MODEL_ASSET).use { it.length }
        } catch (e: Exception) {
            -1L // asset comprimido (faltou o noCompress "gguf" no Gradle): copia toda vez
        }
        val label = "$MODEL_VERSION:$size"
        if (size > 0 && target.length() == size && stamp.exists() && stamp.readText() == label) return target

        val tmp = File(dir, "$MODEL_ASSET.tmp")
        app.assets.open(MODEL_ASSET).use { input -> tmp.outputStream().use { input.copyTo(it, 1 shl 16) } }
        if (target.exists()) target.delete()
        if (!tmp.renameTo(target)) throw IllegalStateException("Não foi possível salvar o modelo")
        stamp.writeText(label)
        return target
    }

    override suspend fun simplify(text: String): SimplifyResult {
        val clean = text.trim()
        if (!SimplificationRules.shouldSimplify(clean)) return SimplifyResult.NoGain
        // Se passar do tempo (ou a pessoa sair da tela), a geração nativa é interrompida na hora.
        return withTimeoutOrNull(TIMEOUT_MS) { generate(clean) } ?: SimplifyResult.Failed
    }

    private suspend fun generate(clean: String): SimplifyResult = suspendCancellableCoroutine { cont ->
        // Só interrompe se ESTE pedido já estiver gerando (não derruba o pedido de outra pessoa na fila).
        val running = AtomicBoolean(false)
        cont.invokeOnCancellation { if (running.get()) LlamaNative.abort() }
        executor.execute {
            if (!cont.isActive) return@execute
            running.set(true)
            val result = try {
                if (status != AiStatus.READY || !LlamaNative.isLoaded()) {
                    SimplifyResult.Failed
                } else {
                    val bytes = LlamaNative.generate(
                        SimplificationRules.buildPrompt(clean).toByteArray(Charsets.UTF_8),
                        maxTokens = 64, temperature = 0.1f, topP = 0.9f,
                        repeatPenalty = 1.2f, frequencyPenalty = 0.5f, presencePenalty = 0.1f,
                    )
                    val raw = String(bytes, Charsets.UTF_8)
                    val answer = SimplificationRules.cleanOutput(raw)
                    when {
                        raw.isBlank() -> SimplifyResult.Failed
                        SimplificationRules.isWorthSuggesting(clean, answer) -> SimplifyResult.Shorter(answer)
                        else -> SimplifyResult.NoGain
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Erro ao simplificar", e)
                SimplifyResult.Failed
            } finally {
                running.set(false)
            }
            if (cont.isActive) cont.resume(result)
        }
    }

    override fun unload() {
        executor.execute {
            status = AiStatus.OFF
            try {
                if (LlamaNative.available) LlamaNative.release()
            } catch (_: Throwable) {
            }
        }
    }

    companion object {
        const val MODEL_ASSET = "sentence_compressor.gguf"
        /** Aumente quando trocar o arquivo do modelo por outro. */
        private const val MODEL_VERSION = 1
        private const val TAG = "ConTatoIA"
        /** Celulares mais simples levam alguns segundos; passou disso, desiste e mantém a frase. */
        private const val TIMEOUT_MS = 20_000L

        @Volatile private var instance: LlamaTextSimplifier? = null

        fun get(context: Context): LlamaTextSimplifier =
            instance ?: synchronized(this) {
                instance ?: LlamaTextSimplifier(context).also { instance = it }
            }
    }
}
