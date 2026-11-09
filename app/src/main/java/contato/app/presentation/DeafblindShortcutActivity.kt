package contato.app.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import contato.app.domain.model.UserMode
import contato.app.presentation.onboarding.SplashActivity
import contato.app.presentation.onboarding.TermsAcceptanceStore

/**
 * Porta de entrada pelos ATALHOS (não tem tela):
 *  - atalho de acessibilidade do Android (segurar os dois botões de volume, botão ou gesto de acessibilidade);
 *  - atalho do ícone (tocar e segurar o ícone do ConTato > "Modo Surdocego").
 *
 * Abre direto o modo Surdocego, sem passar pela escolha de modo. Se os termos desse modo ainda
 * não foram aceitos neste celular, abre o começo normal do app (a pessoa de apoio faz a 1ª vez).
 */
class DeafblindShortcutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val ready = TermsAcceptanceStore(this).hasAccepted(UserMode.DEAFBLIND)
        val target = if (ready) DeafblindActivity::class.java else SplashActivity::class.java
        startActivity(
            Intent(this, target).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        )
        finish()
    }
}
