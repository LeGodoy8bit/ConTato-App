package contato.app.domain.state

enum class ConnectionState {

    DISCONNECTED,
    SCANNING,
    DEVICE_FOUND,
    CONNECTING,
    CONNECTED,
    CONNECTION_ERROR,
    RECONNECTING
}
