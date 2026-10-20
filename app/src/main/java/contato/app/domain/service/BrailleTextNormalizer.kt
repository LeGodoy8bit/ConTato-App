package contato.app.domain.service

import java.text.Normalizer

/**
 * Prepara um texto qualquer para ser vibrado em Braille.
 *
 * O BrailleMapper só conhece as letras A-Z. Sem esta etapa, qualquer acento
 * ("você", "olá"), "ç", número ou pontuação vindo do Comunicador fazia o
 * BrailleMapper.map() lançar IllegalArgumentException e o app do Surdocego fechar.
 *
 * Regras:
 * - remove acentos e cedilha ("ção" -> "CAO");
 * - converte para maiúsculas;
 * - descarta tudo que não for letra A-Z ou espaço (números e pontuação ainda
 *   não têm representação vibratória: ver pendências);
 * - junta espaços repetidos e tira espaços das pontas.
 *
 * Kotlin puro (java.text faz parte da JDK, não do Android): testável com JUnit.
 */
object BrailleTextNormalizer {

    private val COMBINING_MARKS = Regex("\\p{Mn}+")
    private val MULTIPLE_SPACES = Regex(" +")

    fun normalizeForBraille(text: String): String {
        val withoutAccents = Normalizer.normalize(text, Normalizer.Form.NFD)
            .replace(COMBINING_MARKS, "")

        val onlySupported = withoutAccents
            .uppercase()
            .map { char -> if (char.isWhitespace()) ' ' else char }
            .filter { char -> char == ' ' || char in 'A'..'Z' }
            .joinToString("")

        return onlySupported.replace(MULTIPLE_SPACES, " ").trim()
    }
}
