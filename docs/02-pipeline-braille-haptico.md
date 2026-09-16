# Pipeline Braille → Háptico

Este é o núcleo conceitual do ConTato: a transformação de texto em
vibração, e de vibração (pontos pressionados) de volta em texto.

## Direção 1: Texto → Vibração (mensagem recebida)

```
String (mensagem)
    │
    ▼
BrailleMapper.map(Char)              → BrailleCharacter (6 pontos booleanos)
    │
    ▼
HapticPatternService.createPattern() → HapticPattern (6 HapticEvent)
    │
    ▼
HapticMessageService.createMessage() → HapticMessage (lista de HapticPattern,
    │                                    um por caractere, tratando espaço)
    ▼
HapticWaveformBuilder.buildMessageWaveform() → HapticWaveform (timings + amplitudes)
    │
    ▼
HapticController.playMessage()       → vibração real no aparelho
```

Toda essa cadeia é orquestrada por uma única fachada, para que nenhuma
tela precise conhecer os passos intermediários:

```kotlin
HapticMessagePlaybackService(context).play("CASA AZUL")
```

### Regras de negócio relevantes

- **Matriz Braille**: 6 pontos por caractere, representados como
  `List<Boolean>` na ordem P1, P2, P3, P4, P5, P6.
- **Limite de mensagem**: `HapticConfig.MAX_MESSAGE_LENGTH = 50`
  caracteres, validado em `HapticMessageService.createMessage()` via
  `require(...)`.
- **Espaço** é tratado como um `HapticPattern` com lista de eventos
  **vazia** — não é um "ponto inativo especial", é a ausência de pontos.
  Isso permite ao `HapticWaveformBuilder` diferenciar pausas entre letras
  (`CHARACTER_PAUSE`, 500ms) de pausas entre palavras (`SPACE_PAUSE`,
  1000ms) apenas checando `pattern.events.isEmpty()`.
- **Ponto ativo**: 250ms, amplitude 255. **Ponto inativo**: 140ms,
  amplitude 100 (não é silêncio total — a especificação original exige
  diferença tátil perceptível entre ativo e inativo, não ausência de
  sinal). Pausa entre pontos dentro da mesma letra: 80ms, sempre em
  amplitude 0.

Todas essas constantes vivem em `core/config/HapticConfig.kt` — nenhuma
tela ou serviço deve ter números mágicos de tempo/amplitude hardcoded.

## Direção 2: Vibração (toque) → Texto (composição do Surdocego)

```
6 toques na célula Braille virtual (List<Boolean>)
    │
    ▼
BrailleMapper.reverseMap(points)     → Char? (null se combinação inválida)
    │
    ▼
MessageBuilder.appendCharacter(char) → mantém o texto em construção
    │
    ▼
MessageBuilder.currentText           → String pronta para envio
```

`reverseMap` percorre a mesma tabela usada por `map()`, procurando a
entrada cujo valor bate com os pontos informados. Retorna `null` quando o
padrão não corresponde a nenhuma letra conhecida — o chamador
(`DeafblindActivity`) trata esse caso tocando o padrão de vibração de
erro, sem adicionar nada à mensagem.

`MessageBuilder` é uma classe de estado simples (usa `StringBuilder`
internamente) com quatro operações: `appendCharacter`, `appendSpace`,
`deleteLastCharacter`, `clear`. Está sujeita ao mesmo
`MAX_MESSAGE_LENGTH` usado na direção contrária.

## Vibração de sistema (paralela, não Braille)

Eventos como "buscando conexão", "conectado", "erro" têm um vocabulário
de vibração **totalmente separado** do Braille — ver
[Comunicação e Máquinas de Estado](03-comunicacao-e-estados.md) para os
padrões exatos.

## Classes desta camada, por responsabilidade

| Classe | Camada | Responsabilidade |
|---|---|---|
| `BrailleMapper` | domain/service | Char ↔ pontos (bidirecional) |
| `HapticPatternService` | domain/service | BrailleCharacter → HapticPattern |
| `HapticMessageService` | domain/service | String → HapticMessage (trata espaço/limite) |
| `MessageBuilder` | domain/service | Composição incremental de texto |
| `HapticWaveformBuilder` | domain/service | HapticPattern/HapticMessage → HapticWaveform |
| `HapticController` | core/vibration | HapticWaveform → `Vibrator` real |
| `HapticMessagePlaybackService` | core/vibration | Fachada: texto → vibração, em uma chamada |
