package contato.app.presentation.debug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import contato.app.core.vibration.HapticMessagePlaybackService
import contato.app.presentation.ModeSelectionActivity
import contato.app.presentation.onboarding.SplashScreen

class VibrationDebugActivity : ComponentActivity() {

    private lateinit var hapticPlayback: HapticMessagePlaybackService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hapticPlayback = HapticMessagePlaybackService(this)

        setContent {
            SplashScreen(
                onFinished = {
                    startActivity(
                        Intent(
                            this@VibrationDebugActivity,
                            ModeSelectionActivity::class.java
                        )
                    )

                    finish()
                }
            )
        }
    }

    override fun onPause() {
        super.onPause()

        if (::hapticPlayback.isInitialized) {
            hapticPlayback.stop()
        }
    }
}