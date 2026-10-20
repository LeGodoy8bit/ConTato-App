package contato.app.presentation.onboarding

import android.content.Context
import contato.app.domain.model.UserMode

/**
 * Versão dos termos. Mude este valor sempre que os textos em LegalTexts mudarem
 * de forma relevante: o app volta a pedir o aceite.
 */
internal const val TERMS_VERSION = "2026-10-18"

/**
 * Guarda, por modo de uso, qual versão dos termos a pessoa aceitou e quando.
 * Cada modo tem o seu próprio registro, porque cada pessoa aceita por si mesma.
 */
internal class TermsAcceptanceStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("contato_terms", Context.MODE_PRIVATE)

    fun hasAccepted(mode: UserMode): Boolean =
        prefs.getString(versionKey(mode), null) == TERMS_VERSION

    fun markAccepted(mode: UserMode) {
        prefs.edit()
            .putString(versionKey(mode), TERMS_VERSION)
            .putLong(versionKey(mode) + "_at", System.currentTimeMillis())
            .apply()
    }

    private fun versionKey(mode: UserMode) = "accepted_version_${mode.name}"
}
