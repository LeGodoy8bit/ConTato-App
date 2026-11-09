package contato.app.core.metrics

import android.content.Context
import android.content.pm.ApplicationInfo
import android.os.Build
import android.util.Log
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Liga a medição de latência só no build debug (CT24/CT25).
 *
 * Usa a flag "debuggable" do próprio APK, sem depender de BuildConfig (que não está
 * habilitado no projeto). No release, [createProbe] devolve [LatencyProbe.None] e nada
 * é medido, registrado ou gravado.
 *
 * Saída de cada medição:
 * - logcat, tag [TAG];
 * - CSV em Android/data/contato.app/files/ct_latencia_<papel>.csv (copiar com adb pull).
 */
object DebugLatency {

    const val TAG = "CT_LATENCIA"

    /** Grava o CSV fora da thread principal, para não atrasar as vibrações seguintes. */
    private val io: ExecutorService by lazy { Executors.newSingleThreadExecutor() }

    fun isEnabled(context: Context): Boolean =
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    /** @param papel "surdocego" ou "comunicador" (vira parte do nome do arquivo). */
    fun createProbe(context: Context, papel: String): LatencyProbe {
        if (!isEnabled(context)) return LatencyProbe.None
        val app = context.applicationContext
        val versao = runCatching {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName
        }.getOrNull() ?: "?"
        val file = app.getExternalFilesDir(null)?.let { File(it, "ct_latencia_$papel.csv") }
        Log.i(TAG, "medição ligada ($papel). CSV: ${file?.absolutePath ?: "indisponível"}")

        return LatencyRecorder(aparelho = Build.MODEL, versaoApp = versao) { sample ->
            val line = sample.toCsvLine()
            Log.i(TAG, "$papel,$line")
            if (file != null) io.execute {
                runCatching {
                    if (!file.exists()) file.writeText(LatencySample.CSV_HEADER + "\n")
                    file.appendText(line + "\n")
                }.onFailure { Log.w(TAG, "falha ao gravar o CSV", it) }
            }
        }
    }
}
