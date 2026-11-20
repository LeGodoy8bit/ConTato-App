package contato.app.core.metrics

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Liga o registro de eventos dos testes técnicos só no build debug (mesma regra da
 * medição de latência: flag "debuggable" do APK). No release, [create] devolve
 * [TestEventLog.None] e nada é registrado.
 *
 * Saída de cada evento:
 * - logcat, tag [TAG];
 * - CSV em Android/data/contato.app/files/ct_eventos_<papel>.csv (copiar com adb pull
 *   ou pelo gerenciador de arquivos). O arquivo só cresce: cada abertura do app
 *   acrescenta linhas, e o evento APP_ABERTO marca o começo de cada uso.
 *
 * Também registra quando o modo avião e o Bluetooth mudam (MODO_AVIAO, BLUETOOTH):
 * no CT29 isso dá o instante exato em que a queda começou e terminou, sem cronômetro.
 *
 * As mensagens dos testes são fictícias; por isso o texto pode ir para o arquivo.
 */
object DebugEvents {

    const val TAG = "CT_EVENTOS"

    /** Grava fora da thread principal, para não atrasar vibrações nem a interface. */
    private val io: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    /** Papéis que já têm o receptor de sistema (evita registrar duas vezes ao reabrir a tela). */
    private val systemWatchers = mutableSetOf<String>()

    /** @param papel "surdocego" ou "comunicador" (vira parte do nome do arquivo). */
    fun create(context: Context, papel: String): TestEventLog {
        if (!DebugLatency.isEnabled(context)) return TestEventLog.None
        val app = context.applicationContext
        val versao = runCatching {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName
        }.getOrNull() ?: "?"
        val aparelho = Build.MODEL ?: "?"
        val file = app.getExternalFilesDir(null)?.let { File(it, "ct_eventos_$papel.csv") }
        Log.i(TAG, "registro de eventos ligado ($papel). CSV: ${file?.absolutePath ?: "indisponível"}")

        val log = object : TestEventLog {
            override fun log(evento: String, msgId: String, detalhe: String) {
                val event = TestEvent(
                    epochMs = System.currentTimeMillis(),
                    uptimeMs = SystemClock.elapsedRealtime(),
                    papel = papel,
                    evento = evento,
                    msgId = msgId,
                    detalhe = detalhe,
                    aparelho = aparelho,
                    versaoApp = versao,
                )
                io.execute {
                    val line = event.toCsvLine()
                    Log.i(TAG, line)
                    if (file != null) runCatching {
                        if (!file.exists()) file.writeText(TestEvent.CSV_HEADER + "\n")
                        file.appendText(line + "\n")
                    }.onFailure { Log.w(TAG, "falha ao gravar o CSV", it) }
                }
            }
        }
        log.log("APP_ABERTO", detalhe = "android=${Build.VERSION.RELEASE}")
        watchSystem(app, papel, log)
        return log
    }

    /** Modo avião e Bluetooth (transmissões do próprio sistema). Fica ativo enquanto o app estiver aberto. */
    private fun watchSystem(app: Context, papel: String, log: TestEventLog) {
        synchronized(systemWatchers) { if (!systemWatchers.add(papel)) return }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                when (intent.action) {
                    Intent.ACTION_AIRPLANE_MODE_CHANGED ->
                        log.log("MODO_AVIAO", detalhe = "ligado=${intent.getBooleanExtra("state", false)}")
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val estado = when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)) {
                            BluetoothAdapter.STATE_ON -> "ligado"
                            BluetoothAdapter.STATE_OFF -> "desligado"
                            BluetoothAdapter.STATE_TURNING_ON -> "ligando"
                            BluetoothAdapter.STATE_TURNING_OFF -> "desligando"
                            else -> "desconhecido"
                        }
                        log.log("BLUETOOTH", detalhe = "estado=$estado")
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        runCatching {
            ContextCompat.registerReceiver(app, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }.onFailure { Log.w(TAG, "não foi possível acompanhar modo avião/Bluetooth", it) }
    }
}
