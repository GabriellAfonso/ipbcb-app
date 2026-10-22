package com.ipb.castelobranco.features.schedule.data.mapper

import com.ipb.castelobranco.features.schedule.data.dto.MemberDto
import com.ipb.castelobranco.features.schedule.data.dto.MonthScheduleDto
import com.ipb.castelobranco.features.schedule.data.dto.ScheduleEntryDto
import com.ipb.castelobranco.features.schedule.data.dto.ScheduleItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MonthScheduleMapperTest {

    @Test
    fun `toDomain preserves year and month`() {
        val dto = MonthScheduleDto(year = 2025, month = 6, schedule = emptyMap())

        val result = dto.toDomain()

        assertEquals(2025, result.year)
        assertEquals(6, result.month)
    }

    @Test
    fun `toDomain maps empty schedule to empty map`() {
        val dto = MonthScheduleDto(year = 2025, month = 1, schedule = emptyMap())

        val result = dto.toDomain()

        assertTrue(result.schedule.isEmpty())
    }

    @Test
    fun `toDomain preserves section keys`() {
        val dto = MonthScheduleDto(
            year = 2025, month = 1,
            schedule = mapOf(
                "Terça-feira" to ScheduleEntryDto(time = "", items = emptyList()),
                "Domingo" to ScheduleEntryDto(time = "", items = emptyList()),
            )
        )

        val result = dto.toDomain()

        assertTrue(result.schedule.containsKey("Terça-feira"))
        assertTrue(result.schedule.containsKey("Domingo"))
    }

    @Test
    fun `toDomain maps time field correctly`() {
        val dto = MonthScheduleDto(
            year = 2025, month = 1,
            schedule = mapOf("Terça-feira" to ScheduleEntryDto(time = "19h30", items = emptyList()))
        )

        val result = dto.toDomain()

        assertEquals("19h30", result.schedule["Terça-feira"]!!.time)
    }

    @Test
    fun `toDomain maps item day and member name`() {
        val dto = MonthScheduleDto(
            year = 2025, month = 1,
            schedule = mapOf(
                "Domingo" to ScheduleEntryDto(
                    time = "",
                    items = listOf(
                        ScheduleItemDto(day = 5, member = MemberDto(id = 1, name = "João"))
                    )
                )
            )
        )

        val result = dto.toDomain()

        val item = result.schedule["Domingo"]!!.items.first()
        assertEquals(5, item.day)
        assertEquals("João", item.member)
    }

    @Test
    fun `toDomain maps multiple items in a section`() {
        val dto = MonthScheduleDto(
            year = 2025, month = 1,
            schedule = mapOf(
                "Domingo" to ScheduleEntryDto(
                    time = "",
                    items = listOf(
                        ScheduleItemDto(day = 1, member = MemberDto(id = 1, name = "Ana")),
                        ScheduleItemDto(day = 8, member = MemberDto(id = 2, name = "Bruno")),
                        ScheduleItemDto(day = 15, member = MemberDto(id = 3, name = "Carlos")),
                    )
                )
            )
        )

        val result = dto.toDomain()

        val items = result.schedule["Domingo"]!!.items
        assertEquals(3, items.size)
        assertEquals("Ana", items[0].member)
        assertEquals("Bruno", items[1].member)
        assertEquals("Carlos", items[2].member)
    }

    @Test
    fun `toDomain maps multiple sections`() {
        val dto = MonthScheduleDto(
            year = 2025, month = 1,
            schedule = mapOf(
                "Terça-feira" to ScheduleEntryDto(time = "19h", items = listOf(
                    ScheduleItemDto(day = 4, member = MemberDto(id = 1, name = "Ana"))
                )),
                "Domingo" to ScheduleEntryDto(time = "10h", items = listOf(
                    ScheduleItemDto(day = 6, member = MemberDto(id = 2, name = "Bruno"))
                )),
            )
        )

        val result = dto.toDomain()

        assertEquals(2, result.schedule.size)
        assertEquals("Ana", result.schedule["Terça-feira"]!!.items.first().member)
        assertEquals("Bruno", result.schedule["Domingo"]!!.items.first().member)
    }
}
