package contato.app

import contato.app.domain.service.BrailleTextNormalizer
import contato.app.domain.service.HapticMessageService
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BrailleTextNormalizerTest {

    @Test
    fun removeAcentosECedilha() {
        assertEquals("OLA VOCE ACAO", BrailleTextNormalizer.normalizeForBraille("Olá você ação"))
    }

    @Test
    fun descartaPontuacaoENumeros() {
        assertEquals("OI TUDO BEM", BrailleTextNormalizer.normalizeForBraille("Oi, tudo bem? 123"))
    }

    @Test
    fun juntaEspacosRepetidosETiraDasPontas() {
        assertEquals("A B", BrailleTextNormalizer.normalizeForBraille("  a   \n b  "))
    }

    @Test
    fun textoSemLetrasViraVazio() {
        assertEquals("", BrailleTextNormalizer.normalizeForBraille("?!123"))
    }

    @Test
    fun mensagemComAcentoNaoDerrubaMaisOHapticMessageService() {
        // Antes desta correção, "Olá" lançava IllegalArgumentException no BrailleMapper.
        val message = HapticMessageService().createMessage("Olá, você!")
        assertEquals(8, message.messagePattern.size) // "OLA VOCE" = 7 letras + 1 espaço
    }

    @Test
    fun textoSoComSimbolosGeraMensagemVazia() {
        val message = HapticMessageService().createMessage("!!!")
        assertTrue(message.messagePattern.isEmpty())
    }
}
