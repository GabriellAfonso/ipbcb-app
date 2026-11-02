package com.ipb.castelobranco.core.presentation.screens

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationNoticeTest {

    private fun show(
        isLoggedIn: Boolean = true,
        recoveryNoticeSettled: Boolean = true,
        recoveryNoticeShown: Boolean = false,
        notificationsAllowed: Boolean = false,
        answered: Boolean? = false,
    ) = shouldShowNotificationNotice(
        isLoggedIn,
        recoveryNoticeSettled,
        recoveryNoticeShown,
        notificationsAllowed,
        answered,
    )

    @Test
    fun `shows for a signed-in account without notifications that never answered`() {
        assertTrue(show())
    }

    @Test
    fun `stays away when any condition fails`() {
        assertFalse(show(isLoggedIn = false))
        assertFalse(show(recoveryNoticeSettled = false))
        assertFalse(show(recoveryNoticeShown = true))
        assertFalse(show(notificationsAllowed = true))
        assertFalse(show(answered = true))
        assertFalse(show(answered = null))
    }
}
