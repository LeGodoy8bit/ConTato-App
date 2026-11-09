package contato.app.core.metrics

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import contato.app.core.config.HapticConfig

/**
 * Teste automático de latência (CT24/CT25), no celular do Comunicador.
 *
 * Envia as mensagens do [LatencyTestPlan] uma por vez.
 *
 * - Modo completo: só manda a próxima depois que a anterior terminou de vibrar no
 *   Surdocego (status PLAYED) e de uma pausa [GAP_MS]. Leva de 1 a 2 horas.
 * - Modo rápido: manda a próxima assim que a anterior começou a vibrar (status PLAYING,
 *   ou seja, o T1 dela já foi medido) e de uma pausa [FAST_GAP_MS]. A mensagem nova
 *   interrompe a vibração da anterior. Leva poucos minutos.
 *
 * Não sabe nada da tela: recebe funções da sessão do Comunicador.
 */
class LatencyTestRunner(
    private val send: (id: String, text: String) -> Boolean,
    private val isFinished: (id: String) -> Boolean,
    private val isPlayingOrDone: (id: String) -> Boolean,
    private val isConnected: () -> Boolean,
) {

    private val handler = Handler(Looper.getMainLooper())
    private val queue = ArrayDeque<LatencyTestPlan.Item>()
    private var current: LatencyTestPlan.Item? = null
    private var sentAt = 0L
    private var total = 0
    private var fast = false

    val isRunning: Boolean get() = current != null || queue.isNotEmpty()

    fun start(items: List<LatencyTestPlan.Item>, fastMode: Boolean = false) {
        if (isRunning) {
            Log.w(DebugLatency.TAG, "teste já em andamento: pedido ignorado")
            return
        }
        queue.addAll(items)
        total = items.size
        fast = fastMode
        Log.i(DebugLatency.TAG, "início do teste: $total mensagens, modo ${if (fast) "rápido" else "completo"}")
        next()
    }

    fun stop() {
        handler.removeCallbacksAndMessages(null)
        if (isRunning) Log.i(DebugLatency.TAG, "teste interrompido (${total - queue.size} de $total)")
        queue.clear()
        current = null
    }

    private fun next() {
        val item = queue.removeFirstOrNull()
        if (item == null) {
            current = null
            Log.i(DebugLatency.TAG, "fim do teste: $total mensagens")
            return
        }
        if (!isConnected()) {
            Log.w(DebugLatency.TAG, "sem conexão: teste interrompido em ${item.id}")
            queue.clear()
            current = null
            return
        }
        current = item
        Log.i(DebugLatency.TAG, "enviando ${item.id} (${total - queue.size}/$total)")
        if (!send(item.id, item.text)) {
            Log.w(DebugLatency.TAG, "${item.id} não foi enviada")
            current = null
            handler.postDelayed({ next() }, GAP_MS)
            return
        }
        sentAt = SystemClock.uptimeMillis()
        handler.postDelayed({ poll() }, POLL_MS)
    }

    private fun poll() {
        val item = current ?: return
        when {
            (if (fast) isPlayingOrDone(item.id) else isFinished(item.id)) -> {
                current = null
                handler.postDelayed({ next() }, if (fast) FAST_GAP_MS else GAP_MS)
            }
            SystemClock.uptimeMillis() - sentAt > TIMEOUT_MS -> {
                Log.w(DebugLatency.TAG, "${item.id}: sem fim de vibração em ${TIMEOUT_MS / 1000} s, seguindo")
                current = null
                handler.postDelayed({ next() }, GAP_MS)
            }
            else -> handler.postDelayed({ poll() }, POLL_MS)
        }
    }

    companion object {
        private const val POLL_MS = 200L
        /** Deixa o Surdocego voltar a "aguardando mensagem" antes da próxima. */
        private const val GAP_MS = 2000L
        /** Modo rápido: tempo para a vibração começar de fato antes da próxima mensagem. */
        private const val FAST_GAP_MS = 1500L
        /** Uma mensagem de 80 caracteres leva ~2 min na velocidade 1×; folga para a 0,6×. */
        private const val TIMEOUT_MS = 5 * 60_000L
    }
}

/**
 * Comando para iniciar ou parar o teste pelo adb, sem botão na interface.
 * Só é registrado no build debug; no release não existe.
 *
 *   adb shell am broadcast -a contato.app.debug.CT25_START -p contato.app
 *   adb shell am broadcast -a contato.app.debug.CT25_START -p contato.app --ei repeticoes 5 --es tamanhos "10,80"
 *   adb shell am broadcast -a contato.app.debug.CT25_START -p contato.app --ez rapido true
 *   adb shell am broadcast -a contato.app.debug.CT25_STOP -p contato.app
 */
class LatencyTestTrigger(
    context: Context,
    private val onStart: (sizes: List<Int>, repetitions: Int, fastMode: Boolean) -> Unit,
    private val onStop: () -> Unit,
) {

    private val app = context.applicationContext
    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_START -> {
                    val repetitions = intent.getIntExtra(EXTRA_REPETITIONS, LatencyTestPlan.DEFAULT_REPETITIONS)
                        .coerceIn(1, 1000)
                    val sizes = intent.getStringExtra(EXTRA_SIZES)
                        ?.split(',')
                        ?.mapNotNull { it.trim().toIntOrNull() }
                        ?.filter { it in 1..HapticConfig.MAX_MESSAGE_LENGTH }
                        ?.takeIf { it.isNotEmpty() }
                        ?: LatencyTestPlan.DEFAULT_SIZES
                    onStart(sizes, repetitions, intent.getBooleanExtra(EXTRA_FAST, false))
                }
                ACTION_STOP -> onStop()
            }
        }
    }

    fun register() {
        if (registered || !DebugLatency.isEnabled(app)) return
        val filter = IntentFilter().apply {
            addAction(ACTION_START)
            addAction(ACTION_STOP)
        }
        // EXPORTED: o comando vem do adb (outro processo).
        ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_EXPORTED)
        registered = true
    }

    fun unregister() {
        if (!registered) return
        app.unregisterReceiver(receiver)
        registered = false
    }

    companion object {
        const val ACTION_START = "contato.app.debug.CT25_START"
        const val ACTION_STOP = "contato.app.debug.CT25_STOP"
        const val EXTRA_REPETITIONS = "repeticoes"
        const val EXTRA_SIZES = "tamanhos"
        const val EXTRA_FAST = "rapido"
    }
}
