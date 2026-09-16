# Comunicação e Máquinas de Estado

## Comunicação P2P: NearbyConnectionService

`core/connection/NearbyConnectionService.kt` encapsula toda a interação
com a Nearby Connections API. Expõe quatro operações e reporta eventos
através da interface `NearbyListener`:

```kotlin
interface NearbyListener {
    fun onMessageReceived(message: String)
    fun onConnectionStatusChanged(status: String)      // texto livre, debug
    fun onConnectionStateChanged(state: ConnectionState) // estado formal
}
```

- `startAdvertising(userName)` — papel do **Comunicador**: anuncia o
  dispositivo para ser encontrado.
- `startDiscovery()` — papel do **Surdocego**: busca ativamente por um
  Comunicador.
- `sendMessage(text)` — envia bytes UTF-8 pelo canal já conectado. Não
  faz nada silenciosamente se não houver `connectedEndpointId` definido.
- `stop()` — encerra advertising, discovery e conexões ativas.

Estratégia usada: `Strategy.P2P_POINT_TO_POINT` (um único par de
dispositivos, adequado ao cenário Comunicador↔Surdocego). Identificador
de serviço: `"contato.app.BRAILLE"` — dispositivos com Bluetooth ligado
mas sem o app instalado nunca aparecem na busca, pois não anunciam esse
serviceId.

### Guarda contra chamadas duplicadas

`isSearchingOrConnected()` impede que `startAdvertising`/`startDiscovery`
sejam chamados enquanto uma operação já está em andamento — sem essa
guarda, o SDK rejeita a chamada duplicada com os códigos
`STATUS_ALREADY_ADVERTISING` (8001) / `STATUS_ALREADY_DISCOVERING`
(8002), e o tratamento genérico de erro fazia a máquina de estados
transicionar incorretamente para `CONNECTION_ERROR`.

## As três máquinas de estado

Todas seguem o mesmo padrão: um `enum` com os estados possíveis, e uma
classe `*StateMachine` com um mapa `Map<Estado, Set<Estado>>` de
transições válidas. Uma transição fora do mapa é **ignorada
silenciosamente** (`transitionTo` retorna `false`), nunca lança exceção.
Isso é proposital — o objetivo é impedir estados inconsistentes sem
derrubar o app.

### 1. ConnectionState (`domain/state/ConnectionStateMachine.kt`)

```
DISCONNECTED, SCANNING, DEVICE_FOUND, CONNECTING, CONNECTED,
CONNECTION_ERROR, RECONNECTING
```

**Ponto de atenção arquitetural importante:** esta máquina é compartilhada
pelos dois papéis do Nearby, que percorrem caminhos diferentes:

- Quem **busca** (Surdocego): `SCANNING → DEVICE_FOUND → CONNECTING → CONNECTED`
  (recebe `onEndpointFound` antes de solicitar conexão).
- Quem **anuncia** (Comunicador): `SCANNING → CONNECTING → CONNECTED`
  direto, pois só recebe `onConnectionInitiated`, sem nunca passar por
  `DEVICE_FOUND` (esse conceito só existe do lado de quem descobre
  ativamente).

Por isso `SCANNING` permite transição tanto para `DEVICE_FOUND` quanto
para `CONNECTING`. Um bug real de produção ocorreu quando essa segunda
transição não existia: o Comunicador ficava travado em `SCANNING` para
sempre (a transição para `CONNECTING`/`CONNECTED` era rejeitada
silenciosamente), o que deixava o botão de enviar mensagem sempre
desabilitado mesmo com a conexão Nearby de fato estabelecida.

### 2. ConversationState (`domain/state/ConversationStateMachine.kt`)

```
IDLE, COMPOSING, PROCESSING, SENDING, SENT, WAITING_RESPONSE,
RECEIVING, RECEIVED, ERROR
```

Regras notáveis:
- `COMPOSING`/`PROCESSING` podem ser cancelados, voltando a `IDLE`.
- Uma vez em `SENDING`, não há retorno direto a `IDLE` — só `SENT`
  (sucesso) ou `ERROR`, refletindo que a transmissão não deve ser
  interrompida no meio.
- Uma mensagem pode chegar (`RECEIVING`) tanto em repouso (`IDLE`)
  quanto enquanto se aguarda resposta (`WAITING_RESPONSE`).
- `ERROR` permite tentar novamente (`COMPOSING`) ou desistir (`IDLE`).

Usada em `CommunicatorActivity`, acionada a cada etapa de
`enviarMensagem()` e em `onMessageReceived()`.

### 3. DeafblindModeState (`domain/state/DeafblindModeStateMachine.kt`)

```
INITIALIZING, SEARCHING, CONNECTED, WAITING_MESSAGE, RECEIVING_MESSAGE,
REPEATING_MESSAGE, COMPOSING_BRAILLE, SENDING_MESSAGE, RETURNING_TO_WAIT
```

Modela o ciclo contínuo específico do Surdocego: entrada automática,
espera, recebimento, resposta (ou repetição) e retorno à espera. A
composição (`COMPOSING_BRAILLE`) só é alcançável a partir de
`RECEIVING_MESSAGE` — reflete a regra da especificação de que responder
é uma ação subsequente a receber, não uma ação livre a qualquer momento.
Perda de conexão retorna a `SEARCHING` a partir de qualquer estado
operacional (exceto `INITIALIZING`).

Usada em `DeafblindActivity`, integrada em `onConnectionStateChanged`
(entrada) e nos handlers da célula Braille (composição/envio).

## Feedback háptico de sistema

`domain/service/SystemHapticPatterns.getPulsesFor(SystemFeedbackEvent)`
centraliza os padrões de vibração de sistema, distintos do padrão
Braille:

| Evento | Padrão |
|---|---|
| `SEARCHING` | 1 vibração curta, intensidade normal |
| `DEVICE_FOUND` | 1 vibração curta, intensidade forte |
| `CONNECTING` | 2 vibrações curtas |
| `CONNECTED` | 2 vibrações longas |
| `CONNECTION_FAILED` / `ERROR` | 3 vibrações curtas |
| `MESSAGE_SENT` | 2 vibrações curtas |
| `MESSAGE_FINISHED` | 1 vibração longa |
| `COMMAND_RECOGNIZED` | 1 vibração curta (apagar/espaço/confirmar) |

`ConnectionFeedbackMapper.mapConnectionStateToFeedback(ConnectionState)`
traduz um `ConnectionState` no `SystemFeedbackEvent` correspondente
(retorna `null` para `DISCONNECTED`/`RECONNECTING`, que não geram
vibração própria). `SystemHapticWaveformBuilder.buildSystemWaveform()`
monta o `HapticWaveform` a partir da lista de `HapticPulse`, com
`SYSTEM_PULSE_GAP` (200ms) de silêncio entre pulsos.

**Testado manualmente e confirmado:** busca, dispositivo encontrado,
conectando, conectado, comando reconhecido, erro de combinação Braille
inválida. **Não testado de forma controlada:** falha real de conexão
(`CONNECTION_FAILED`) — tentativa de forçar desligando Wi-Fi não
funcionou, pois o sistema Android religa automaticamente o Wi-Fi quando
o Nearby precisa dele.
