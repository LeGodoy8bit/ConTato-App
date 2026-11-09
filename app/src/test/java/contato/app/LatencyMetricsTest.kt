package contato.app

import contato.app.core.metrics.LatencyRecorder
import contato.app.core.metrics.LatencySample
import contato.app.core.metrics.LatencyTestPlan
import contato.app.domain.service.BrailleTextNormalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LatencyMetricsTest {

    // ───────── Plano do teste (CT24/CT25) ─────────

    @Test
    fun textosTemOTamanhoPedidoESoLetrasEEspaco() {
        for (size in listOf(1, 10, 40, 79, 80)) {
            val text = LatencyTestPlan.textOf(size)
            assertEquals(size, text.length)
            assertTrue(text.all { it == ' ' || it in 'A'..'Z' })
        }
    }

    @Test
    fun normalizacaoNaoMudaOsTextosDoTeste() {
        for (size in 1..80) {
            val text = LatencyTestPlan.textOf(size)
            assertEquals(text, BrailleTextNormalizer.normalizeForBraille(text))
        }
    }

    @Test
    fun textosSaoReproduziveis() {
        assertEquals(LatencyTestPlan.textOf(40), LatencyTestPlan.textOf(40))
    }

    @Test(expected = IllegalArgumentException::class)
    fun recusaTamanhoAcimaDoLimite() {
        LatencyTestPlan.textOf(81)
    }

    @Test
    fun planoPadraoTem30EnviosPorTamanhoIntercalados() {
        val plan = LatencyTestPlan.build()
        assertEquals(90, plan.size)
        assertEquals(listOf(10, 40, 80), plan.take(3).map { it.tamanho })
        LatencyTestPlan.DEFAULT_SIZES.forEach { size ->
            assertEquals((1..30).toList(), plan.filter { it.tamanho == size }.map { it.execucao })
        }
        assertEquals(plan.size, plan.map { it.id }.toSet().size)
    }

    @Test
    fun execucaoVemDoIdDoTeste() {
        assertEquals(7, LatencyTestPlan.parseExecucao(LatencyTestPlan.idOf(40, 7)))
        assertNull(LatencyTestPlan.parseExecucao("3f2b8c1e-1d2a-4c5b-9e8f-0a1b2c3d4e5f"))
        assertNull(LatencyTestPlan.parseExecucao(""))
    }

    // ───────── Gravador ─────────

    private fun recorder(out: MutableList<LatencySample>) = LatencyRecorder("Pixel 8", "1.0") { out += it }

    @Test
    fun t1VaiDaChegadaAteAntesDeVibrar() {
        val out = mutableListOf<LatencySample>()
        val r = recorder(out)
        r.textArrived("ct25-10-3", 10, atNanos = 1_000_000_000L)
        r.beforeVibrate(atNanos = 1_012_500_000L)
        assertEquals(1, out.size)
        assertEquals(12.5, out[0].t1Ms!!, 1e-9)
        assertNull(out[0].t2Ms)
        assertEquals(3, out[0].execucao)
        assertEquals(10, out[0].tamanho)
    }

    @Test
    fun vibracaoSemMensagemPendenteNaoGeraMedicao() {
        val out = mutableListOf<LatencySample>()
        val r = recorder(out)
        r.beforeVibrate(5L) // treino, repetir, testar vibração
        r.textArrived("a", 5, 0L)
        r.beforeVibrate(1_000_000L)
        r.beforeVibrate(2_000_000L) // a mesma mensagem não fecha T1 duas vezes
        assertEquals(1, out.size)
    }

    @Test
    fun t2LigaEnvioEConfirmacaoPeloId() {
        val out = mutableListOf<LatencySample>()
        val r = recorder(out)
        r.textSent("ct25-40-1", 40, 0L)
        r.textSent("ct25-80-1", 80, 10_000_000L)
        r.receiptArrived("ct25-80-1", 40_000_000L)
        r.receiptArrived("ct25-40-1", 50_000_000L)
        r.receiptArrived("ct25-40-1", 60_000_000L) // confirmação repetida é ignorada
        r.receiptArrived("desconhecida", 70_000_000L)
        assertEquals(listOf(80 to 30.0, 40 to 50.0), out.map { it.tamanho to it.t2Ms!! })
        assertTrue(out.all { it.t1Ms == null })
    }

    @Test
    fun linhaCsvSegueOCabecalho() {
        assertEquals("execucao,tamanho_msg,T1_ms,T2_ms,aparelho,versao_app", LatencySample.CSV_HEADER)
        val line = LatencySample(2, 80, 123.4567, null, "SM-A155M", "1.0").toCsvLine()
        assertEquals("2,80,123.457,,SM-A155M,1.0", line)
        val manual = LatencySample(null, 10, null, 9.0, "Moto g, 5G", "1.0").toCsvLine()
        assertEquals(",10,,9.000,\"Moto g, 5G\",1.0", manual)
    }
}
