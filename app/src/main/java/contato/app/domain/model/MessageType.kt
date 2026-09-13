package contato.app.domain.model

enum class MessageType {
    TEXT
}

/**
 * Representa o tipo de conteúdo da mensagem.
 * Hoje só existe TEXT, mas fica preparado pra crescer sem quebrar
 * o resto do código (ex: futuramente um tipo SYSTEM para avisos
 * automáticos do próprio app, como "Conexão estabelecida").
 */