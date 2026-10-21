package com.ipb.castelobranco.features.admin.events.presentation.util

import com.ipb.castelobranco.features.admin.events.data.dto.EventAuthorDto
import com.ipb.castelobranco.features.admin.events.data.dto.EventRecordDto
import com.ipb.castelobranco.features.admin.events.data.mapper.toDomain
import com.ipb.castelobranco.features.admin.events.testing.record
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class EventHistoryLabelsTest {

    // 02:30 UTC on the 22nd is still the 21st in Brasília.
    private val created = Instant.parse("2026-10-22T02:30:00Z")
    private val edited = Instant.parse("2026-10-22T15:00:00Z")

    @Test
    fun `creator and editor are both named, dates in Brasilia time`() {
        val event = record(createdBy = "Ana Paula", updatedBy = "Gabriel", createdAt = created, updatedAt = edited)

        assertEquals("Criado por Ana Paula em 21/10/2026 · editado por Gabriel em 22/10/2026", event.historyLabel())
        assertEquals("por Ana Paula", event.authorLabel())
    }

    @Test
    fun `never edited shows only the creation`() {
        val event = record(createdBy = "Ana Paula", createdAt = created, updatedAt = created)

        assertEquals("Criado por Ana Paula em 21/10/2026", event.historyLabel())
    }

    @Test
    fun `a part without a name keeps its date`() {
        val event = record(
            createdBy = null,
            updatedBy = null,
            wasEdited = true,
            createdAt = created,
            updatedAt = edited,
        )

        assertEquals("Criado em 21/10/2026 · editado em 22/10/2026", event.historyLabel())
        assertNull(event.authorLabel())
    }

    @Test
    fun `nothing known gives no label`() {
        assertNull(record(createdBy = null).historyLabel())
    }

    @Test
    fun `updated_by maps to the editor and its absence to never edited`() {
        val dto = EventRecordDto(
            id = 1,
            title = "Congresso",
            startDate = "2026-11-14",
            imageUrl = "https://host/x.jpg",
            visibility = "public",
            createdBy = EventAuthorDto(id = "u1", name = "Ana Paula"),
            updatedBy = EventAuthorDto(id = "u2", name = ""),
        )

        val editedByUnnamed = dto.toDomain()
        assertTrue(editedByUnnamed.wasEdited)
        assertNull(editedByUnnamed.updatedByName)

        val neverEdited = dto.copy(updatedBy = null).toDomain()
        assertFalse(neverEdited.wasEdited)
        assertEquals("Ana Paula", neverEdited.createdByName)
    }
}
