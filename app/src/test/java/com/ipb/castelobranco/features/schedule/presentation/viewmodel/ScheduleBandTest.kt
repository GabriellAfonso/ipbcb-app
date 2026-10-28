package com.ipb.castelobranco.features.schedule.presentation.viewmodel

import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleRowUi
import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleSectionUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ScheduleBandTest {

    private val me = 7L
    private val today = LocalDate.of(2026, 11, 4)

    private val thursday = ScheduleSectionUi(
        title = "Quinta-feira",
        time = "19h30",
        rows = listOf(
            ScheduleRowUi(day = 5, member = "Ana", memberId = 1),
            ScheduleRowUi(day = 12, member = "Bruno", memberId = 2),
        ),
    )
    private val sunday = ScheduleSectionUi(
        title = "Domingo",
        time = "18h",
        rows = listOf(
            ScheduleRowUi(day = 1, member = "Eu", memberId = me),
            ScheduleRowUi(day = 8, member = "Marcos Vinícius", memberId = 3),
            ScheduleRowUi(day = 15, member = "Eu", memberId = me),
        ),
    )

    private fun band(memberId: Long?, next: ScheduleSectionUi?, date: LocalDate = today) = buildScheduleBand(
        sections = listOf(thursday, sunday),
        scheduleYear = 2026,
        scheduleMonth = 11,
        memberId = memberId,
        nextSection = next,
        today = date,
    )

    @Test
    fun `own upcoming date wins, skipping past ones`() {
        val result = band(me, next = thursday)!!

        assertTrue(result.isOwn)
        assertEquals("NOV", result.monthLabel)
        assertEquals(15, result.day)
        assertEquals("Domingo, 15/11 · 18h", result.title)
        assertEquals("Faltam 11 dias", result.detail)
    }

    @Test
    fun `own date tomorrow and today get their own labels`() {
        assertEquals("Amanhã", band(me, thursday, date = LocalDate.of(2026, 11, 14))!!.detail)
        assertEquals("É hoje", band(me, thursday, date = LocalDate.of(2026, 11, 15))!!.detail)
    }

    @Test
    fun `not on the rota shows the next service and who serves`() {
        val result = band(memberId = 99, next = thursday)!!

        assertFalse(result.isOwn)
        assertEquals(5, result.day)
        assertEquals("Quinta, 05/11 · 19h30", result.title)
        assertEquals("Escalado: Ana", result.detail)
    }

    @Test
    fun `no member link falls back to the next service`() {
        assertFalse(band(memberId = null, next = sunday)!!.isOwn)
    }

    @Test
    fun `section without time leaves it out of the title`() {
        val noTime = sunday.copy(time = " ")
        val result = buildScheduleBand(listOf(noTime), 2026, 11, null, noTime, today)!!

        assertEquals("Domingo, 08/11", result.title)
    }

    @Test
    fun `nothing upcoming gives no band`() {
        assertNull(band(memberId = me, next = thursday, date = LocalDate.of(2026, 11, 20)))
        assertNull(band(memberId = 99, next = null))
    }
}
