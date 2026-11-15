# ConTato

Aplicativo Android de comunicação assistiva para pessoas com surdocegueira adquirida. O ConTato converte texto em vibração inspirada na célula Braille e permite que a pessoa com surdocegueira responda tocando numa célula Braille desenhada na tela.

Trabalho de Conclusão de Curso de Ciência da Computação da Universidade Paulista (UNIP), 2026.

> **Protótipo acadêmico.** O algoritmo e o aplicativo foram verificados por testes técnicos, mas ainda **não foram avaliados com pessoas com surdocegueira**. O protocolo dessa avaliação foi submetido ao Comitê de Ética em Pesquisa e está descrito no TCC. Não use o ConTato como único meio de comunicação em situações de risco ou emergência.

## Como funciona

- **Dois celulares, um para cada pessoa.** O modo **Comunicador** é um chat comum. O modo **Surdocego** recebe as mensagens por vibração e envia respostas pela célula Braille.
- **Texto em vibração.** Cada letra vira uma célula Braille de seis pontos, vibrados em ordem: vibração forte para ponto em relevo e fraca para ponto liso. Só as letras A–Z e o espaço vibram; acentos são removidos e números e pontuação são ignorados.
- **Resposta pelo toque.** A tela do modo Surdocego tem seis áreas de toque (pontos 1-2-3 à esquerda, 4-5-6 à direita) e uma área de gestos: toque confirma a letra, ← apaga, → insere espaço, ↑ envia e ↓ repete a última mensagem.
- **Sem internet.** Os aparelhos se conectam diretamente pela Nearby Connections (Bluetooth e Wi-Fi). A internet só é usada, de forma opcional, para login com Google e backup das conversas no Firebase.
- **Simplificação por IA (opcional).** Um modelo de linguagem pequeno, executado no próprio celular do Comunicador, sugere versões mais curtas das mensagens. Nada é enviado sem que o Comunicador toque em "Usar".

## Instalar o aplicativo pronto (APK)

1. Abra a página de [Releases](https://github.com/LeGodoy8bit/ConTato-App/releases) e baixe `ConTato-v1.0.apk` da versão **v1.0-tcc**.
2. Confira o arquivo comparando o SHA-256 com o publicado no Release (no Windows: `certutil -hashfile ConTato-v1.0.apk SHA256`).
3. Instale em **dois** celulares com Android 10 ou superior. O Android vai pedir permissão para instalar apps de fora da Play Store.
4. Em um celular, escolha o modo **Comunicador**. No outro, escolha o modo **Surdocego** mantendo o dedo pressionado na tela por 5 segundos.
5. Deixe Bluetooth e Wi-Fi ligados nos dois, aceite as permissões de dispositivos próximos e toque em **Conectar** no Comunicador.

O atalho de acessibilidade pelos botões de volume, que abre o modo Surdocego sem olhar a tela, exige Android 11 ou superior.

## Compilar a partir do código

### Requisitos

- Android Studio recente, com suporte ao Android Gradle Plugin 9.3.
- Pelo SDK Manager do Android Studio: Android SDK 36, **NDK** e **CMake 3.22.1**.
- Git.

### Passos

1. Clone o repositório:

   ```
   git clone https://github.com/LeGodoy8bit/ConTato-App.git
   cd ConTato-App
   ```

2. Baixe o **llama.cpp** na versão usada no projeto. Ele não está no repositório, mas é obrigatório para compilar (o CMake para com erro se não o encontrar):

   ```
   git clone https://github.com/ggml-org/llama.cpp third_party/llama.cpp
   git -C third_party/llama.cpp checkout b11860
   ```

3. (Opcional) Para usar a simplificação por IA, baixe `sentence_compressor.gguf` do [Release v1.0-tcc](https://github.com/LeGodoy8bit/ConTato-App/releases) e coloque em `app/src/main/assets/`. Sem o modelo, o aplicativo funciona normalmente e a IA aparece como indisponível.
4. Abra a pasta no Android Studio, espere o Gradle sincronizar e rode em um aparelho físico. Vibração e conexão não funcionam no emulador.

### Firebase

O arquivo `app/google-services.json` aponta para o projeto Firebase da equipe e só é usado no login com Google e no backup das conversas. Para usar outro projeto, crie um no Firebase com Authentication (Google) e Cloud Firestore e substitua o arquivo. As regras de segurança do Firestore (cada conta lê e grava apenas os próprios dados) estão descritas no TCC, seção 4.8.4.

## Testes

### Testes de unidade

A lógica principal (Braille, forma de onda da vibração, protocolo entre os aparelhos, máquinas de estado, junção de conversas, regras da IA e estimativas de tempo) fica na camada `domain`, em Kotlin puro, e é testada sem emulador:

```
./gradlew testDebugUnitTest
```

No Windows, use `gradlew.bat testDebugUnitTest`. O relatório fica em `app/build/reports/tests/testDebugUnitTest/index.html`.

### Teste de latência (CT24/CT25)

A medição só existe no build **debug**. No release, nada é medido nem gravado.

1. Instale o build debug nos dois aparelhos e conecte-os.
2. No celular do Comunicador, inicie o teste automático:

   ```
   adb shell am broadcast -a contato.app.debug.CT25_START -p contato.app
   ```

   Para parar: `adb shell am broadcast -a contato.app.debug.CT25_STOP -p contato.app`.
3. Copie os CSVs de cada aparelho (`Android/data/contato.app/files/ct_latencia_surdocego.csv` e `ct_latencia_comunicador.csv`) com `adb pull`.
4. Gere as estatísticas e os gráficos:

   ```
   pip install pandas matplotlib
   python scripts/analisar_latencia.py ct_latencia_surdocego.csv ct_latencia_comunicador.csv --saida resultados --limite 500
   ```

## Estrutura do projeto

```
app/src/main/java/contato/app/
├── presentation/   telas em Jetpack Compose e classes Session (estado de cada modo)
├── core/           vibração, conexão Nearby, IA nativa, orientação da tela, medição de latência
├── domain/         regras de negócio em Kotlin puro: Braille, vibração, protocolo, máquinas de estado
└── data/           SharedPreferences e Firebase
app/src/main/cpp/   núcleo C++ da IA sobre o llama.cpp, com ponte JNI
app/src/test/       testes de unidade
scripts/            análise do teste de latência
```

A pasta `docs/` descreve uma versão anterior do projeto. A documentação completa da versão final está no TCC.

## Versão do TCC

A versão avaliada no TCC é a tag **[v1.0-tcc](https://github.com/LeGodoy8bit/ConTato-App/releases/tag/v1.0-tcc)**. O Release dessa tag traz o APK, o modelo de IA, os hashes SHA-256 e os registros dos testes técnicos.

## Autores

Bruno Correa Santos, Caique Justino Deodato, Jheniffer Raymundo de Jesus, Letícia Silva Pinheiro de Godoy e Yoan Melnic Rodrigues.

Orientação: Profª Stephany Mendes Oliveira.

## Uso de inteligência artificial generativa

Ferramentas de IA generativa foram usadas como apoio no desenvolvimento e na documentação deste projeto, com revisão dos autores. O uso está detalhado na declaração anexa ao TCC.

## Direitos

© 2026 os autores. Todos os direitos reservados. O código é público para consulta e avaliação acadêmica; outros usos dependem de autorização dos autores.
