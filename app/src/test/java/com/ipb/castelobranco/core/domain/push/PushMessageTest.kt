package com.ipb.castelobranco.core.domain.push

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class PushMessageTest {

    private val sunday = LocalDate.of(2026, 10, 4)

    @Test
    fun `the two known types are read with their date`() {
        assertEquals(
            PushMessage.SetlistSaved(sunday),
            PushMessage.parse(mapOf("type" to "setlist_saved", "date" to "2026-10-04")),
        )
        assertEquals(
            PushMessage.ConfirmPlays(sunday),
            PushMessage.parse(mapOf("type" to "confirm_plays", "date" to "2026-10-04")),
        )
    }

    @Test
    fun `an unknown type is ignored`() {
        assertNull(PushMessage.parse(mapOf("type" to "news", "date" to "2026-10-04")))
    }

    @Test
    fun `a missing or malformed date is ignored`() {
        assertNull(PushMessage.parse(mapOf("type" to "setlist_saved")))
        assertNull(PushMessage.parse(mapOf("type" to "setlist_saved", "date" to "04/10/2026")))
    }
}
