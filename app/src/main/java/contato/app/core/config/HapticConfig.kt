package contato.app.core.config

object HapticConfig {

    // CONFIG PONTO ATIVO
    const val ACTIVE_DURATION = 250L
    const val ACTIVE_AMPLITUDE = 255


    // CONFIG PONTO INATIVO
    const val INACTIVE_DURATION = 140L
    const val INACTIVE_AMPLITUDE = 100

    // PAUSA ENTRE PONTOS E CARACTERES
    const val POINT_PAUSE = 80L
    const val CHARACTER_PAUSE = 500L

    const val SPACE_PAUSE = 1000L // PAUSA ENTRE PALAVRAS (quando encontrar um espaço)


    // PAUSA ENTRE LETRAS (dentro da mesma palavra)


    const val DEFAULT_AMPLITUDE = 128 //Intensidade padrão da vibração (inicial)

    const val MAX_MESSAGE_LENGTH = 50 //limite máximo da mensagem
}