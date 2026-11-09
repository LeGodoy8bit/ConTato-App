package contato.app

import contato.app.domain.service.TimeEstimates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeEstimatesTest {

    @Test
    fun mensagemMaiorLevaMaisTempo() {
        assertTrue(TimeEstimates.feelMs("OI TUDO BEM") > TimeEstimates.feelMs("OI"))
        assertEquals(0L, TimeEstimates.feelMs("123"))
    }

    @Test
    fun faltaMenosNoFimDaMensagem() {
        val text = "BOM DIA"
        assertTrue(TimeEstimates.remainingFeelMs(text, 5, 1f) < TimeEstimates.remainingFeelMs(text, 0, 1f))
        assertEquals(TimeEstimates.feelMs(text), TimeEstimates.remainingFeelMs(text, 0, 1f))
    }

    @Test
    fun respostaTipicaUsaAMediana() {
        assertEquals(TimeEstimates.DEFAULT_REPLY_MS, TimeEstimates.typicalReplyMs(emptyList()))
        assertEquals(40_000L, TimeEstimates.typicalReplyMs(listOf(30_000, 40_000, 200_000)))
        assertEquals(15_000L, TimeEstimates.typicalReplyMs(listOf(1_000)))
    }

    @Test
    fun cedoDemaisParaMedirVelocidade() {
        assertNull(TimeEstimates.measureScale("AB", 0, 0, 500))
    }

    @Test
    fun textoAmigavel() {
        assertEquals("~10 s", TimeEstimates.friendly(8_000))
        assertEquals("~1 min", TimeEstimates.friendly(70_000))
        assertEquals("~2 min", TimeEstimates.friendly(120_000))
        assertEquals("~2 min e meio", TimeEstimates.friendly(150_000))
    }
}
