package com.ipb.castelobranco.features.events.data.mapper

import com.ipb.castelobranco.features.events.data.dto.EventPublicDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class EventMapperTest {

    private fun dto(
        id: Int = 1,
        startDate: String = "2026-11-14",
        endDate: String? = null,
        startTime: String? = null,
        location: String? = null,
        description: String? = null,
        visibility: String = EventPublicDto.VISIBILITY_PUBLIC,
    ) = EventPublicDto(
        id = id,
        title = "Congresso",
        startDate = startDate,
        endDate = endDate,
        startTime = startTime,
        location = location,
        description = description,
        imageUrl = "https://host/x.jpg",
        visibility = visibility,
    )

    @Test
    fun `full item maps every field`() {
        val event = listOf(
            dto(
                endDate = "2026-11-16",
                startTime = "19:30",
                location = "Templo",
                description = "Três noites",
                visibility = EventPublicDto.VISIBILITY_MEMBERS,
            )
        ).toDomain().single()

        assertEquals(LocalDate.of(2026, 11, 14), event.startDate)
        assertEquals(LocalDate.of(2026, 11, 16), event.endDate)
        assertEquals(LocalTime.of(19, 30), event.startTime)
        assertEquals("Templo", event.location)
        assertEquals("Três noites", event.description)
        assertTrue(event.isMembersOnly)
    }

    @Test
    fun `nulls and blanks become null and visibility defaults to public`() {
        val event = listOf(dto(location = " ", description = "")).toDomain().single()

        assertNull(event.endDate)
        assertNull(event.startTime)
        assertNull(event.location)
        assertNull(event.description)
        assertFalse(event.isMembersOnly)
    }

    @Test
    fun `unreadable time becomes null`() {
        assertNull(listOf(dto(startTime = "7pm")).toDomain().single().startTime)
    }

    @Test
    fun `item with unreadable start date is dropped and the rest kept`() {
        val events = listOf(dto(id = 1, startDate = "amanhã"), dto(id = 2)).toDomain()

        assertEquals(listOf(2), events.map { it.id })
    }
}
