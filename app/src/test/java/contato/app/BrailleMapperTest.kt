package contato.app

import contato.app.domain.service.BrailleMapper
import junit.framework.TestCase.assertEquals
import org.junit.Test

class BrailleMapperTest {

    @Test
    fun `nenhum caractere deve possuir o mesmo padrao braille`() {

        val patterns = BrailleMapper.brailleTable.values

        val uniquePatterns = patterns.toSet()

        assertEquals(patterns.size, uniquePatterns.size)
    }


    @Test
    fun `deve mapear C corretamente`() {

        val result = BrailleMapper.map('C')

        assertEquals('C', result.character)

        assertEquals(
            listOf(true, false, false, true, false, false),
            result.points
        )
    }


    @Test
    fun `testar A ate Z` () {

        val entradas = listOf('A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z')

        for (entrada in entradas) {
            val resultado = BrailleMapper.map(entrada)

            println("Entrada: $entrada \n" +
                    "Saida: ${resultado.points}")
        }
    }

    @Test
    fun `deve fazer o mapeamento inverso de pontos para o caractere C`() {
        val pontos = listOf(true, false, false, true, false, false)
        val resultado = BrailleMapper.reverseMap(pontos)

        assertEquals('C', resultado)
    }

    @Test
    fun `deve retornar null para um padrao de pontos nao mapeado`() {
        val pontosInvalidos = listOf(true, true, true, true, true, true)
        val resultado = BrailleMapper.reverseMap(pontosInvalidos)

        assertEquals(null, resultado)
    }


}




