package contato.app.presentation.onboarding

/**
 * Textos legais e de ajuda do ConTato.
 *
 * ATENÇÃO: texto-base PROVISÓRIO, escrito a partir das práticas comuns de apps e da LGPD.
 * Os trechos marcados com [REVISAR] dependem de dados que só o grupo tem (nome da
 * instituição, e-mail, foro) e o conteúdo todo deve passar pelo orientador/jurídico.
 *
 * Ao mudar qualquer texto de verdade, altere TERMS_VERSION (TermsAcceptanceStore.kt)
 * para o app pedir o aceite de novo.
 */

internal sealed interface DocBlock {
    data class Paragraph(val text: String) : DocBlock
    data class Bullets(val items: List<String>) : DocBlock
}

internal class DocSection(val title: String, val blocks: List<DocBlock>)

internal class LegalDocument(val title: String, val intro: String, val sections: List<DocSection>)

private fun sec(title: String, vararg blocks: DocBlock) = DocSection(title, blocks.toList())
private fun p(text: String) = DocBlock.Paragraph(text)
private fun bl(vararg items: String) = DocBlock.Bullets(items.toList())

internal object LegalTexts {

    val termsOfUse = LegalDocument(
        title = "Termos de Uso",
        intro = "Bem-vindo(a) ao ConTato, um aplicativo de Tecnologia Assistiva desenvolvido para apoiar " +
                "a comunicação entre pessoas com surdocegueira e seus interlocutores por meio de comunicação " +
                "tátil e estímulos vibratórios.\n\nAo utilizar o ConTato, você declara que teve acesso a " +
                "estes Termos de Uso e concorda com as condições apresentadas a seguir.",
        sections = listOf(
            sec(
                "1. Sobre o ConTato",
                p("O ConTato é um aplicativo móvel desenvolvido com finalidade assistiva e acadêmica, voltado ao apoio da comunicação de pessoas com surdocegueira."),
                p("O sistema utiliza recursos de dispositivos móveis, comunicação entre dispositivos e estímulos táteis e vibratórios para representar informações de forma acessível."),
                p("A proposta do aplicativo é oferecer uma alternativa de comunicação que reduza a dependência de recursos exclusivamente visuais ou sonoros."),
            ),
            sec(
                "2. Finalidade do aplicativo",
                p("O ConTato tem como finalidade auxiliar a comunicação entre uma pessoa com surdocegueira e seu interlocutor."),
                p("O aplicativo não tem finalidade médica, terapêutica ou emergencial e não substitui:"),
                bl(
                    "profissionais especializados;",
                    "recursos oficiais de comunicação;",
                    "serviços de emergência;",
                    "dispositivos de Tecnologia Assistiva especializados;",
                    "acompanhamento profissional quando necessário.",
                ),
                p("O ConTato deve ser entendido como uma ferramenta de apoio à comunicação."),
            ),
            sec(
                "3. Como o ConTato funciona",
                p("O aplicativo pode utilizar dois dispositivos móveis para possibilitar a comunicação entre os usuários."),
                p("Dependendo da funcionalidade utilizada, uma mensagem inserida por um interlocutor poderá ser convertida em padrões de estímulos táteis e vibratórios apresentados ao outro dispositivo. Cada letra é representada por um padrão de vibração, com base no alfabeto Braille."),
                p("Os recursos efetivamente disponíveis podem variar de acordo com a versão do aplicativo, sistema operacional, modelo do dispositivo e recursos de hardware disponíveis. A intensidade e a qualidade da vibração dependem do motor de vibração de cada aparelho."),
            ),
            sec(
                "4. Usuários e modos de utilização",
                p("O ConTato possui diferentes modos de interação, desenvolvidos considerando as necessidades de seus usuários."),
                p("O modo Comunicador utiliza uma interface convencional para inserção e transmissão das mensagens."),
                p("O modo Surdocego é voltado à pessoa com surdocegueira, que recebe as mensagens por vibração e responde por uma célula Braille na tela. Ele é ativado ao manter a tela pressionada por 5 segundos."),
                p("Cada pessoa aceita estes Termos por si mesma. Uma pessoa de apoio pode ler os termos junto, mas a decisão de aceitar é de quem vai usar o aplicativo."),
                p("Modo de simplificação por IA (em desenvolvimento): poderá simplificar mensagens para reduzir a carga cognitiva de quem as recebe. Quando estiver disponível, será opcional, claramente informado e acompanhado de atualização da Política de Privacidade."),
            ),
            sec(
                "5. Permissões do dispositivo",
                p("Para funcionar, o ConTato solicita acesso a recursos do aparelho:"),
                bl(
                    "dispositivos próximos (Bluetooth e Wi-Fi), para encontrar e conectar os dois celulares;",
                    "localização, apenas em versões do Android que exigem essa permissão para a busca por dispositivos próximos. O ConTato não usa a sua localização para outras finalidades;",
                    "vibração, para apresentar as mensagens e os avisos do sistema.",
                ),
                p("Você pode revogar as permissões nas configurações do aparelho, mas o aplicativo pode deixar de funcionar."),
            ),
            sec(
                "6. Responsabilidades do usuário",
                p("Ao usar o ConTato, você se compromete a:"),
                bl(
                    "usar o aplicativo de forma lícita e respeitosa;",
                    "não utilizá-lo para assediar, ameaçar ou prejudicar outras pessoas;",
                    "conectar-se apenas a dispositivos de pessoas com quem deseja se comunicar;",
                    "não tentar adulterar, copiar indevidamente ou comprometer o funcionamento do aplicativo.",
                ),
            ),
            sec(
                "7. Limitações e disponibilidade",
                p("O ConTato é um projeto acadêmico em desenvolvimento e é oferecido no estado em que se encontra. Podem ocorrer falhas de conexão, atrasos, interrupções ou diferenças na vibração entre aparelhos."),
                p("Por isso, não utilize o aplicativo como único meio de comunicação em situações de risco, urgência ou emergência."),
            ),
            sec(
                "8. Propriedade intelectual",
                p("O nome ConTato, o logotipo, o design e o código do aplicativo pertencem aos seus desenvolvedores [REVISAR: nome do grupo/instituição]. É proibido reproduzi-los sem autorização, salvo nos termos de eventual licença informada."),
            ),
            sec(
                "9. Privacidade",
                p("O tratamento de dados pessoais é descrito na Política de Privacidade, que faz parte destes Termos."),
            ),
            sec(
                "10. Alterações destes Termos",
                p("Estes Termos podem ser atualizados. Quando houver mudança relevante, o aplicativo pedirá um novo aceite antes de continuar."),
            ),
            sec(
                "11. Contato e legislação",
                p("Dúvidas, sugestões e solicitações podem ser enviadas para leticia.godoy9@aluno.unip.br."),
                p("Estes Termos são regidos pela legislação brasileira. Fica eleito o foro de São Paulo (SP) para resolver eventuais conflitos, ressalvados os direitos do consumidor."),
            ),
        ),
    )

    val privacyPolicy = LegalDocument(
        title = "Política de Privacidade",
        intro = "Esta Política explica, de forma simples, quais informações o ConTato trata, para que " +
                "servem e quais são os seus direitos, em conformidade com a Lei Geral de Proteção de Dados " +
                "(LGPD, Lei nº 13.709/2018).",
        sections = listOf(
            sec(
                "1. Quem é o responsável",
                p("O responsável pelo tratamento dos dados é time ConTato Assitive Tecnology Contato: leticia.godoy9@aluno.unip.br."),
            ),
            sec(
                "2. Quais dados o ConTato trata",
                p("O ConTato foi pensado para tratar o mínimo possível de dados. Não pedimos nome, e-mail, telefone, documentos nem criamos conta."),
                bl(
                    "Modo escolhido (Comunicador ou Surdocego): guardado apenas no seu aparelho, para lembrar sua escolha.",
                    "Registro de aceite: versão dos termos aceitos e data, guardados apenas no seu aparelho.",
                    "Mensagens: enviadas diretamente de um celular para o outro por conexão entre dispositivos próximos. Ficam na memória do aplicativo durante a conversa e não são enviadas a servidores do ConTato.",
                    "Identificador temporário de conexão (como o nome \"Comunicador\"): usado só para encontrar o outro aparelho.",
                ),
                p("O ConTato não coleta dados de localização, contatos, fotos, microfone ou câmera."),
            ),
            sec(
                "3. Para que usamos e base legal",
                p("Usamos essas informações para fazer o aplicativo funcionar: conectar os dois aparelhos, transmitir as mensagens, apresentar as vibrações e lembrar o modo escolhido."),
                p("O tratamento se baseia no seu consentimento (LGPD, art. 7º, I), dado quando você aceita estes documentos, e na execução da funcionalidade solicitada. [REVISAR]"),
            ),
            sec(
                "4. Compartilhamento",
                p("Não vendemos nem compartilhamos dados com terceiros para publicidade."),
                p("As mensagens são entregues ao aparelho da pessoa com quem você escolheu conversar. A conexão entre os aparelhos usa o recurso Nearby Connections, do Android, da Google [REVISAR: detalhar proteções de criptografia]."),
                p("Se o modo de simplificação por IA for disponibilizado, o texto da mensagem poderá ser processado por um serviço de IA. Isso só acontecerá com aviso claro e consentimento específico."),
            ),
            sec(
                "5. Armazenamento e segurança",
                p("As preferências ficam no aparelho e podem ser apagadas a qualquer momento, limpando os dados do aplicativo nas configurações do Android ou desinstalando-o."),
                p("Adotamos medidas técnicas razoáveis para proteger as informações, mas nenhum sistema é totalmente livre de riscos."),
            ),
            sec(
                "6. Por quanto tempo guardamos",
                p("Mensagens: apenas enquanto a conversa estiver aberta no aplicativo. Preferências e registro de aceite: até você limpar os dados ou desinstalar o aplicativo."),
            ),
            sec(
                "7. Seus direitos",
                p("Conforme o art. 18 da LGPD, você pode solicitar:"),
                bl(
                    "confirmação de que tratamos seus dados e acesso a eles;",
                    "correção de dados incompletos ou desatualizados;",
                    "eliminação de dados tratados com base no consentimento;",
                    "informação sobre com quem os dados são compartilhados;",
                    "revogação do consentimento, a qualquer momento.",
                ),
                p("Para exercer seus direitos, entre em contato pelo e-mail informado acima."),
            ),
            sec(
                "8. Acessibilidade e consentimento",
                p("O consentimento deve ser dado pela própria pessoa usuária. Quem tem surdocegueira pode contar com o apoio de alguém de confiança para ler e entender estes documentos, mas a decisão de aceitar é sua. Os textos podem ser lidos com leitor de tela e display Braille."),
            ),
            sec(
                "9. Crianças e adolescentes",
                p("O uso por menores de 18 anos deve ter o acompanhamento e o consentimento de um responsável legal (LGPD, art. 14)."),
            ),
            sec(
                "10. Alterações desta Política",
                p("Esta Política pode ser atualizada. Mudanças relevantes serão informadas no aplicativo e poderão exigir novo aceite."),
            ),
        ),
    )
}