package contato.app

import contato.app.domain.model.WireMessage
import contato.app.domain.service.WireProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WireProtocolTest {

    @Test
    fun codificaEDecodificaTodosOsTipos() {
        val messages = listOf(
            WireMessage.Text("abc", "Olá, tudo bem?"),
            WireMessage.Received("abc"),
            WireMessage.Progress("abc", 7),
            WireMessage.Played("abc"),
            WireMessage.Profile("Ana (Pixel 8)"),
        )
        messages.forEach { assertEquals(it, WireProtocol.decode(WireProtocol.encode(it))) }
    }

    @Test
    fun textoPuroViraMensagemSemId() {
        assertEquals(WireMessage.Text("", "OI"), WireProtocol.decode("OI"))
    }

    @Test
    fun progressoCorrompidoEhIgnorado() {
        val raw = WireProtocol.encode(WireMessage.Progress("x", 1)).dropLast(1) + "z"
        assertNull(WireProtocol.decode(raw))
    }
}
