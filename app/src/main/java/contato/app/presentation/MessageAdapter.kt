package contato.app.presentation

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import contato.app.R
import contato.app.domain.model.Message
import contato.app.domain.model.UserMode

/**
 * Adapter do RecyclerView que exibe o histórico de mensagens do chat.
 *
 * "Adapter" é a peça que traduz uma lista de dados (aqui, List<Message>)
 * em itens visuais na tela. O RecyclerView não sabe nada sobre Message;
 * ele só sabe pedir ao Adapter: "quantos itens tem?", "desenha o item
 * número X pra mim". O Adapter é quem faz essa ponte.
 *
 * @param currentUserMode identifica se esta tela pertence ao Comunicador
 * ou ao Surdocego, para saber qual mensagem é "minha" (alinhada à direita)
 * e qual é "do outro lado" (alinhada à esquerda). Como o Adapter compara
 * message.sender com este valor, o mesmo Adapter pode futuramente ser
 * reaproveitado em outra tela de chat, se precisar.
 */
class MessageAdapter(
    private val currentUserMode: UserMode
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    // Lista interna de mensagens exibidas. Começa vazia; é atualizada
    // via submitMessages() sempre que uma mensagem nova entra no histórico.
    private var messages: List<Message> = emptyList()

    // Constantes que identificam os dois "tipos de item" que este Adapter
    // pode desenhar: balão de mensagem enviada ou recebida.
    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    /**
     * ViewHolder guarda as referências das Views de UM item da lista
     * (aqui, só o TextView do conteúdo), evitando que o RecyclerView
     * precise "procurar" essas Views de novo toda vez que rolar a tela.
     * Isso é o que torna o RecyclerView eficiente mesmo com listas grandes.
     */
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textContent: TextView = itemView.findViewById(R.id.textMessageContent)
    }

    // Chamado pelo RecyclerView para decidir, para cada posição da lista,
    // se deve desenhar o layout de "enviada" ou "recebida".
    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.sender == currentUserMode) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    // Chamado pelo RecyclerView quando precisa criar um novo item do zero
    // (infla o XML correspondente e embrulha num ViewHolder).
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_SENT) {
            R.layout.item_message_sent
        } else {
            R.layout.item_message_received
        }

        val view = LayoutInflater.from(parent.context)
            .inflate(layoutId, parent, false)

        return MessageViewHolder(view)
    }

    // Chamado pelo RecyclerView para preencher um item (já criado) com
    // os dados da mensagem daquela posição.
    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.textContent.text = message.content
    }

    // Diz ao RecyclerView quantos itens existem no total.
    override fun getItemCount(): Int = messages.size

    /**
     * Atualiza a lista de mensagens exibidas e redesenha a tela.
     * Chamado pela CommunicatorActivity toda vez que o histórico muda
     * (ex: quando uma mensagem nova é adicionada via
     * ConversationHistoryRepository).
     */
    fun submitMessages(newMessages: List<Message>) {
        messages = newMessages
        notifyDataSetChanged()
    }
}