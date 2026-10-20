package contato.app.domain.model

/**
 * O que trafega entre os dois celulares pelo Nearby.
 *
 * Antes, só texto puro ia de um lado para o outro. Agora o Surdocego também
 * devolve confirmações, para o Comunicador saber o que está acontecendo:
 * recebida, vibrando a letra X, terminou.
 */
sealed interface WireMessage {

    /** Mensagem de texto. [id] liga as confirmações à mensagem certa. */
    data class Text(val id: String, val text: String) : WireMessage

    /** O celular do Surdocego recebeu a mensagem [id]. */
    data class Received(val id: String) : WireMessage

    /** O celular do Surdocego começou a vibrar a letra de posição [index] (texto normalizado). */
    data class Progress(val id: String, val index: Int) : WireMessage

    /** A vibração da mensagem [id] terminou. */
    data class Played(val id: String) : WireMessage
}
