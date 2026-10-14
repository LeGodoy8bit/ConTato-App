package contato.app.presentation.debug

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import contato.app.R
import contato.app.core.connection.NearbyConnectionService
import contato.app.core.connection.NearbyListener
import contato.app.core.connection.NearbyPermissions
import contato.app.core.vibration.HapticMessagePlaybackService
import contato.app.domain.service.ConnectionFeedbackMapper
import contato.app.domain.service.SystemHapticPatterns
import contato.app.domain.state.ConnectionState

/**
 * Tela de teste manual para a comunicação P2P via Nearby Connections.
 *
 * Papel "Comunicador": anuncia e envia mensagens de texto.
 * Papel "Surdocego": procura, recebe mensagens e sente a vibração correspondente.
 *
 * A conversão de texto para vibração acontece em onMessageReceived, via
 * HapticMessagePlaybackService — é aqui que a comunicação em rede se conecta
 * ao pipeline Braille/háptico já existente no domínio do app.
 *
 * Já a conversão de ESTADO DE CONEXÃO para vibração de sistema acontece em
 * onConnectionStateChanged, usando ConnectionFeedbackMapper + SystemHapticPatterns.
 * São dois "vocabulários" de vibração distintos: um representa letras (Braille),
 * o outro representa eventos do sistema (buscando, conectado, erro, etc.).
 */
class NearbyDebugActivity : AppCompatActivity(), NearbyListener {

    private lateinit var nearbyService: NearbyConnectionService
    private lateinit var hapticPlayback: HapticMessagePlaybackService

    private lateinit var tvStatus: TextView
    private lateinit var tvReceived: TextView
    private lateinit var etMessage: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nearby_debug)

        ActivityCompat.requestPermissions(this, NearbyPermissions.getRequiredPermissions(), 1)

        nearbyService = NearbyConnectionService(this, this)
        hapticPlayback = HapticMessagePlaybackService(this)

        tvStatus = findViewById(R.id.tvStatus)
        tvReceived = findViewById(R.id.tvReceivedMessage)
        etMessage = findViewById(R.id.etMessage)

        // Papel Comunicador: anuncia a presença do dispositivo para ser encontrado.
        findViewById<Button>(R.id.btnGuide).setOnClickListener {
            nearbyService.startAdvertising("Guia de Teste")
            tvStatus.text = "Status: Anunciando..."
        }

        // Papel Surdocego: procura ativamente por um dispositivo Comunicador.
        findViewById<Button>(R.id.btnDeafblind).setOnClickListener {
            nearbyService.startDiscovery()
            tvStatus.text = "Status: Procurando..."
        }

        // Envia o texto digitado para o dispositivo já conectado.
        findViewById<Button>(R.id.btnSend).setOnClickListener {
            val text = etMessage.text.toString()
            if (text.isNotEmpty()) {
                nearbyService.sendMessage(text)
                etMessage.text.clear()
                Toast.makeText(this, "Enviado!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Chamado quando uma mensagem de texto chega pela rede.
     * Mostra o texto na tela (debug visual) e dispara a vibração
     * correspondente à mensagem, via pipeline Braille/háptico.
     */
    override fun onMessageReceived(message: String) {
        runOnUiThread {
            tvReceived.text = message
        }
        hapticPlayback.play(message)
    }

    /**
     * Callback de texto livre, usado apenas para debug visual
     * (mensagens como "Conectado com: ...", "Erro na Conexão", etc.).
     * Não substitui onConnectionStateChanged, que carrega o estado formal.
     */
    override fun onConnectionStatusChanged(status: String) {
        runOnUiThread {
            tvStatus.text = "Status: $status"
        }
    }

    /**
     * Chamado sempre que a ConnectionStateMachine realiza uma transição válida.
     *
     * Além de atualizar a tela (útil para debug), traduz o estado formal em
     * um evento de feedback háptico (quando aplicável) e reproduz o padrão
     * de vibração correspondente — é isto que permite que o usuário Surdocego
     * acompanhe o progresso da conexão sem depender de texto na tela.
     */
    override fun onConnectionStateChanged(state: ConnectionState) {
        runOnUiThread {
            tvStatus.text = "Estado: $state"
        }

        ConnectionFeedbackMapper.mapConnectionStateToFeedback(state)?.let { feedbackEvent ->
            val pulses = SystemHapticPatterns.getPulsesFor(feedbackEvent)
            hapticPlayback.playSystemFeedback(pulses)
        }
    }

    /**
     * Interrompe qualquer vibração em andamento e encerra advertising/discovery/
     * conexões ativas quando a tela perde o foco, evitando que o app continue
     * consumindo rádio e vibrando em segundo plano.
     */
    override fun onPause() {
        super.onPause()
        hapticPlayback.stop()
        nearbyService.stop()
    }
}