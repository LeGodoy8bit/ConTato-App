package contato.app.domain.model


/**
 * Representa os eventos do sistema que necessitam de
 * uma resposta tátil para informar o usuário surdocego.
 *
 * Cada evento possui um padrão de vibração correspondente,
 * definido pela classe SystemHapticPatterns.
 */


enum class SystemFeedbackEvent {

    SEARCHING, //O sistema está procurando um dispositivo
    DEVICE_FOUND, //Um dispositivo foi encontrado
    CONNECTING, //O sistema está tentando estabelecer conexão
    CONNECTED, //A conexão foi estabelecida
    CONNECTION_FAILED, //A tentativa de conexão falhou
    MESSAGE_SENT, //Uma mensagem foi enviada
    MESSAGE_FINISHED, //A reprodução/transmissão da mensagem terminou
    ERROR, //Ocorreu algum erro no sistema
    COMMAND_RECOGNIZED, // seção 3.9: cada comando (apagar, espaço, confirmar) precisa de feedback próprio
    SPLASH_TYPING_TICK,
    SPLASH_SYMBOL_FORMED


}