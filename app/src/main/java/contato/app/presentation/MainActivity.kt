package contato.app.presentation

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import contato.app.core.vibration.HapticController
import contato.app.domain.service.HapticMessageService

class MainActivity : Activity() {

    private lateinit var hapticController: HapticController
    private lateinit var hapticMessageService: HapticMessageService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hapticController = HapticController(this)
        hapticMessageService = HapticMessageService()

        val button = Button(this)
        button.text = "Testar vibração"

        button.setOnClickListener {
            val message = hapticMessageService.createMessage("CASA AZUL")
            hapticController.playMessage(message)
        }

        setContentView(button)
    }
}