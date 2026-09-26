package com.ipb.castelobranco.features.admin.members.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeleteConfirmationTest {

    @Test
    fun `the name matches ignoring case and surrounding spaces`() {
        assertTrue(DeleteConfirmation.matches("Ana Souza", "Ana Souza"))
        assertTrue(DeleteConfirmation.matches("  ana souza ", "Ana Souza"))
    }

    @Test
    fun `a partial, different or empty name does not match`() {
        assertFalse(DeleteConfirmation.matches("Ana", "Ana Souza"))
        assertFalse(DeleteConfirmation.matches("Ana  Souza", "Ana Souza"))
        assertFalse(DeleteConfirmation.matches("", ""))
    }
}
