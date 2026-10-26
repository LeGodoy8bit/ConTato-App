package contato.app.core.orientation

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Surface

/**
 * Avisa quando a tela do modo Surdocego gira de ponta-cabeça (e quando volta).
 *
 * O app fica sempre na vertical (Manifest: sensorPortrait). Se a pessoa vira o celular de
 * cabeça para baixo, o Android gira a tela 180° — mesmo com a rotação automática desligada —
 * e a cela gira junto: o ponto 1 continua no canto de cima à esquerda de quem segura.
 * Este observador só percebe esse giro, para o app vibrar um padrão próprio.
 *
 * Um giro de 180° não muda a configuração da tela (onConfigurationChanged não é chamado),
 * por isso ouvimos o DisplayManager.
 * Em tablets (tela "deitada" por natureza) a conta de rotação é outra: lá não avisamos.
 */
internal class FlipWatcher(
    context: Context,
    private val onFlip: (upsideDown: Boolean) -> Unit,
) {
    private val displayManager = context.applicationContext.getSystemService(DisplayManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val isPhone = context.resources.configuration.smallestScreenWidthDp < 600

    private var lastUpsideDown = false
    private var running = false

    private val upsideDown: Boolean
        get() = displayManager?.getDisplay(Display.DEFAULT_DISPLAY)?.rotation == Surface.ROTATION_180

    private val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            if (displayId != Display.DEFAULT_DISPLAY) return
            val now = upsideDown
            if (now != lastUpsideDown) {
                lastUpsideDown = now
                onFlip(now)
            }
        }
    }

    fun start() {
        if (running || !isPhone || displayManager == null) return
        running = true
        lastUpsideDown = upsideDown // ao voltar para o app não vibra à toa
        displayManager.registerDisplayListener(listener, handler)
    }

    fun stop() {
        if (!running) return
        running = false
        displayManager?.unregisterDisplayListener(listener)
    }
}
