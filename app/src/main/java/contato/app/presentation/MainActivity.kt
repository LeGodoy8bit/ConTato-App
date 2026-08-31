package contato.app.presentation

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import contato.app.core.vibration.HapticController
import contato.app.domain.model.BrailleCharacter
import contato.app.domain.model.HapticPattern
import contato.app.domain.service.HapticPatternService

class MainActivity : Activity() {

    private lateinit var hapticController: HapticController
    private lateinit var hapticPatternService: HapticPatternService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hapticController = HapticController(this)
        hapticPatternService = HapticPatternService()

        val button = Button(this)
        button.text = "Testar vibração"

        button.setOnClickListener {

            val brailleCharacter = BrailleCharacter(
                character = 'C',
                points = listOf(
                    true,
                    false,
                    false,
                    true,
                    false,
                    false
                )
            )

            val pattern = hapticPatternService.createPattern(
                brailleCharacter
            )

            hapticController.play(pattern)
        }

        setContentView(button)
    }
}