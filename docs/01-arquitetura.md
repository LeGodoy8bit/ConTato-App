# Arquitetura

## Visão geral

O ConTato é organizado em quatro camadas, cada uma com uma responsabilidade
clara e uma regra rígida sobre o que ela pode ou não conhecer:

```
presentation/   → Activities. Sabe desenhar telas e reagir a toques.
core/           → Infraestrutura Android (vibração, conexão P2P).
domain/         → Regras de negócio em Kotlin puro. NUNCA importa Android.
data/           → Persistência (hoje: SharedPreferences).
```

A regra mais importante do projeto é esta: **o pacote `domain` nunca
importa nada de `android.*` ou de bibliotecas externas como o Google Play
Services.** Isso não é um capricho estético — é o que permite testar toda
a lógica de negócio (mapeamento Braille, máquinas de estado, construção de
mensagem) com JUnit puro, sem emulador, sem mock de `Context` ou
`Vibrator`. Sempre que uma classe nova precisar tocar em algo do Android,
ela pertence a `core` ou `presentation`, não a `domain`.

## Estrutura de pastas atual

```
contato.app/
├── core/
│   ├── config/
│   │   └── HapticConfig.kt              (todas as constantes de tempo/amplitude)
│   ├── connection/
│   │   ├── NearbyConnectionService.kt   (+ interface NearbyListener)
│   │   └── NearbyPermissions.kt
│   └── vibration/
│       ├── HapticController.kt
│       └── HapticMessagePlaybackService.kt   (fachada)
├── data/
│   ├── UserModePreferencesRepository.kt
│   └── InMemoryConversationHistoryRepository.kt
├── domain/
│   ├── model/
│   │   ├── BrailleCharacter.kt
│   │   ├── HapticEvent.kt, HapticEventType.kt
│   │   ├── HapticPattern.kt, HapticMessage.kt, HapticWaveform.kt
│   │   ├── HapticPulse.kt, SystemFeedbackEvent.kt
│   │   ├── UserMode.kt
│   │   └── Message.kt, MessageStatus.kt
│   ├── repository/
│   │   └── UserModeRepository.kt (interface)
│   ├── service/
│   │   ├── BrailleMapper.kt
│   │   ├── HapticPatternService.kt
│   │   ├── HapticMessageService.kt
│   │   ├── HapticWaveformBuilder.kt
│   │   ├── SystemHapticPatterns.kt
│   │   ├── SystemHapticWaveformBuilder.kt
│   │   ├── ConnectionFeedbackMapper.kt
│   │   └── MessageBuilder.kt
│   └── state/
│       ├── ConnectionState.kt + ConnectionStateMachine.kt
│       ├── ConversationState.kt + ConversationStateMachine.kt
│       └── DeafblindModeState.kt + DeafblindModeStateMachine.kt
└── presentation/
    ├── ModeSelectionActivity.kt   (LAUNCHER)
    ├── DeafblindActivity.kt      (tela real do Surdocego)
    ├── CommunicatorActivity.kt   (tela real do Comunicador)
    ├── MessageAdapter.kt
    ├── MainActivity.kt           (bancada de teste de vibração)
    └── NearbyDebugActivity.kt    (bancada de teste de Nearby)
```

## Decisões técnicas relevantes (e por quê)

### Nearby Connections em vez de BLE puro

A especificação formal do TCC pede Bluetooth Low Energy (BLE) como
tecnologia de comunicação. O projeto usa, na prática, a **Nearby
Connections API** do Google Play Services
(`com.google.android.gms:play-services-nearby`).

Isso é uma decisão consciente, não um desvio acidental. Razões:

- Nearby Connections abstrai a escolha entre BLE, Bluetooth clássico e
  Wi-Fi Direct, escolhendo automaticamente a melhor combinação disponível
  — e continua funcionando **totalmente offline**, sem depender de
  internet ou servidor.
- Implementar BLE puro exigiria definir manualmente GATT services,
  characteristics, UUIDs e lidar com MTU — trabalho considerável sem
  relação direta com o problema central do projeto.
- A biblioteca ainda **usa BLE internamente** em parte do processo de
  descoberta, então não é uma substituição completa do conceito, apenas
  uma camada de abstração sobre ele.

**Ressalva a documentar na banca:** a versão da biblioteca está fixada em
`19.0.0`. A versão `19.5.0` foi testada e quebra a compilação, pois foi
compilada com metadados do Kotlin incompatíveis com a versão de Kotlin
embutida no AGP do projeto (ver seção de Build abaixo).

### Terminologia: "Comunicador" e "Surdocego"

A especificação formal usa os termos oficiais "Comunicador" (pessoa que
interage com o usuário surdocego — não necessariamente um intérprete
profissional) e "Surdocego". O código usa `UserMode.COMMUNICATOR` e
`UserMode.DEAFBLIND` consistentemente. Termos anteriores como "Guia" ou
"Interlocutor", usados em versões iniciais do código, foram descontinuados.

### Duas Activities por modo, nunca uma só com condicionais

Cada modo de uso (Comunicador, Surdocego) tem sua própria Activity e seu
próprio layout XML. Colocar as duas experiências dentro de uma única
Activity controlada por `if`s (padrão conhecido como "God Activity") foi
deliberadamente evitado: os dois modos têm ciclos de vida, gestos e
necessidades de interface completamente diferentes.

### Nomenclatura de funções: nunca sobrecarga genérica

O projeto evita deliberadamente nomes de método genéricos e sobrecarga
(`build()`, `map()`, `patternFor()`) em favor de nomes específicos que
dizem o que a função faz e de onde ela vem, mesmo que isso signifique
nomes mais longos: `buildCharacterWaveform()`, `buildMessageWaveform()`,
`mapConnectionStateToFeedback()`, `getPulsesFor()`. Isso é uma convenção
de equipe, mantida conscientemente mesmo quando o Kotlin permitiria
sobrecarga.

### Duas vias de vibração, propositalmente separadas

Existem dois "vocabulários" de vibração inteiramente distintos no
sistema, e eles nunca compartilham modelo ou lógica de montagem:

| | Representa | Modelo | Builder |
|---|---|---|---|
| Padrão Braille | Letras de uma mensagem | `HapticPattern` / `HapticEvent` | `HapticWaveformBuilder` |
| Feedback de sistema | Eventos (conectado, erro, etc.) | `HapticPulse` | `SystemHapticWaveformBuilder` |

Os dois convergem apenas no `HapticController`, que sabe tocar qualquer
`HapticWaveform` independente de qual builder o gerou.

## Notas de build

- **AGP 9.3.1** traz um compilador Kotlin embutido (versão 2.2.0) — o
  projeto não declara plugin Kotlin separadamente no
  `libs.versions.toml`. Adicionar dependências compiladas com uma versão
  de Kotlin mais nova quebra `compileDebugKotlin`.
- `play-services-nearby` deve permanecer em `19.0.0` até que essa
  restrição seja revisada.
