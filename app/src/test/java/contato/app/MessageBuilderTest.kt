package contato.app

import contato.app.core.config.HapticConfig
import contato.app.domain.service.MessageBuilder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageBuilderTest {

    @Test
    fun `deve iniciar vazio`() {
        val builder = MessageBuilder()

        assertTrue(builder.isEmpty())
        assertEquals("", builder.currentText)
    }

    @Test
    fun `deve adicionar caracteres um a um`() {
        val builder = MessageBuilder()

        builder.appendCharacter('C')
        builder.appendCharacter('A')
        builder.appendCharacter('S')
        builder.appendCharacter('A')

        assertEquals("CASA", builder.currentText)
    }

    @Test
    fun `deve adicionar espaco entre palavras`() {
        val builder = MessageBuilder()

        builder.appendCharacter('O')
        builder.appendCharacter('I')
        builder.appendSpace()
        builder.appendCharacter('O')
        builder.appendCharacter('I')

        assertEquals("OI OI", builder.currentText)
    }

    @Test
    fun `deve apagar o ultimo caractere`() {
        val builder = MessageBuilder()

        builder.appendCharacter('O')
        builder.appendCharacter('I')
        val resultado = builder.deleteLastCharacter()

        assertTrue(resultado)
        assertEquals("O", builder.currentText)
    }

    @Test
    fun `deve retornar false ao tentar apagar mensagem vazia`() {
        val builder = MessageBuilder()

        val resultado = builder.deleteLastCharacter()

        assertFalse(resultado)
        assertEquals("", builder.currentText)
    }

    @Test
    fun `deve recusar caractere alem do limite maximo`() {
        val builder = MessageBuilder()

        repeat(HapticConfig.MAX_MESSAGE_LENGTH) {
            builder.appendCharacter('A')
        }

        val resultado = builder.appendCharacter('A')

        assertFalse(resultado)
        assertEquals(HapticConfig.MAX_MESSAGE_LENGTH, builder.currentText.length)
    }

    @Test
    fun `deve limpar a mensagem em construcao`() {
        val builder = MessageBuilder()

        builder.appendCharacter('O')
        builder.appendCharacter('I')
        builder.clear()

        assertTrue(builder.isEmpty())
        assertEquals("", builder.currentText)
    }
}