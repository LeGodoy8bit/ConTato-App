package contato.app

import contato.app.domain.model.HapticEvent
import contato.app.domain.model.HapticEventType
import junit.framework.TestCase.assertEquals
import org.junit.Test


class HapticEventTest {
    @Test
    fun `deve criar um evento haptico corretamente`() {

        val event = HapticEvent(
            type = HapticEventType.ACTIVE,
            duration = 100L,
            amplitude = 128
        )

        assertEquals(HapticEventType.ACTIVE, event.type)
        assertEquals(100L, event.duration)
        assertEquals(128, event.amplitude)

    }
}