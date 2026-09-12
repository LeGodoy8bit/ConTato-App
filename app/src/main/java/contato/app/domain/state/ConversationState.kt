package contato.app.domain.state

enum class ConversationState {
    IDLE,
    COMPOSING,
    PROCESSING,
    SENDING,
    SENT,
    WAITING_RESPONSE,
    RECEIVING,
    RECEIVED,
    ERROR
}