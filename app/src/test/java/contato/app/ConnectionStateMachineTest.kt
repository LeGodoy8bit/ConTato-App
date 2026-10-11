package contato.app

import contato.app.domain.state.ConnectionState
import contato.app.domain.state.ConnectionStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateMachineTest {

    @Test
    fun `deve iniciar no estado DISCONNECTED por padrao`() {
        val machine = ConnectionStateMachine()
        assertEquals(ConnectionState.DISCONNECTED, machine.currentState)
    }

    @Test
    fun `deve permitir transicao valida de DISCONNECTED para SCANNING`() {
        val machine = ConnectionStateMachine()

        val result = machine.transitionTo(ConnectionState.SCANNING)

        assertTrue(result)
        assertEquals(ConnectionState.SCANNING, machine.currentState)
    }

    @Test
    fun `deve ignorar transicao invalida de DISCONNECTED para CONNECTED`() {
        val machine = ConnectionStateMachine()

        val result = machine.transitionTo(ConnectionState.CONNECTED)

        assertFalse(result)
        assertEquals(ConnectionState.DISCONNECTED, machine.currentState)
    }

    @Test
    fun `deve percorrer o fluxo completo de conexao com sucesso`() {
        val machine = ConnectionStateMachine()

        assertTrue(machine.transitionTo(ConnectionState.SCANNING))
        assertTrue(machine.transitionTo(ConnectionState.DEVICE_FOUND))
        assertTrue(machine.transitionTo(ConnectionState.CONNECTING))
        assertTrue(machine.transitionTo(ConnectionState.CONNECTED))

        assertEquals(ConnectionState.CONNECTED, machine.currentState)
    }

    @Test
    fun `deve notificar o listener quando uma transicao valida ocorre`() {
        val machine = ConnectionStateMachine()
        var notifiedState: ConnectionState? = null

        machine.setOnStateChangedListener { newState ->
            notifiedState = newState
        }

        machine.transitionTo(ConnectionState.SCANNING)

        assertEquals(ConnectionState.SCANNING, notifiedState)
    }

    @Test
    fun `nao deve notificar o listener quando a transicao e invalida`() {
        val machine = ConnectionStateMachine()
        var notifiedState: ConnectionState? = null

        machine.setOnStateChangedListener { newState ->
            notifiedState = newState
        }

        machine.transitionTo(ConnectionState.CONNECTED) // inválida a partir de DISCONNECTED

        assertEquals(null, notifiedState)
    }

    @Test
    fun `deve permitir ir direto de SCANNING para CONNECTING (papel de quem anuncia)`() {
        val machine = ConnectionStateMachine()

        machine.transitionTo(ConnectionState.SCANNING)
        val result = machine.transitionTo(ConnectionState.CONNECTING)

        assertTrue(result)
        assertEquals(ConnectionState.CONNECTING, machine.currentState)
    }
}


