package contato.app

import contato.app.core.metrics.TestEvent
import contato.app.core.metrics.TestEventLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TestEventLogTest {

    private fun event(detalhe: String) = TestEvent(
        epochMs = 1_700_000_000_000L,
        uptimeMs = 12_345L,
        papel = "comunicador",
        evento = "ENVIO",
        msgId = "abc",
        detalhe = detalhe,
        aparelho = "Redmi 14C",
        versaoApp = "1.0",
    )

    @Test
    fun linhaTemAsMesmasColunasDoCabecalho() {
        val colunas = TestEvent.CSV_HEADER.split(",").size
        val linha = event("tam=3;texto=OLA").toCsvLine()
        assertEquals(colunas, linha.split(",").size)
        assertTrue(linha.contains(",12345,comunicador,ENVIO,abc,tam=3;texto=OLA,Redmi 14C,1.0"))
    }

    @Test
    fun textoComVirgulaOuAspasViraCampoEntreAspas() {
        val linha = event("texto=OI, \"TUDO\" BEM").toCsvLine()
        assertTrue(linha.contains(",\"texto=OI, \"\"TUDO\"\" BEM\","))
    }

    @Test
    fun dataHoraTemMilissegundos() {
        val dataHora = TestEvent.formatDataHora(1_700_000_000_123L)
        assertTrue(Regex("""\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.123""").matches(dataHora))
    }

    @Test
    fun registroDoReleaseNaoFazNada() {
        TestEventLog.None.log("ENVIO", "abc", "texto=OLA") // não lança exceção nem grava
    }
}
