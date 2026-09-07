package contato.app
import contato.app.core.config.HapticConfig
import contato.app.domain.model.HapticEvent
import contato.app.domain.model.HapticEventType
import contato.app.domain.model.HapticPattern
import contato.app.domain.service.HapticMessageService
import contato.app.domain.service.HapticWaveformBuilder
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class HapticWaveformBuilderTest {

        private val builder = HapticWaveformBuilder()

        @Test
        fun `deve montar timings e amplitudes corretos para um pattern isolado`() {

            val pattern = HapticPattern(
                events = listOf(
                    HapticEvent(HapticEventType.ACTIVE, HapticConfig.ACTIVE_DURATION, HapticConfig.ACTIVE_AMPLITUDE),
                    HapticEvent(HapticEventType.INACTIVE, HapticConfig.INACTIVE_DURATION, HapticConfig.INACTIVE_AMPLITUDE)
                )
            )

            val waveform = builder.buildCharacterWaveform(pattern)

            val expectedTimings = longArrayOf(
                HapticConfig.ACTIVE_DURATION, HapticConfig.POINT_PAUSE,
                HapticConfig.INACTIVE_DURATION, HapticConfig.POINT_PAUSE
            )

            val expectedAmplitudes = intArrayOf(
                HapticConfig.ACTIVE_AMPLITUDE, 0,
                HapticConfig.INACTIVE_AMPLITUDE, 0
            )

            assertArrayEquals(expectedTimings, waveform.timings)
            assertArrayEquals(expectedAmplitudes, waveform.amplitudes)
        }

        @Test
        fun `deve usar CHARACTER_PAUSE entre duas letras`() {

            val message = HapticMessageService().createMessage("AA")

            val waveform = builder.buildMessageWaveform(message)

            // cada letra A tem 1 evento ativo -> 2 posições (duration + POINT_PAUSE)
            // então a pausa entre as duas letras deve estar bem no meio do array
            val pauseIndex = 12 // posição 0 e 1 = letra A (evento + point_pause), posição 2 = pausa entre letras

            assertArrayEquals(
                longArrayOf(HapticConfig.CHARACTER_PAUSE),
                longArrayOf(waveform.timings[pauseIndex])
            )
        }

        @Test
        fun `deve usar SPACE_PAUSE quando houver espaco entre as letras`() {

            val message = HapticMessageService().createMessage("A A")

            val waveform = builder.buildMessageWaveform(message)

            val pauseIndex = 12

            assertArrayEquals(
                longArrayOf(HapticConfig.SPACE_PAUSE),
                longArrayOf(waveform.timings[pauseIndex])
            )
        }
}
