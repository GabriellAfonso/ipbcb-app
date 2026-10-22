package com.ipb.castelobranco.features.schedule.presentation.mapper

import com.ipb.castelobranco.features.schedule.domain.model.MonthSchedule
import com.ipb.castelobranco.features.schedule.domain.model.ScheduleEntry
import com.ipb.castelobranco.features.schedule.domain.model.ScheduleItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleUiMapperTest {

    // region ordering

    @Test
    fun `toSectionsUi orders terca before quinta before domingo before others`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Domingo" to ScheduleEntry(time = "", items = listOf(ScheduleItem(1, "A"))),
                "Louvor" to ScheduleEntry(time = "", items = listOf(ScheduleItem(2, "B"))),
                "Quinta-feira" to ScheduleEntry(time = "", items = listOf(ScheduleItem(3, "C"))),
                "Terça-feira" to ScheduleEntry(time = "", items = listOf(ScheduleItem(4, "D"))),
            )
        )

        val result = schedule.toSectionsUi()

        val titles = result.map { it.title }
        assertTrue(titles.indexOf("Terça-feira") < titles.indexOf("Quinta-feira"))
        assertTrue(titles.indexOf("Quinta-feira") < titles.indexOf("Domingo"))
        assertTrue(titles.indexOf("Domingo") < titles.indexOf("Louvor"))
    }

    @Test
    fun `toSectionsUi sorts sections with equal weight alphabetically`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Zebrinha" to ScheduleEntry(time = "", items = listOf(ScheduleItem(1, "A"))),
                "Abertura" to ScheduleEntry(time = "", items = listOf(ScheduleItem(2, "B"))),
                "Louvor" to ScheduleEntry(time = "", items = listOf(ScheduleItem(3, "C"))),
            )
        )

        val result = schedule.toSectionsUi()

        val titles = result.map { it.title }
        assertTrue(titles.indexOf("Abertura") < titles.indexOf("Louvor"))
        assertTrue(titles.indexOf("Louvor") < titles.indexOf("Zebrinha"))
    }

    @Test
    fun `toSectionsUi matches terca in uppercase title`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Domingo" to ScheduleEntry(time = "", items = listOf(ScheduleItem(1, "A"))),
                "TERÇA-FEIRA" to ScheduleEntry(time = "", items = listOf(ScheduleItem(2, "B"))),
            )
        )

        val result = schedule.toSectionsUi()

        val titles = result.map { it.title }
        assertTrue(titles.indexOf("TERÇA-FEIRA") < titles.indexOf("Domingo"))
    }

    // endregion

    // region rows ordering

    @Test
    fun `toSectionsUi orders rows by day within a section`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Domingo" to ScheduleEntry(
                    time = "",
                    items = listOf(
                        ScheduleItem(15, "Carlos"),
                        ScheduleItem(1, "Ana"),
                        ScheduleItem(8, "Bruno"),
                    )
                )
            )
        )

        val result = schedule.toSectionsUi()

        val days = result.first().rows.map { it.day }
        assertEquals(listOf(1, 8, 15), days)
    }

    // endregion

    // region field mapping

    @Test
    fun `toSectionsUi propagates time field`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Terça-feira" to ScheduleEntry(time = "19h30", items = emptyList())
            )
        )

        val result = schedule.toSectionsUi()

        assertEquals("19h30", result.first().time)
    }

    @Test
    fun `toSectionsUi propagates empty time field`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Terça-feira" to ScheduleEntry(time = "", items = emptyList())
            )
        )

        val result = schedule.toSectionsUi()

        assertEquals("", result.first().time)
    }

    @Test
    fun `toSectionsUi maps title correctly`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Terça-feira" to ScheduleEntry(time = "", items = emptyList())
            )
        )

        val result = schedule.toSectionsUi()

        assertEquals("Terça-feira", result.first().title)
    }

    @Test
    fun `toSectionsUi maps row day and member`() {
        val schedule = MonthSchedule(
            year = 2025, month = 1,
            schedule = mapOf(
                "Domingo" to ScheduleEntry(
                    time = "",
                    items = listOf(ScheduleItem(day = 7, member = "Maria"))
                )
            )
        )

        val result = schedule.toSectionsUi()

        val row = result.first().rows.first()
        assertEquals(7, row.day)
        assertEquals("Maria", row.member)
    }

    // endregion

    // region edge cases

    @Test
    fun `toSectionsUi returns empty list for empty schedule`() {
        val schedule = MonthSchedule(year = 2025, month = 1, schedule = emptyMap())

        val result = schedule.toSectionsUi()

        assertTrue(result.isEmpty())
    }

    // endregion
}
