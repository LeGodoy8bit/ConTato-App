package contato.app.domain.state

enum class DeafblindModeState {
    INITIALIZING,
    SEARCHING,
    CONNECTED,
    WAITING_MESSAGE,
    RECEIVING_MESSAGE,
    REPEATING_MESSAGE,
    COMPOSING_BRAILLE,
    SENDING_MESSAGE,
    RETURNING_TO_WAIT
}