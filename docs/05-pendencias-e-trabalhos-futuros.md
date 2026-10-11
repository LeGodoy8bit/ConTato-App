# Pendências e Trabalhos Futuros

Este documento é a lista viva de dívidas técnicas, decisões
deliberadamente adiadas, e possibilidades de pesquisa. Deve ser
atualizado a cada sessão de trabalho — item resolvido sai da lista (ou
move para um changelog, se o grupo preferir manter histórico).

## Dívidas técnicas conhecidas

1. **Pareamento seguro entre dispositivos.** Hoje, qualquer dispositivo
   ConTato encontrado nas proximidades é aceito automaticamente
   (`connectionsClient.acceptConnection` sem etapa de confirmação). Em
   um ambiente com múltiplos pares Comunicador/Surdocego usando o app
   simultaneamente (ex.: lugar público), o Surdocego de uma pessoa
   poderia conectar com o Comunicador de outra. Dispositivos Bluetooth
   genéricos (fones, outros celulares) **não** são um risco, pois o
   Nearby só descobre outros dispositivos rodando o mesmo `serviceId`.
   Solução proposta, não implementada: salvar o `endpointId` do parceiro
   após o primeiro pareamento (feito com apoio, como já previsto na
   especificação) e restringir conexões automáticas futuras a esse
   dispositivo específico.

2. **`onRequestPermissionsResult` ausente em `NearbyDebugActivity`.**
   Essa tela ainda usa `ActivityCompat.requestPermissions` sem esperar
   resposta antes de agir. Risco baixo, pois é uma tela de debug apenas,
   mas replicar a correção da `DeafblindActivity` caso essa tela evolua.

3. **Cenário de erro de conexão (`CONNECTION_FAILED`) não testado de
   forma controlada.** Tentativa de forçar desligando o Wi-Fi de um
   aparelho não funcionou — o Android religa o Wi-Fi automaticamente
   quando o Nearby precisa dele. Testar via Modo Avião em um dos
   aparelhos durante a busca, ou outro método mais confiável.

4. **Campo `TYPE` do modelo `Message` não implementado.** A
   especificação prevê diferenciação entre mensagem de
   texto/comando/confirmação/erro/sistema; hoje toda mensagem é
   implicitamente tratada como texto.

5. **Posicionamento do botão "Reconectar"** no Comunicador é provisório;
   revisar junto ao polimento visual. Considerar reconexão automática em
   vez de botão manual, por simetria com o Surdocego.

## Funcionalidades da especificação ainda não implementadas

- **Gestos de comando** (swipe left/right/down/up, long press) para
  apagar, espaço, enviar, repetir e confirmar — hoje tudo é feito por
  botões na célula Braille virtual. Parâmetros de gesto (distância
  mínima, duração, velocidade) ainda não têm valores definidos; a
  especificação prevê calibração durante testes de usabilidade.
- **Repetir última mensagem** (`DeafblindModeState.REPEATING_MESSAGE`) —
  estado já existe na máquina, mas nenhum gatilho (gesto ou botão) foi
  conectado a ele ainda.
- **Confirmação de entrega real** — o status `SENT` é aplicado de forma
  otimista assim que `sendMessage()` é chamado; o Nearby não confirma
  entrega neste ponto (exigiria tratar `onPayloadTransferUpdate`).

## Decisões deliberadamente adiadas (não são bugs)

- **Login Firebase**: apenas estrutura futura ("Fazer login (em breve)"
  em Configurações), sem implementação real. Decisão consciente: login é
  independente do fluxo principal e não deve ser implementado antes que
  a navegação básica esteja sólida (princípio YAGNI aplicado
  explicitamente nesta etapa do projeto).
- **Simplificação de texto por IA**: serviço próprio, desenvolvido por
  um colega do grupo, ainda em treinamento. Integração aguarda entrega
  do modelo/API.
- **Documentação/polimento visual final**: interfaces atuais (célula
  Braille, chat) são funcionais mas visualmente provisórias, por
  decisão explícita do grupo — MVP funcional primeiro, design
  intencional (Material Design 3, WCAG 2.2 AA) depois.

## Possibilidades de pesquisa (citáveis no TCC como trabalhos relacionados)

- **TalkBack + suporte nativo do Android a telas Braille físicas**
  (refreshable braille displays, antigo BrailleBack, hoje integrado ao
  TalkBack). Poderia, em tese, permitir que um usuário Surdocego
  configure permissões do sistema de forma autônoma, sem pessoa de
  apoio, usando gestos de navegação do TalkBack sobre qualquer tela do
  Android, incluindo popups de permissão. Ressalva importante a
  documentar: telas Braille físicas têm custo elevado e baixa
  disponibilidade — o ConTato se posiciona como alternativa mais
  acessível por depender apenas do motor de vibração já presente em
  qualquer smartphone.
