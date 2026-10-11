# Telas e Fluxos de Usuário

## Mapa de Activities

| Activity | Papel | Status |
|---|---|---|
| `ModeSelectionActivity` | Primeira tela (LAUNCHER) | Real, final |
| `DeafblindActivity` | Tela do Surdocego | Real, MVP funcional |
| `CommunicatorActivity` | Tela do Comunicador (chat) | Real, MVP funcional |
| `MainActivity` | Bancada de teste de vibração | Debug apenas |
| `NearbyDebugActivity` | Bancada de teste de Nearby | Debug apenas |

As duas últimas **não fazem parte do fluxo real do usuário** — servem
apenas para testes manuais isolados durante o desenvolvimento.

## ModeSelectionActivity

Primeira tela que o app abre. Decide entre dois caminhos:

1. **Já existe um modo salvo** (`UserModeRepository.getUserMode()` não é
   `null`) → navega direto para a Activity correspondente, sem mostrar
   nenhuma UI de seleção.
2. **Primeira vez** → mostra um botão ("Sou o Comunicador") e reage a um
   **toque contínuo de 5 segundos em qualquer ponto da tela** para
   selecionar o modo Surdocego — decisão de acessibilidade: uma pessoa
   surdocega não consegue localizar visualmente um botão específico.

A escolha é persistida via `UserModePreferencesRepository`
(SharedPreferences) e nunca mais perguntada.

## DeafblindActivity

Tela real do usuário Surdocego. **Não possui nenhum botão de conexão** —
o fluxo é inteiramente automático:

1. `onCreate` pede permissões via `NearbyPermissions`.
2. `onRequestPermissionsResult` (nunca `onCreate` diretamente — ver nota
   sobre race condition abaixo) inicia `startDiscovery()` se as
   permissões foram concedidas.
3. `onConnectionStateChanged` traduz cada transição de estado em
   vibração de sistema, e ao atingir `CONNECTED` habilita a célula
   Braille (`DeafblindModeStateMachine` avança para `WAITING_MESSAGE`).
4. `onMessageReceived` toca a mensagem recebida (padrão Braille) e
   habilita a composição de resposta.
5. `onResume` reinicia `startDiscovery()` automaticamente se a conexão
   caiu enquanto o app estava em segundo plano — necessário porque o
   Android mata o processo quando o app é removido da lista de
   recentes, e sem isso o usuário precisaria limpar dados do app para
   reconectar.

### Célula Braille virtual

Seis botões (`btnPoint1`...`btnPoint6`, 90dp cada — acima do mínimo de
80dp da especificação), representando os pontos P1-P6. Cada toque
inverte o estado daquele ponto em `currentPoints`. Comandos separados:
Apagar, Espaço, Confirmar, Enviar.

**Confirmar** é o ponto de integração entre toque e domínio: pega
`currentPoints`, tenta `BrailleMapper.reverseMap()`; se encontrar um
caractere, adiciona via `MessageBuilder` e vibra confirmação; se não
encontrar, vibra o padrão de erro. Em ambos os casos, os pontos são
resetados para a próxima letra.

**Interface é provisória** — botões quadrados simples, sem o design
moderno planejado para a versão final.

### Nota de correção histórica: race condition de permissões

Uma versão anterior chamava `startDiscovery()` logo após
`requestPermissions()`, sem esperar resposta. Como o pedido de permissão
é assíncrono, isso causava erro `MISSING_PERMISSION` sempre que as
permissões ainda não haviam sido concedidas anteriormente. Corrigido
movendo `startDiscovery()` para dentro de `onRequestPermissionsResult`.

## CommunicatorActivity

Tela real do Comunicador: interface de chat com `RecyclerView` +
`MessageAdapter`, campo de texto (`EditText`, limite 50 caracteres) e
botão de enviar.

Fluxo de envio (`enviarMensagem`): percorre
`ConversationStateMachine` (`COMPOSING → PROCESSING → SENDING`), cria um
`Message`, adiciona ao histórico de forma otimista (antes de qualquer
confirmação real de entrega), chama `nearbyService.sendMessage()`, e
completa a máquina de estados (`SENT → WAITING_RESPONSE`).

`onConnectionStateChanged` habilita `buttonSend` apenas quando o estado
chega a `CONNECTED` — dependente da correção de simetria de estados
documentada em [03-comunicacao-e-estados.md](03-comunicacao-e-estados.md).

**Pendência de UX conhecida:** existe um botão "Reconectar" manual,
posicionado de forma provisória, usado apenas para debug — revisar
posição e necessidade (talvez reconexão automática, como no Surdocego)
na etapa de polimento visual.

## Message (domínio)

```kotlin
data class Message(
    val id: String,
    val content: String,
    val sender: UserMode,
    val timestamp: Long,
    val status: MessageStatus
)
```

Segue a estrutura da seção 2.7 da especificação (ID, CONTENT, SENDER,
TIMESTAMP, STATUS — o campo `TYPE` da especificação original, para
diferenciar texto/comando/confirmação/erro/sistema, ainda não foi
modelado; hoje toda mensagem é implicitamente do tipo texto).

`InMemoryConversationHistoryRepository` (`data/`) mantém a lista de
mensagens em memória (não persiste entre sessões) e notifica um listener
sempre que uma mensagem é adicionada, usado para atualizar o
`RecyclerView` automaticamente.
