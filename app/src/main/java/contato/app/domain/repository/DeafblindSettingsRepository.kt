package contato.app.domain.repository

import contato.app.domain.model.DeafblindSettings

interface DeafblindSettingsRepository {
    fun load(): DeafblindSettings
    fun save(settings: DeafblindSettings)
}
