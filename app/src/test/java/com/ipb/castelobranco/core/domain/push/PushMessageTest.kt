package com.ipb.castelobranco.core.domain.push

import com.ipb.castelobranco.core.domain.access.AccessRequestType
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

    @Test
    fun `access request messages are read with their request type and reason`() {
        assertEquals(
            PushMessage.AccessRequestCreated(AccessRequestType.ACCESS),
            PushMessage.parse(
                mapOf("type" to "access_request_created", "request_id" to "41", "request_type" to "access")
            ),
        )
        assertEquals(
            PushMessage.AccessRequestApproved(AccessRequestType.DATA_CORRECTION),
            PushMessage.parse(mapOf("type" to "access_request_approved", "request_type" to "data_correction")),
        )
        assertEquals(
            PushMessage.AccessRequestDenied(AccessRequestType.ACCESS, "Procure o pastor."),
            PushMessage.parse(
                mapOf("type" to "access_request_denied", "request_type" to "access", "reason" to " Procure o pastor. ")
            ),
        )
        assertEquals(
            PushMessage.AccessRequestDenied(AccessRequestType.ACCESS, null),
            PushMessage.parse(mapOf("type" to "access_request_denied", "request_type" to "access")),
        )
    }

    @Test
    fun `an access request message with an unknown request type is ignored`() {
        assertNull(PushMessage.parse(mapOf("type" to "access_request_created", "request_type" to "other")))
        assertNull(PushMessage.parse(mapOf("type" to "access_request_approved")))
    }
}
