package contato.app.domain.service

import contato.app.domain.model.TrainingModule

/**
 * Conteúdo do treino, do mais simples ao mais difícil.
 * Tudo já em maiúsculas e sem acento (é o que vibra). Palavras e frases curtas e úteis no dia a dia.
 */
object TrainingCatalog {

    val modules: List<TrainingModule> = listOf(
        TrainingModule(
            id = "letters", number = 1, title = "Conhecer as letras",
            subtitle = "As 26 letras, uma por vez: sentir e marcar na cela.",
            items = ('A'..'Z').map { it.toString() },
        ),
        TrainingModule(
            id = "words", number = 2, title = "Palavras",
            subtitle = "Palavras curtas do dia a dia.",
            items = listOf("OI", "SIM", "NAO", "AGUA", "CASA", "BOM", "AJUDA", "FOME", "DOR", "OBRIGADO"),
        ),
        TrainingModule(
            id = "phrases", number = 3, title = "Frases",
            subtitle = "Frases para conversar, com espaço entre as palavras.",
            items = listOf("OI TUDO BEM", "BOM DIA", "ESTOU BEM", "QUERO AGUA", "PRECISO DE AJUDA", "ATE LOGO"),
        ),
    )

    fun module(id: String): TrainingModule? = modules.firstOrNull { it.id == id }

    /** Todo item precisa ser vibrável (só A-Z e espaço) e caber numa mensagem. */
    fun isValid(): Boolean = modules.all { m ->
        m.items.isNotEmpty() && m.items.all { item ->
            BrailleTextNormalizer.normalizeForBraille(item) == item
        }
    }
}
