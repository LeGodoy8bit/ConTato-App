package contato.app.core.config

object HapticConfig {

    //PADRÃO BRAILLE (LETRAS E PALAVRAS)

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
    const val MAX_MESSAGE_LENGTH = 80 //limite máximo da mensagem (letras e espaços)




    // PULSOS DE VIBRAÇÃO DE SISTEMA (distintos do padrão Braille)
    const val SYSTEM_SHORT_PULSE = 150L
    const val SYSTEM_LONG_PULSE = 600L
    const val SYSTEM_PULSE_GAP = 200L
    const val SYSTEM_NORMAL_AMPLITUDE = 200
    const val SYSTEM_STRONG_AMPLITUDE = 255


    // VIBRAÇÃO DA SPLASH (toque por letra e pulso final)
    const val SPLASH_TICK_DURATION = 15L
    const val SPLASH_TICK_AMPLITUDE = 60
    const val SPLASH_FORMED_DURATION = 250L


    // TOQUE NOS PONTOS DA CELA (modo Surdocego): marcar = firme, desmarcar = leve.
    // Bem mais curtos que qualquer ponto Braille, para não serem confundidos com mensagem.
    const val DOT_ON_DURATION = 35L
    const val DOT_ON_AMPLITUDE = 255
    const val DOT_OFF_DURATION = 20L
    const val DOT_OFF_AMPLITUDE = 70



}