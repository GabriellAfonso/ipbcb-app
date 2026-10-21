package com.ipb.castelobranco.features.profile.presentation.state

import com.ipb.castelobranco.features.profile.presentation.format.formatJoinedSince
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ProfileUiStateTest {

    @Test
    fun `a name is saved only when trimmed, not blank and changed`() {
        assertTrue(canSaveName(" Ana Maria ", "Ana"))
        assertFalse(canSaveName("   ", "Ana"))
        assertFalse(canSaveName(" Ana ", "Ana"))
    }

    @Test
    fun `the password change needs the three fields`() {
        assertTrue(canChangePassword("a", "b", "c"))
        assertFalse(canChangePassword("a", "", "c"))
    }

    @Test
    fun `deletion needs EXCLUIR and, with a password account, the password`() {
        assertTrue(canDeleteAccount(requiresPassword = true, password = "x", confirmation = "excluir"))
        assertFalse(canDeleteAccount(requiresPassword = true, password = "", confirmation = "EXCLUIR"))
        assertFalse(canDeleteAccount(requiresPassword = true, password = "x", confirmation = "EXCLUI"))
        assertTrue(canDeleteAccount(requiresPassword = false, password = "", confirmation = "EXCLUIR"))
    }

    @Test
    fun `joined since reads the month in full, lowercase`() {
        assertEquals("março de 2025", formatJoinedSince(LocalDate.of(2025, 3, 14)))
        assertEquals("dezembro de 2024", formatJoinedSince(LocalDate.of(2024, 12, 1)))
    }
}
