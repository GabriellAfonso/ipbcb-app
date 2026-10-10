package com.ipb.castelobranco.core.data.security

import com.ipb.castelobranco.core.testing.FakeAeadCipher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRecoveryTest {

    private val cipher = FakeAeadCipher()
    private val recovery = SessionRecovery(cipher)

    @Test
    fun `unreadable session destroys the key and asks for a wipe`() {
        recovery.onSessionUnreadable()

        assertEquals(1, cipher.destroyed)
        assertTrue(recovery.pendingWipe.value)
    }

    @Test
    fun `acknowledging lowers the flag`() {
        recovery.onSessionUnreadable()
        recovery.acknowledgeWipe()

        assertFalse(recovery.pendingWipe.value)
    }
}
