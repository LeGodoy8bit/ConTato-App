package contato.app.domain.model

/**
 * Representa uma mensagem trocada entre o Comunicador e o Surdocego,
 * conforme a seção 2.3 da especificação do TCC.
 *
 * Cada mensagem enviada ou recebida vira uma instância desta classe,
 * que depois é guardada no histórico da conversa (lista exibida na
 * CommunicatorActivity, futuramente).
 */
data class Message(

    // Identificador único da mensagem. Serve pra diferenciar mensagens
    // mesmo que tenham o mesmo conteúdo (ex: duas pessoas mandando "Oi"
    // em momentos diferentes viram objetos diferentes por causa do id).
    val id: String,

    // O texto em si da mensagem (o que a pessoa digitou ou compôs em Braille).
    val content: String,

    // Quem enviou a mensagem: COMMUNICATOR ou DEAFBLIND (reaproveita o
    // enum UserMode que já existe no projeto, pra não duplicar conceito).
    val sender: UserMode,

    // Momento exato em que a mensagem foi criada, em milissegundos desde
    // 1º de janeiro de 1970 (padrão chamado "época Unix"). É um número
    // (ex: 1737654321000) em vez de um texto tipo "14:32" porque assim
    // fica fácil ORDENAR as mensagens por ordem cronológica e também
    // fácil CONVERTER esse número pra qualquer formato de hora/data na
    // tela depois, sem se preocupar com fuso horário no momento de salvar.
    // Pra gerar esse valor, usa-se: System.currentTimeMillis()
    val timestamp: Long,

    // Estado atual da mensagem no seu ciclo de vida: SENDING (ainda
    // enviando), SENT (o par confirmou recebimento via Nearby) ou
    // FAILED (deu erro na conexão durante o envio).
    val status: MessageStatus,

    // Tipo da mensagem. Por enquanto só existe TEXT (mensagem de texto
    // normal), mas o enum já fica pronto pra crescer no futuro sem
    // quebrar nada (ex: um tipo SYSTEM pra avisos automáticos no chat).
    val type: MessageType = MessageType.TEXT
)

