package contato.app.domain.service

import contato.app.core.config.HapticConfig

/**
 * Constrói uma mensagem de texto incrementalmente, caractere por caractere,
 * como parte da composição em Braille do usuário Surdocego (seção 3.8-3.9
 * da especificação).
 *
 * Mantém o estado da mensagem em construção e expõe operações equivalentes
 * aos comandos de edição da célula Braille virtual: adicionar caractere,
 * apagar o último, adicionar espaço, e limpar tudo.
 *
 * Esta classe não sabe nada sobre toque, gestos ou vibração — apenas
 * mantém e valida o texto sendo composto.
 */
class MessageBuilder {

    private val buffer = StringBuilder()

    /** Texto atualmente composto, ainda não enviado. */
    val currentText: String
        get() = buffer.toString()

    /**
     * Adiciona um caractere ao final da mensagem em construção.
     *
     * @return true se o caractere foi adicionado; false se a mensagem já
     *         atingiu o limite máximo (HapticConfig.MAX_MESSAGE_LENGTH) e
     *         o caractere foi recusado.
     */
    fun appendCharacter(character: Char): Boolean {
        if (buffer.length >= HapticConfig.MAX_MESSAGE_LENGTH) {
            return false
        }

        buffer.append(character)
        return true
    }

    /**
     * Adiciona um espaço ao final da mensagem em construção.
     * Sujeito ao mesmo limite de tamanho que appendCharacter.
     */
    fun appendSpace(): Boolean {
        return appendCharacter(' ')
    }

    /**
     * Remove o último caractere da mensagem em construção.
     *
     * @return true se havia algo para remover; false se a mensagem já
     *         estava vazia (nada a apagar).
     */
    fun deleteLastCharacter(): Boolean {
        if (buffer.isEmpty()) {
            return false
        }

        buffer.deleteCharAt(buffer.length - 1)
        return true
    }

    /** Limpa toda a mensagem em construção, voltando ao estado inicial. */
    fun clear() {
        buffer.clear()
    }

    /** Verdadeiro se não há nenhum caractere composto ainda. */
    fun isEmpty(): Boolean = buffer.isEmpty()
}