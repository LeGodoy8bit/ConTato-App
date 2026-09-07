package contato.app.presentation

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import contato.app.core.vibration.HapticMessagePlaybackService

class MainActivity : Activity() {

    private lateinit var hapticPlayback: HapticMessagePlaybackService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hapticPlayback = HapticMessagePlaybackService(this)

        val button = Button(this)
        button.text = "Testar vibração"

        button.setOnClickListener {
            hapticPlayback.play("CASA AZUL")
        }

        setContentView(button)
    }

    override fun onPause() {
        super.onPause()
        hapticPlayback.stop()
    }
}