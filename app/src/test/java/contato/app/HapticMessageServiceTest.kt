package contato.app

import contato.app.domain.service.HapticMessageService
import org.junit.Assert.assertEquals
import org.junit.Test

class HapticMessageServiceTest {

        private val service = HapticMessageService()

        @Test
        fun `deve gerar um pattern para cada caractere da mensagem`() {

            val result = service.createMessage("CASA")

            assertEquals(4, result.messagePattern.size)
        }

        @Test
        fun `deve aceitar letras minusculas convertendo para maiusculas`() {

            val resultMinusculo = service.createMessage("casa")
            val resultMaiusculo = service.createMessage("CASA")

            assertEquals(resultMaiusculo, resultMinusculo)
        }

        @Test(expected = IllegalArgumentException::class)
        fun `deve lancar excecao para mensagem acima do limite`() {

            val mensagemGrande = "A".repeat(51)

            service.createMessage(mensagemGrande)
        }

    @Test
    fun `deve gerar um pattern vazio para o espaco`() {

        val result = service.createMessage("A A")

        assertEquals(3, result.messagePattern.size)

        val spacePattern = result.messagePattern[1]
        assertEquals(0, spacePattern.events.size)
    }

    }
