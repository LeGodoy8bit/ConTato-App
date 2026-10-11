package contato.app

import contato.app.domain.state.DeafblindModeState
import contato.app.domain.state.DeafblindModeStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeafblindModeStateMachineTest {

    @Test
    fun `deve iniciar no estado INITIALIZING por padrao`() {
        val machine = DeafblindModeStateMachine()
        assertEquals(DeafblindModeState.INITIALIZING, machine.currentState)
    }

    @Test
    fun `deve percorrer o fluxo de entrada ate WAITING_MESSAGE`() {
        val machine = DeafblindModeStateMachine()

        assertTrue(machine.transitionTo(DeafblindModeState.SEARCHING))
        assertTrue(machine.transitionTo(DeafblindModeState.CONNECTED))
        assertTrue(machine.transitionTo(DeafblindModeState.WAITING_MESSAGE))

        assertEquals(DeafblindModeState.WAITING_MESSAGE, machine.currentState)
    }

    @Test
    fun `deve percorrer o ciclo completo de recebimento e resposta`() {
        val machine = DeafblindModeStateMachine()

        machine.transitionTo(DeafblindModeState.SEARCHING)
        machine.transitionTo(DeafblindModeState.CONNECTED)
        machine.transitionTo(DeafblindModeState.WAITING_MESSAGE)
        machine.transitionTo(DeafblindModeState.RECEIVING_MESSAGE)

        assertTrue(machine.transitionTo(DeafblindModeState.COMPOSING_BRAILLE))
        assertTrue(machine.transitionTo(DeafblindModeState.SENDING_MESSAGE))
        assertTrue(machine.transitionTo(DeafblindModeState.RETURNING_TO_WAIT))
        assertTrue(machine.transitionTo(DeafblindModeState.WAITING_MESSAGE))

        assertEquals(DeafblindModeState.WAITING_MESSAGE, machine.currentState)
    }

    @Test
    fun `deve permitir repetir mensagem e retornar a espera`() {
        val machine = DeafblindModeStateMachine()

        machine.transitionTo(DeafblindModeState.SEARCHING)
        machine.transitionTo(DeafblindModeState.CONNECTED)
        machine.transitionTo(DeafblindModeState.WAITING_MESSAGE)
        machine.transitionTo(DeafblindModeState.RECEIVING_MESSAGE)

        assertTrue(machine.transitionTo(DeafblindModeState.REPEATING_MESSAGE))
        assertTrue(machine.transitionTo(DeafblindModeState.WAITING_MESSAGE))

        assertEquals(DeafblindModeState.WAITING_MESSAGE, machine.currentState)
    }

    @Test
    fun `nao deve permitir enviar mensagem sem antes compor`() {
        val machine = DeafblindModeStateMachine()

        machine.transitionTo(DeafblindModeState.SEARCHING)
        machine.transitionTo(DeafblindModeState.CONNECTED)
        machine.transitionTo(DeafblindModeState.WAITING_MESSAGE)

        val result = machine.transitionTo(DeafblindModeState.SENDING_MESSAGE)

        assertFalse(result)
        assertEquals(DeafblindModeState.WAITING_MESSAGE, machine.currentState)
    }

    @Test
    fun `deve permitir queda de conexao durante composicao, retornando a SEARCHING`() {
        val machine = DeafblindModeStateMachine()

        machine.transitionTo(DeafblindModeState.SEARCHING)
        machine.transitionTo(DeafblindModeState.CONNECTED)
        machine.transitionTo(DeafblindModeState.WAITING_MESSAGE)
        machine.transitionTo(DeafblindModeState.RECEIVING_MESSAGE)
        machine.transitionTo(DeafblindModeState.COMPOSING_BRAILLE)

        val result = machine.transitionTo(DeafblindModeState.SEARCHING)

        assertTrue(result)
        assertEquals(DeafblindModeState.SEARCHING, machine.currentState)
    }
}