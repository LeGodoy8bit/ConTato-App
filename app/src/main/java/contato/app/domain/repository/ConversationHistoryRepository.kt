package contato.app.domain.repository

import contato.app.domain.model.Message

/**
 * Define o contrato para guardar e consultar o histórico de mensagens
 * de uma conversa. É uma interface (e não uma classe concreta) para
 * que, no futuro, a forma de guardar (memória, banco local, etc) possa
 * mudar sem afetar quem usa este repositório (ex: CommunicatorActivity).
 */
interface ConversationHistoryRepository {

    // Adiciona uma nova mensagem ao histórico.
    fun addMessage(message: Message)

    // Retorna todas as mensagens da conversa, na ordem em que foram
    // adicionadas (da mais antiga para a mais recente).
    fun getMessages(): List<Message>

    // Permite que outra parte do código (ex: a Activity) seja avisada
    // toda vez que uma mensagem nova entrar no histórico, para atualizar
    // a tela automaticamente.
    fun setOnMessageAddedListener(listener: (Message) -> Unit)
}

