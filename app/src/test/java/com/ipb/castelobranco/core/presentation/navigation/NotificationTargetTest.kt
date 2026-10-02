package com.ipb.castelobranco.core.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class NotificationTargetTest {

    @Test
    fun `sunday setlist needs no date`() {
        assertEquals(NotificationTarget.SundaySetlist, NotificationTarget.fromExtras("sunday_setlist", null))
    }

    @Test
    fun `confirm plays carries its date`() {
        assertEquals(
            NotificationTarget.ConfirmPlays(LocalDate.of(2026, 10, 4)),
            NotificationTarget.fromExtras("confirm_plays", "2026-10-04"),
        )
    }

    @Test
    fun `a normal launch or broken extras are no target`() {
        assertNull(NotificationTarget.fromExtras(null, null))
        assertNull(NotificationTarget.fromExtras("confirm_plays", null))
        assertNull(NotificationTarget.fromExtras("confirm_plays", "ontem"))
        assertNull(NotificationTarget.fromExtras("other", "2026-10-04"))
    }
}
