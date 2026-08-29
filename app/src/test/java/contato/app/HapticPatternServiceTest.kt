package contato.app

import contato.app.core.config.HapticConfig
import contato.app.domain.model.BrailleCharacter
import contato.app.domain.model.HapticEventType
import contato.app.domain.service.HapticPatternService
import org.junit.Assert.assertEquals
import org.junit.Test

class HapticPatternServiceTest {

    private val service = HapticPatternService()

    @Test
    fun `deve gerar seis eventos para um caractere Braille`() {

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

        val pattern = service.createPattern(brailleCharacter)

        assertEquals(6, pattern.events.size)
    }


    @Test
    fun `deve converter corretamente pontos ativos e inativos`() {

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

        val pattern = service.createPattern(brailleCharacter)

        assertEquals(HapticEventType.ACTIVE, pattern.events[0].type)
        assertEquals(HapticEventType.INACTIVE, pattern.events[1].type)
        assertEquals(HapticEventType.INACTIVE, pattern.events[2].type)
        assertEquals(HapticEventType.ACTIVE, pattern.events[3].type)
        assertEquals(HapticEventType.INACTIVE, pattern.events[4].type)
        assertEquals(HapticEventType.INACTIVE, pattern.events[5].type)
    }



    @Test
    fun `deve usar configuracoes diferentes para pontos ativos e inativos`() {

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

        val pattern = service.createPattern(brailleCharacter)

        val activeEvent = pattern.events[0]
        val inactiveEvent = pattern.events[1]

        assertEquals(
            HapticConfig.ACTIVE_DURATION,
            activeEvent.duration
        )

        assertEquals(
            HapticConfig.ACTIVE_AMPLITUDE,
            activeEvent.amplitude
        )

        assertEquals(
            HapticConfig.INACTIVE_DURATION,
            inactiveEvent.duration
        )

        assertEquals(
            HapticConfig.INACTIVE_AMPLITUDE,
            inactiveEvent.amplitude
        )
    }


}

