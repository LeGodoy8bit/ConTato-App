package contato.app

import contato.app.domain.service.SimplificationRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SimplificationRulesTest {

    @Test
    fun fraseCurtaNaoChamaAIA() {
        assertFalse(SimplificationRules.shouldSimplify("Oi, tudo bem?"))
        assertTrue(SimplificationRules.shouldSimplify("Você poderia me ajudar com as compras amanhã?"))
    }

    @Test
    fun promptIgualAoDoTreino() {
        assertEquals(
            "<|user|>\nComprima a seguinte sentença de forma concisa, mantendo o significado principal:\n\nOlá mundo\n<|assistant|>\n",
            SimplificationRules.buildPrompt("  Olá mundo "),
        )
    }

    @Test
    fun limpaARespostaDoModelo() {
        assertEquals("Pode me ajudar amanhã?", SimplificationRules.cleanOutput("\n \"Pode me ajudar amanhã?\"\n<|user|> lixo"))
        assertEquals("Vamos sair", SimplificationRules.cleanOutput("Resposta: Vamos sair"))
        assertEquals("", SimplificationRules.cleanOutput("<|end|>"))
    }

    @Test
    fun soSugereQuandoVibraMenosLetras() {
        val original = "Você poderia, por gentileza, me ajudar com as compras amanhã?"
        assertTrue(SimplificationRules.isWorthSuggesting(original, "Me ajuda nas compras amanhã?"))
        assertFalse(SimplificationRules.isWorthSuggesting(original, original))
        assertFalse(SimplificationRules.isWorthSuggesting(original, "!!!"))
        assertEquals(5, SimplificationRules.lettersSaved("abcde fghij", "abcde"))
    }
}
