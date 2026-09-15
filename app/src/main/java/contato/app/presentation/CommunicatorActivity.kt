package contato.app.presentation

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import contato.app.R
import contato.app.core.connection.NearbyConnectionService
import contato.app.core.connection.NearbyListener
import contato.app.core.connection.NearbyPermissions
import contato.app.data.InMemoryConversationHistoryRepository
import contato.app.domain.model.Message
import contato.app.domain.model.MessageStatus
import contato.app.domain.model.UserMode
import contato.app.domain.state.ConnectionState
import contato.app.domain.state.ConversationState
import contato.app.domain.state.ConversationStateMachine
import java.util.UUID

/**
 * Tela real do Comunicador: permite digitar e enviar mensagens ao
 * Surdocego, e exibe o histórico da conversa (enviadas e recebidas).
 *
 * Implementa NearbyListener para reagir a três tipos de evento vindos
 * da camada de conexão: mensagem recebida, mudança de status "textual"
 * (para debug/feedback simples) e mudança de estado formal da conexão
 * (usado para habilitar/desabilitar o botão de enviar).
 */
class CommunicatorActivity : Activity(), NearbyListener {

    // Código usado para identificar a resposta do pedido de permissões
    // (o Android devolve esse mesmo código em onRequestPermissionsResult).
    private val permissionRequestCode = 1001

    // Limite de caracteres por mensagem, alinhado ao mesmo limite já
    // usado pelo HapticMessageService do lado do Surdocego.
    private val maxMessageLength = 50

    private lateinit var nearbyService: NearbyConnectionService
    private val conversationStateMachine = ConversationStateMachine()
    private val conversationHistory = InMemoryConversationHistoryRepository()
    private lateinit var messageAdapter: MessageAdapter

    private lateinit var textConnectionStatus: TextView
    private lateinit var recyclerMessages: RecyclerView
    private lateinit var editMessageInput: EditText
    private lateinit var buttonSend: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_communicator)

        textConnectionStatus = findViewById(R.id.textConnectionStatus)
        recyclerMessages = findViewById(R.id.recyclerMessages)
        editMessageInput = findViewById(R.id.editMessageInput)
        buttonSend = findViewById(R.id.buttonSend)

        setupMessageList()
        setupSendButton()

        // O NearbyConnectionService precisa de "this" como listener,
        // por isso só é criado depois que a Activity já existe.
        nearbyService = NearbyConnectionService(this, this)

        // Toda vez que uma mensagem entra no histórico (enviada ou
        // recebida), a lista na tela é atualizada automaticamente.
        conversationHistory.setOnMessageAddedListener {
            atualizarListaDeMensagens()
        }

        pedirPermissoesENecessarias()
    }

    /**
     * Configura o RecyclerView: define o layout (itens empilhados
     * verticalmente, do mais antigo no topo ao mais recente embaixo)
     * e conecta o MessageAdapter que criamos anteriormente.
     */
    private fun setupMessageList() {
        recyclerMessages.layoutManager = LinearLayoutManager(this)
        messageAdapter = MessageAdapter(currentUserMode = UserMode.COMMUNICATOR)
        recyclerMessages.adapter = messageAdapter
    }

    private fun setupSendButton() {
        buttonSend.setOnClickListener {
            val texto = editMessageInput.text.toString().trim()

            if (texto.isEmpty() || texto.length > maxMessageLength) {
                return@setOnClickListener
            }

            enviarMensagem(texto)
            editMessageInput.text.clear()
        }
    }

    /**
     * Pede as permissões necessárias para o Nearby funcionar. Assim como
     * na DeafblindActivity, startAdvertising() SÓ é chamado depois que o
     * usuário responde ao pedido de permissão (dentro de
     * onRequestPermissionsResult), nunca aqui direto — isso evita o bug
     * de "race condition" já corrigido anteriormente no projeto.
     */
    private fun pedirPermissoesENecessarias() {
        requestPermissions(
            NearbyPermissions.getRequiredPermissions(),
            permissionRequestCode
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionRequestCode) {
            val todasConcedidas = grantResults.isNotEmpty() &&
                    grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }

            if (todasConcedidas) {
                nearbyService.startAdvertising("Comunicador")
            } else {
                textConnectionStatus.text = "Permissões negadas — não é possível conectar"
            }
        }
    }

    /**
     * Monta e envia uma mensagem de texto ao Surdocego.
     *
     * Segue o ciclo de estados da conversa (IDLE -> COMPOSING -> PROCESSING
     * -> SENDING -> SENT -> WAITING_RESPONSE), conforme a ConversationStateMachine
     * já validada com testes JUnit.
     *
     * PENDÊNCIA CONHECIDA: o status SENT é aplicado de forma otimista, assim
     * que o envio é disparado — o Nearby não confirma entrega real neste
     * ponto (isso exigiria tratar onPayloadTransferUpdate, ainda não feito).
     */
    private fun enviarMensagem(conteudo: String) {
        conversationStateMachine.transitionTo(ConversationState.COMPOSING)
        conversationStateMachine.transitionTo(ConversationState.PROCESSING)
        conversationStateMachine.transitionTo(ConversationState.SENDING)

        val mensagem = Message(
            id = UUID.randomUUID().toString(),
            content = conteudo,
            sender = UserMode.COMMUNICATOR,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENDING
        )

        // Adiciona a mensagem já na tela (otimista), antes mesmo da
        // confirmação real de entrega — comportamento comum em apps de chat.
        conversationHistory.addMessage(mensagem)

        nearbyService.sendMessage(conteudo)

        conversationStateMachine.transitionTo(ConversationState.SENT)
        conversationStateMachine.transitionTo(ConversationState.WAITING_RESPONSE)
    }

    private fun atualizarListaDeMensagens() {
        messageAdapter.submitMessages(conversationHistory.getMessages())
        // Rola a lista automaticamente para a mensagem mais recente.
        recyclerMessages.scrollToPosition(conversationHistory.getMessages().size - 1)
    }

    // --- Implementação de NearbyListener ---

    override fun onMessageReceived(message: String) {
        conversationStateMachine.transitionTo(ConversationState.RECEIVING)

        val mensagem = Message(
            id = UUID.randomUUID().toString(),
            content = message,
            sender = UserMode.DEAFBLIND,
            timestamp = System.currentTimeMillis(),
            status = MessageStatus.SENT
        )

        conversationHistory.addMessage(mensagem)

        conversationStateMachine.transitionTo(ConversationState.RECEIVED)
        conversationStateMachine.transitionTo(ConversationState.IDLE)
    }

    override fun onConnectionStatusChanged(status: String) {
        textConnectionStatus.text = status
    }

    override fun onConnectionStateChanged(state: ConnectionState) {

        // O botão de enviar só fica ativo quando há, de fato, uma conexão
        // estabelecida com o Surdocego.
        android.util.Log.d("ConTato", "Estado de conexão recebido: $state")
        buttonSend.isEnabled = (state == ConnectionState.CONNECTED)
        android.util.Log.d("ConTato", "Botão enviar habilitado? ${buttonSend.isEnabled}")

    }
}