package com.ipb.castelobranco.features.admin.events.domain

import com.ipb.castelobranco.features.admin.events.domain.model.EventChanges
import com.ipb.castelobranco.features.admin.events.domain.model.EventDraft
import com.ipb.castelobranco.features.admin.events.domain.model.EventField
import com.ipb.castelobranco.features.admin.events.domain.model.EventVisibility
import com.ipb.castelobranco.features.admin.events.domain.repository.ImageBounds
import com.ipb.castelobranco.features.admin.events.domain.usecase.ValidateEventDraftUseCase
import com.ipb.castelobranco.features.admin.events.domain.usecase.ValidateEventImageUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EventDraftRulesTest {

    private val validate = ValidateEventDraftUseCase()
    private val start = LocalDate.of(2026, 11, 14)
    private val valid = EventDraft(title = "Congresso", startDate = start)

    @Test
    fun `a filled draft with an image passes`() {
        assertTrue(validate(valid, hasImage = true).isEmpty())
    }

    @Test
    fun `missing title, start date and image are each reported`() {
        val errors = validate(EventDraft(title = "  "), hasImage = false)

        assertEquals(setOf(EventField.TITLE, EventField.START_DATE, EventField.IMAGE), errors.keys)
    }

    @Test
    fun `end before start, long title and long location are refused`() {
        val errors = validate(
            valid.copy(
                title = "x".repeat(121),
                endDate = start.minusDays(1),
                location = "y".repeat(201),
            ),
            hasImage = true,
        )

        assertEquals(setOf(EventField.TITLE, EventField.END_DATE, EventField.LOCATION), errors.keys)
    }

    @Test
    fun `create sends only filled fields, with wire formats`() {
        val draft = valid.copy(startTime = LocalTime.of(9, 5), visibility = EventVisibility.MEMBERS)

        assertEquals(
            mapOf(
                EventField.TITLE to "Congresso",
                EventField.START_DATE to "2026-11-14",
                EventField.START_TIME to "09:05",
                EventField.VISIBILITY to "members",
            ),
            EventChanges.forCreate(draft).values,
        )
    }

    @Test
    fun `edit sends only what changed and clears with empty strings`() {
        val original = valid.copy(location = "Templo", startTime = LocalTime.of(19, 30), endDate = start.plusDays(2))
        val edited = original.copy(location = "", startTime = null, endDate = null, title = "Congresso 2026")

        assertEquals(
            mapOf(
                EventField.TITLE to "Congresso 2026",
                EventField.END_DATE to "",
                EventField.START_TIME to "",
                EventField.LOCATION to "",
            ),
            EventChanges.between(original, edited).values,
        )
    }

    @Test
    fun `an untouched edit has no changes`() {
        assertTrue(EventChanges.between(valid, valid.copy(title = " Congresso ")).isEmpty)
    }

    @Test
    fun `image under 640x360 or unreadable is refused`() {
        fun check(bounds: ImageBounds?) = ValidateEventImageUseCase { bounds }(ByteArray(1))

        assertEquals(ValidateEventImageUseCase.TOO_SMALL, check(ImageBounds(639, 360)))
        assertEquals(ValidateEventImageUseCase.TOO_SMALL, check(ImageBounds(1920, 359)))
        assertEquals(ValidateEventImageUseCase.UNREADABLE, check(null))
        assertNull(check(ImageBounds(1920, 1080)))
    }
}
