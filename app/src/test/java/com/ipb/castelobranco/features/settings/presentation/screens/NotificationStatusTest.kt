package com.ipb.castelobranco.features.settings.presentation.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationStatusTest {

    @Test
    fun `status follows the preference and the system`() {
        assertEquals("Ativado", notificationStatus(enabled = true, allowedBySystem = true))
        assertEquals("Bloqueado no sistema", notificationStatus(enabled = true, allowedBySystem = false))
        assertEquals("Desativado", notificationStatus(enabled = false, allowedBySystem = true))
        assertEquals("Desativado", notificationStatus(enabled = false, allowedBySystem = false))
    }
}
