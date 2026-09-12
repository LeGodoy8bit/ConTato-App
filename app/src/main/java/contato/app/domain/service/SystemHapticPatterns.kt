package contato.app.domain.service

import contato.app.core.config.HapticConfig
import contato.app.domain.model.HapticPulse
import contato.app.domain.model.SystemFeedbackEvent

/**
 * Centralizados aqui para que nenhuma tela precise "inventar" seu próprio padrão.
 */

/**
 * Define os padrões de vibração utilizados para fornecer
 * feedback sobre eventos do sistema.
 *
 * A classe centraliza as respostas táteis da interface,
 * evitando que cada tela implemente seus próprios padrões
 * de vibração.
 *
 * Os padrões definidos nesta classe são distintos da
 * codificação vibrotátil baseada em Braille, pois têm a
 * finalidade de informar estados e ações do aplicativo,
 * como conexão, envio de mensagem e ocorrência de erros.
 */


object SystemHapticPatterns {


    /**
     * Retorna o padrão de vibração correspondente ao evento
     * informado.
     *
     * @param event evento do sistema que deverá ser representado
     *              por feedback tátil.
     * @return lista de pulsos de vibração que representa o evento.
     */

    fun getPulsesFor(event: SystemFeedbackEvent): List<HapticPulse> {
        val short = HapticConfig.SYSTEM_SHORT_PULSE
        val long = HapticConfig.SYSTEM_LONG_PULSE
        val normal = HapticConfig.SYSTEM_NORMAL_AMPLITUDE
        val strong = HapticConfig.SYSTEM_STRONG_AMPLITUDE

        return when (event) {


            // Uma vibração curta indica que o sistema
            // está realizando a busca por um dispositivo.
            SystemFeedbackEvent.SEARCHING ->
                listOf(HapticPulse(short, normal))


            // Uma vibração curta de maior intensidade
            // indica que um dispositivo foi localizado.
            SystemFeedbackEvent.DEVICE_FOUND ->
                listOf(HapticPulse(short, strong))



            // Duas vibrações curtas representam a tentativa
            // de estabelecer uma conexão.
            SystemFeedbackEvent.CONNECTING ->
                listOf(HapticPulse(short, normal), HapticPulse(short, normal))



            // Duas vibrações longas indicam que a conexão
            // foi estabelecida com sucesso.
            SystemFeedbackEvent.CONNECTED ->
                listOf(HapticPulse(long, normal), HapticPulse(long, normal))


            // Três vibrações curtas representam uma condição
            // de falha ou erro durante a operação.
            SystemFeedbackEvent.CONNECTION_FAILED, SystemFeedbackEvent.ERROR ->
                listOf(HapticPulse(short, normal), HapticPulse(short, normal), HapticPulse(short, normal))



            // Duas vibrações curtas indicam que a mensagem
            // foi enviada.
            SystemFeedbackEvent.MESSAGE_SENT ->

                listOf(HapticPulse(short, normal), HapticPulse(short, normal))


            // Uma vibração longa indica o término da
            // transmissão ou reprodução da mensagem.
            SystemFeedbackEvent.MESSAGE_FINISHED ->
                listOf(HapticPulse(long, normal))
        }
    }
}