package com.ipb.castelobranco.core.domain.auth

import app.cash.turbine.test
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class AuthEventBusTest {

    private lateinit var bus: AuthEventBusImpl

    @Before
    fun setUp() {
        bus = AuthEventBusImpl()
    }

    @Test
    fun `emit LoginSuccess is received by subscriber`() = runTest {
        bus.events.test {
            bus.emit(AuthEventBus.Event.LoginSuccess)

            assertEquals(AuthEventBus.Event.LoginSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple events are received in emission order`() = runTest {
        bus.events.test {
            bus.emit(AuthEventBus.Event.LoginSuccess)
            bus.emit(AuthEventBus.Event.LoginSuccess)

            assertEquals(AuthEventBus.Event.LoginSuccess, awaitItem())
            assertEquals(AuthEventBus.Event.LoginSuccess, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `two independent subscribers both receive the same event`() = runTest {
        bus.events.test {
            val received = mutableListOf<AuthEventBus.Event>()
            val job = launch { bus.events.collect { received += it } }

            // Ensure second subscriber is actively collecting before emit
            testScheduler.advanceUntilIdle()

            bus.emit(AuthEventBus.Event.LoginSuccess)

            assertEquals(AuthEventBus.Event.LoginSuccess, awaitItem())
            testScheduler.advanceUntilIdle()
            assertEquals(listOf(AuthEventBus.Event.LoginSuccess), received)

            job.cancel()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `events property is not a MutableSharedFlow`() {
        assertFalse(bus.events is MutableSharedFlow)
    }
}
