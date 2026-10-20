package contato.app.domain.model

/**
 * Ciclo de vida de uma mensagem enviada pelo Comunicador.
 *
 * A ordem importa: uma confirmação atrasada nunca "rebaixa" o status
 * (ex.: um RECEIVED que chega depois do primeiro PROGRESS é ignorado).
 *
 * SENDING   -> criada, ainda não saiu do aparelho
 * SENT      -> entregue ao Nearby, sem confirmação do outro lado
 * DELIVERED -> o celular do Surdocego confirmou o recebimento
 * PLAYING   -> o celular do Surdocego está vibrando a mensagem (letra a letra)
 * PLAYED    -> a vibração terminou
 * FAILED    -> a conexão caiu antes da confirmação
 */
enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    PLAYING,
    PLAYED,
    FAILED,
}
