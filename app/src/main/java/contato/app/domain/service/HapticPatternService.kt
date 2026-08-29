package contato.app.domain.service

import contato.app.core.config.HapticConfig
import contato.app.domain.model.BrailleCharacter
import contato.app.domain.model.HapticEvent
import contato.app.domain.model.HapticEventType
import contato.app.domain.model.HapticPattern

class HapticPatternService {

    fun createPattern (brailleCharacter: BrailleCharacter) : HapticPattern {

        val events = brailleCharacter.points.map { points ->

            if (points) {
                HapticEvent (
                    type = HapticEventType.ACTIVE,
                    duration = HapticConfig.ACTIVE_DURATION,
                    amplitude = HapticConfig.ACTIVE_AMPLITUDE
            )} else {
                    HapticEvent (
                        type = HapticEventType.INACTIVE,
                        duration = HapticConfig.INACTIVE_DURATION,
                        amplitude = HapticConfig.INACTIVE_AMPLITUDE
                    )
                }
            }
        return HapticPattern(events)
    }

    }








