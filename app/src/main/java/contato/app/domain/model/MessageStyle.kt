package contato.app.domain.model

/** Cor dos balões que o Comunicador envia (as recebidas ficam sempre brancas, para contraste). */
enum class BubbleColor(val label: String, val argb: Long) {
    BLUE("Azul", 0xFF1F5BD8),
    TEAL("Verde-água", 0xFF0F8B8D),
    PURPLE("Roxo", 0xFF6D4ED8),
    GRAPHITE("Grafite", 0xFF2B3445),
}

/** Tamanho do texto dentro dos balões. */
enum class MessageTextSize(val label: String, val sp: Int) {
    SMALL("Pequeno", 14),
    MEDIUM("Médio", 16),
    LARGE("Grande", 19),
}

/** Estilo das mensagens na conversa (como em apps de mensagem). */
data class MessageStyle(
    val bubble: BubbleColor = BubbleColor.BLUE,
    val textSize: MessageTextSize = MessageTextSize.MEDIUM,
    val showTimes: Boolean = true,
)
