package contato.app.domain.service

/**
 * Regras da simplificação por IA (Kotlin puro, testável):
 *  - quando vale chamar a IA;
 *  - o texto exato que a IA recebe (o MESMO formato do treino do modelo);
 *  - como limpar a resposta;
 *  - quando a sugestão vale a pena mostrar (precisa vibrar menos letras).
 */
object SimplificationRules {

    /** Com menos de 5 palavras a IA "inventa" (observado no treino): devolvemos a frase como está. */
    const val MIN_WORDS = 5

    /** Frases muito longas não cabem no contexto do modelo pequeno. */
    const val MAX_CHARS = 280

    fun wordCount(text: String): Int = text.trim().split(Regex("\\s+")).count { it.isNotEmpty() }

    fun shouldSimplify(text: String): Boolean {
        val t = text.trim()
        return t.length <= MAX_CHARS && wordCount(t) >= MIN_WORDS
    }

    /**
     * Prompt idêntico ao usado no fine-tuning (training/trainer.py do projeto da IA).
     * O modelo aprendeu exatamente este formato; mudar o texto piora as respostas.
     */
    fun buildPrompt(text: String): String =
        "<|user|>\nComprima a seguinte sentença de forma concisa, mantendo o significado principal:\n\n" +
            text.trim() + "\n<|assistant|>\n"

    /** Primeira linha válida, sem marcadores, aspas ou prefixos do tipo "Resposta:". */
    fun cleanOutput(raw: String): String {
        val noTags = raw.replace("<|assistant|>", "").replace("<|user|>", "").replace("<|end|>", "")
        val line = noTags.lines().map { it.trim() }.firstOrNull { it.isNotEmpty() && !it.startsWith("<|") } ?: return ""
        return line
            .removePrefix("Resposta:").removePrefix("Sentença comprimida:").removePrefix("Frase:")
            .trim()
            .trim('"', '“', '”', '\'')
            .trim()
    }

    /**
     * A sugestão só aparece se for de fato mais curta para sentir (menos letras vibrando)
     * e ainda tiver letras. Compara o texto que vibra (A-Z e espaço).
     */
    fun isWorthSuggesting(original: String, suggestion: String): Boolean {
        val a = BrailleTextNormalizer.normalizeForBraille(original).replace(" ", "")
        val b = BrailleTextNormalizer.normalizeForBraille(suggestion).replace(" ", "")
        return b.isNotEmpty() && b.length < a.length && !suggestion.equals(original.trim(), ignoreCase = true)
    }

    /** Quantas letras a menos a pessoa surdocega vai sentir. */
    fun lettersSaved(original: String, suggestion: String): Int {
        val a = BrailleTextNormalizer.normalizeForBraille(original).replace(" ", "").length
        val b = BrailleTextNormalizer.normalizeForBraille(suggestion).replace(" ", "").length
        return (a - b).coerceAtLeast(0)
    }
}
