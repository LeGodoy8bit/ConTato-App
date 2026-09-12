package contato.app.presentation

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import contato.app.R
import contato.app.data.UserModePreferencesRepository
import contato.app.domain.model.UserMode
import contato.app.domain.repository.UserModeRepository

class ModeSelectionActivity : Activity() {

    private lateinit var userModeRepository: UserModeRepository

    private val handler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        userModeRepository = UserModePreferencesRepository(this)

        // Se o usuário já escolheu um modo antes, pula a seleção
        // e vai direto para a tela correspondente.
        val existingMode = userModeRepository.getUserMode()
        if (existingMode != null) {
            navigateToMode(existingMode)
            return
        }

        setContentView(R.layout.activity_mode_selection)

        val rootView = findViewById<View>(R.id.rootModeSelection)
        val communicatorButton = findViewById<Button>(R.id.btnSelectCommunicator)

        communicatorButton.setOnClickListener {
            selectMode(UserMode.COMMUNICATOR)
        }

        // Toque longo em qualquer ponto da tela seleciona o modo Surdocego.
        rootView.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    val runnable = Runnable { selectMode(UserMode.DEAFBLIND) }
                    longPressRunnable = runnable
                    handler.postDelayed(runnable, LONG_PRESS_DURATION_MS)
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    longPressRunnable?.let { handler.removeCallbacks(it) }
                    true
                }
                else -> false
            }
        }
    }

    private fun selectMode(mode: UserMode) {
        userModeRepository.saveUserMode(mode)
        navigateToMode(mode)
    }

    private fun navigateToMode(mode: UserMode) {
        val destination = when (mode) {
            UserMode.COMMUNICATOR -> NearbyDebugActivity::class.java   // provisório até criarmos a tela real do Comunicador
            UserMode.DEAFBLIND -> DeafblindActivity::class.java        // tela real do Surdocego
        }

        startActivity(Intent(this, destination))
        finish()
    }

    companion object {
        private const val LONG_PRESS_DURATION_MS = 5000L
    }
}