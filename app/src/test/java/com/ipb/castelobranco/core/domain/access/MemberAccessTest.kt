package com.ipb.castelobranco.core.domain.access

import org.junit.Assert.assertEquals
import org.junit.Test

class MemberAccessTest {

    private fun access(status: AccessStatus, blocked: Boolean = false) =
        MemberAccess(hasAccess = false, status = status, requestsBlocked = blocked, correctionPending = false)

    @Test
    fun `no access and not blocked offers a request`() {
        assertEquals(AccessPrompt.CAN_REQUEST, accessPromptFor(access(AccessStatus.NONE)))
    }

    @Test
    fun `a pending request shows the pending text`() {
        assertEquals(AccessPrompt.PENDING, accessPromptFor(access(AccessStatus.PENDING)))
    }

    @Test
    fun `a block wins over every status`() {
        AccessStatus.entries.forEach { status ->
            assertEquals(AccessPrompt.BLOCKED, accessPromptFor(access(status, blocked = true)))
        }
    }

    @Test
    fun `granted and unknown offer nothing`() {
        assertEquals(AccessPrompt.NONE, accessPromptFor(access(AccessStatus.GRANTED)))
        assertEquals(AccessPrompt.NONE, accessPromptFor(access(AccessStatus.UNKNOWN)))
        assertEquals(AccessPrompt.NONE, accessPromptFor(MemberAccess.NONE))
    }

    @Test
    fun `wire values map to the status`() {
        assertEquals(AccessStatus.GRANTED, AccessStatus.fromWire("granted"))
        assertEquals(AccessStatus.PENDING, AccessStatus.fromWire("pending"))
        assertEquals(AccessStatus.NONE, AccessStatus.fromWire("none"))
        assertEquals(AccessStatus.UNKNOWN, AccessStatus.fromWire(null))
        assertEquals(AccessRequestType.DATA_CORRECTION, AccessRequestType.fromWire("data_correction"))
        assertEquals(null, AccessRequestType.fromWire("other"))
    }
}
