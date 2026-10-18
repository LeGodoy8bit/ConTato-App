package contato.app.domain.model

/**
 * Como a splash deve ser exibida.
 *
 * FULL: animação completa (texto digitando e apagando, símbolo se formando).
 * Usada apenas na primeira abertura do app.
 *
 * SHORT: versão curta (símbolo já formado e a bola preenchendo a tela).
 * Usada nas aberturas seguintes, para não repetir ~vários segundos de
 * animação toda vez, o que seria cansativo e atrasaria quem reabre o app
 * com frequência (por exemplo, depois de a conexão cair).
 */
enum class SplashMode {
    FULL,
    SHORT
}
