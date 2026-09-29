package com.ipb.castelobranco.features.admin.members.presentation.util

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MembersFailureTest {

    private val denied = AppError.Auth(code = 403, userMessage = "Você não tem permissão para esta ação.")

    @Test
    fun `a refused read leaves the area`() {
        val event = denied.toMembersEvent(FailureKind.READ)

        assertEquals(MembersEvent.LeaveArea("Você não tem permissão para esta ação."), event)
    }

    @Test
    fun `a refused write only shows the message`() {
        val event = denied.toMembersEvent(FailureKind.WRITE)

        assertEquals(MembersEvent.ShowMessage("Você não tem permissão para esta ação."), event)
    }

    @Test
    fun `a vanished member goes back to the list either way`() {
        val gone = AppError.Server(code = 404, userMessage = "Este membro não existe mais")

        assertTrue(gone.toMembersEvent(FailureKind.READ) is MembersEvent.MemberGone)
        assertTrue(gone.toMembersEvent(FailureKind.WRITE) is MembersEvent.MemberGone)
    }

    @Test
    fun `a server error is a message`() {
        val event = AppError.Server(code = 500).toMembersEvent(FailureKind.READ)

        assertTrue(event is MembersEvent.ShowMessage)
    }
}
