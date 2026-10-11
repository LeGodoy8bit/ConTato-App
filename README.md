# Documentação Técnica — ConTato

TCC de Ciência da Computação (UNIP) — aplicativo Android assistivo para
comunicação com pessoas com surdocegueira adquirida, transformando
mensagens de texto em sequências vibrotáteis inspiradas no Braille.

Esta pasta documenta a arquitetura, as decisões técnicas e os fluxos
implementados até o momento. Ela deve ser atualizada junto com o código —
uma mudança de arquitetura sem atualização aqui é considerada incompleta.

## Índice

1. [Arquitetura](docs/01-arquitetura.md) — camadas, estrutura de pastas, e as
   principais decisões técnicas (Nearby Connections vs. BLE, nomenclatura,
   convenções de código).
2. [Pipeline Braille → Háptico](docs/02-pipeline-braille-haptico.md) — como uma
   letra vira vibração, do `BrailleMapper` ao `HapticController`.
3. [Comunicação e Máquinas de Estado](docs/03-comunicacao-e-estados.md) — como
   os dois dispositivos se conectam via Nearby Connections, e as três
   máquinas de estado que governam conexão, conversa e o ciclo do
   Surdocego.
4. [Telas e Fluxos de Usuário](docs/04-telas-e-fluxos.md) — o que cada Activity
   faz, para quem, e como as telas se conectam.
5. [Pendências e Trabalhos Futuros](docs/05-pendencias-e-trabalhos-futuros.md)
   — dívidas técnicas conhecidas, decisões adiadas de propósito, e
   possibilidades de pesquisa citáveis no TCC.

## Como ler esta documentação

Se você é alguém do grupo retomando o projeto depois de um tempo, ou a
orientadora avaliando a arquitetura: comece pelo documento 1
(Arquitetura) para entender o raciocínio geral, depois vá para o
documento 2 ou 3 dependendo da parte do sistema que precisa entender.

Se você é a próxima sessão de IA (Claude ou outra) dando continuidade ao
desenvolvimento: leia todos os cinco documentos nesta ordem antes de
escrever qualquer código novo. O documento 5 é o mais importante para
saber exatamente onde retomar.
