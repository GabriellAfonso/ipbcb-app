package com.ipb.castelobranco.core.domain.push

import com.ipb.castelobranco.core.domain.auth.SessionPresenceProvider
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.testing.FakeDevicesRepository
import com.ipb.castelobranco.core.testing.FakePushRegistrationScheduler
import com.ipb.castelobranco.core.testing.FakeRegisteredTokenStore
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceRegistrationUseCasesTest {

    private val devices = FakeDevicesRepository()
    private val store = FakeRegisteredTokenStore()
    private val scheduler = FakePushRegistrationScheduler()

    private fun register(loggedIn: Boolean = true, token: String? = "token-1") = RegisterDeviceUseCase(
        sessionPresence = SessionPresenceProvider { loggedIn },
        tokenSource = PushTokenSource { token },
        devices = devices,
        store = store,
    )

    @Test
    fun `a logged-in device sends its token and remembers it`() = runTest {
        assertEquals(RegistrationOutcome.DONE, register()())

        assertEquals(listOf("token-1"), devices.registered)
        assertEquals("token-1", store.token)
    }

    @Test
    fun `a logged-out device sends nothing`() = runTest {
        assertEquals(RegistrationOutcome.DONE, register(loggedIn = false)())
        assertTrue(devices.registered.isEmpty())
    }

    @Test
    fun `no token from the provider sends nothing`() = runTest {
        assertEquals(RegistrationOutcome.DONE, register(token = null)())
        assertTrue(devices.registered.isEmpty())
    }

    @Test
    fun `a network failure asks for a retry`() = runTest {
        devices.registerResult = Result.failure(AppError.Network())

        assertEquals(RegistrationOutcome.RETRY, register()())
        assertNull(store.token)
    }

    @Test
    fun `a refused token fails without retry`() = runTest {
        devices.registerResult = Result.failure(AppError.Server(code = 400))

        assertEquals(RegistrationOutcome.FAILED, register()())
    }

    @Test
    fun `unregister forgets the stored token and cancels pending registration`() = runTest {
        store.token = "token-1"

        UnregisterDeviceUseCase(scheduler, devices, store)()

        assertEquals(listOf("token-1"), devices.unregistered)
        assertEquals(1, scheduler.cancelCalls)
        assertNull(store.token)
    }

    @Test
    fun `a failed unregister still clears the token`() = runTest {
        store.token = "token-1"
        devices.unregisterResult = Result.failure(AppError.Network())

        UnregisterDeviceUseCase(scheduler, devices, store)()

        assertNull(store.token)
    }

    @Test
    fun `an unregister that never answers gives up after the timeout`() = runTest {
        store.token = "token-1"
        devices.beforeUnregister = { awaitCancellation() }

        UnregisterDeviceUseCase(scheduler, devices, store)()

        assertNull(store.token)
        assertTrue(testScheduler.currentTime >= UnregisterDeviceUseCase.UNREGISTER_TIMEOUT_MS)
    }

    @Test
    fun `nothing registered, nothing to unregister`() = runTest {
        UnregisterDeviceUseCase(scheduler, devices, store)()

        assertTrue(devices.unregistered.isEmpty())
    }
}
