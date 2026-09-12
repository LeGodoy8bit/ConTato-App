package contato.app

import contato.app.domain.state.ConversationState
import contato.app.domain.state.ConversationStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationStateMachineTest {

    @Test
    fun `deve iniciar no estado IDLE por padrao`() {
        val machine = ConversationStateMachine()
        assertEquals(ConversationState.IDLE, machine.currentState)
    }

    @Test
    fun `deve percorrer o fluxo completo de envio com sucesso`() {
        val machine = ConversationStateMachine()

        assertTrue(machine.transitionTo(ConversationState.COMPOSING))
        assertTrue(machine.transitionTo(ConversationState.PROCESSING))
        assertTrue(machine.transitionTo(ConversationState.SENDING))
        assertTrue(machine.transitionTo(ConversationState.SENT))
        assertTrue(machine.transitionTo(ConversationState.WAITING_RESPONSE))
        assertTrue(machine.transitionTo(ConversationState.RECEIVING))
        assertTrue(machine.transitionTo(ConversationState.RECEIVED))
        assertTrue(machine.transitionTo(ConversationState.IDLE))

        assertEquals(ConversationState.IDLE, machine.currentState)
    }

    @Test
    fun `deve permitir cancelar durante COMPOSING`() {
        val machine = ConversationStateMachine()

        machine.transitionTo(ConversationState.COMPOSING)
        val result = machine.transitionTo(ConversationState.IDLE)

        assertTrue(result)
        assertEquals(ConversationState.IDLE, machine.currentState)
    }

    @Test
    fun `nao deve permitir cancelar diretamente durante SENDING`() {
        val machine = ConversationStateMachine()

        machine.transitionTo(ConversationState.COMPOSING)
        machine.transitionTo(ConversationState.PROCESSING)
        machine.transitionTo(ConversationState.SENDING)

        val result = machine.transitionTo(ConversationState.IDLE)

        assertFalse(result)
        assertEquals(ConversationState.SENDING, machine.currentState)
    }

    @Test
    fun `deve permitir tentar novamente apos ERROR`() {
        val machine = ConversationStateMachine()

        machine.transitionTo(ConversationState.COMPOSING)
        machine.transitionTo(ConversationState.PROCESSING)
        machine.transitionTo(ConversationState.SENDING)
        machine.transitionTo(ConversationState.ERROR)

        val result = machine.transitionTo(ConversationState.COMPOSING)

        assertTrue(result)
        assertEquals(ConversationState.COMPOSING, machine.currentState)
    }

    @Test
    fun `deve permitir receber mensagem estando em repouso (IDLE)`() {
        val machine = ConversationStateMachine()

        val result = machine.transitionTo(ConversationState.RECEIVING)

        assertTrue(result)
        assertEquals(ConversationState.RECEIVING, machine.currentState)
    }

    @Test
    fun `nao deve permitir transicao invalida de IDLE para SENT`() {
        val machine = ConversationStateMachine()

        val result = machine.transitionTo(ConversationState.SENT)

        assertFalse(result)
        assertEquals(ConversationState.IDLE, machine.currentState)
    }
}