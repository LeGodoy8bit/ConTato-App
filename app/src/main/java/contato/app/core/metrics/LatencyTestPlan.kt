package contato.app.core.metrics

import contato.app.core.config.HapticConfig

/**
 * Mensagens do teste automático de latência (CT24/CT25): textos fixos e reproduzíveis,
 * só com letras A–Z e espaço, para que a normalização não mude o tamanho.
 *
 * As rodadas são intercaladas (10, 40, 80, 10, 40, 80...): assim, aquecimento do aparelho,
 * bateria ou interferência ao longo do teste afetam os três tamanhos por igual.
 *
 * Kotlin puro: testável com JUnit.
 */
object LatencyTestPlan {

    const val ID_PREFIX = "ct25"
    val DEFAULT_SIZES = listOf(10, 40, 80)
    const val DEFAULT_REPETITIONS = 30

    /** Sem espaços duplos; repetido até atingir o tamanho pedido. */
    private const val BASE_TEXT =
        "TESTE DE LATENCIA DO CONTATO MENSAGEM FIXA PARA MEDIR O TEMPO ATE A PRIMEIRA VIBRACAO "

    data class Item(val id: String, val execucao: Int, val tamanho: Int, val text: String)

    /** Texto com exatamente [size] caracteres, sem espaço nas pontas. */
    fun textOf(size: Int): String {
        require(size in 1..HapticConfig.MAX_MESSAGE_LENGTH) { "Tamanho fora do limite: $size" }
        val builder = StringBuilder()
        while (builder.length < size) builder.append(BASE_TEXT)
        val chars = builder.substring(0, size).toCharArray()
        // A normalização tiraria um espaço no fim e a mensagem ficaria com uma letra a menos.
        if (chars.last() == ' ') chars[chars.lastIndex] = 'A'
        return String(chars)
    }

    fun build(
        sizes: List<Int> = DEFAULT_SIZES,
        repetitions: Int = DEFAULT_REPETITIONS,
    ): List<Item> = (1..repetitions).flatMap { execucao ->
        sizes.map { size -> Item(idOf(size, execucao), execucao, size, textOf(size)) }
    }

    fun idOf(size: Int, execucao: Int) = "$ID_PREFIX-$size-$execucao"

    /** Número da repetição a partir do id da mensagem; null para mensagens digitadas (id UUID). */
    fun parseExecucao(id: String): Int? {
        val parts = id.split('-')
        return if (parts.size == 3 && parts[0] == ID_PREFIX) parts[2].toIntOrNull() else null
    }
}
