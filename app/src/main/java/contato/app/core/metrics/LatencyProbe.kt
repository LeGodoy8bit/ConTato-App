package contato.app.core.metrics

/**
 * Pontos de medição do RNF02 (casos de teste CT24/CT25): "a mensagem recebida deve
 * começar a vibrar em até 500 ms".
 *
 * Quem chama passa o instante já lido de System.nanoTime(), no ponto exato da medição,
 * para que o custo da própria medição não entre no tempo medido.
 *
 * - T1 (celular do Surdocego): chegada da mensagem T → logo antes do vibrator.vibrate.
 * - T2 (celular do Comunicador): envio da mensagem T → chegada da confirmação R.
 *
 * Cada tempo usa só o relógio do próprio aparelho: os relógios dos dois celulares
 * não são sincronizados, então nunca se subtrai um instante de um aparelho do outro.
 *
 * Em release o app usa [None], que não faz nada. Kotlin puro: testável com JUnit.
 */
interface LatencyProbe {

    /** Surdocego: a mensagem T [id], com [length] letras e espaços após a normalização, chegou. */
    fun textArrived(id: String, length: Int, atNanos: Long) {}

    /** Surdocego: logo antes do vibrator.vibrate com a forma de onda da mensagem. */
    fun beforeVibrate(atNanos: Long) {}

    /** Comunicador: a mensagem T [id] foi entregue ao Nearby para envio. */
    fun textSent(id: String, length: Int, atNanos: Long) {}

    /** Comunicador: chegou a confirmação R da mensagem [id]. */
    fun receiptArrived(id: String, atNanos: Long) {}

    /** Não mede nada (build release). */
    object None : LatencyProbe
}
