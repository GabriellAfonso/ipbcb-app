package com.ipb.castelobranco.core.data.push

import com.ipb.castelobranco.core.domain.push.RecipientGender
import com.ipb.castelobranco.core.domain.push.ScheduleSlot
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class ScheduleNotificationTextTest {

    private val thursday = ScheduleSlot(LocalDate.of(2026, 10, 29), "Quinta de Oração", LocalTime.of(19, 30))
    private val sunday = ScheduleSlot(LocalDate.of(2026, 11, 1), "Culto Dominical", LocalTime.of(19, 0))
    private val saturday = ScheduleSlot(LocalDate.of(2026, 10, 31), "Vigília", LocalTime.of(9, 0))

    @Test
    fun `a slot line has service, weekday, date and time`() {
        assertEquals("Quinta de Oração · quinta 29/10 · 19h30", ScheduleNotificationText.line(thursday))
        assertEquals("Culto Dominical · domingo 01/11 · 19h00", ScheduleNotificationText.line(sunday))
        assertEquals("Vigília · sábado 31/10 · 09h00", ScheduleNotificationText.line(saturday))
    }

    @Test
    fun `week text is the line for one slot and a count for more`() {
        assertEquals("Quinta de Oração · quinta 29/10 · 19h30", ScheduleNotificationText.weekText(listOf(thursday)))
        assertEquals("2 cultos nesta semana", ScheduleNotificationText.weekText(listOf(thursday, sunday)))
    }

    @Test
    fun `eve names the service and the time`() {
        assertEquals("Amanhã: Quinta de Oração", ScheduleNotificationText.eveTitle(thursday))
        assertEquals(
            "Você é o responsável · 29/10 às 19h30",
            ScheduleNotificationText.eveText(thursday, RecipientGender.MALE),
        )
        assertEquals(
            "Você é a responsável · 29/10 às 19h30",
            ScheduleNotificationText.eveText(thursday, RecipientGender.FEMALE),
        )
    }

    @Test
    fun `published names the month and counts the dates`() {
        assertEquals("Escala de novembro publicada", ScheduleNotificationText.publishedTitle(YearMonth.of(2026, 11)))
        assertEquals("Escala de março publicada", ScheduleNotificationText.publishedTitle(YearMonth.of(2027, 3)))
        assertEquals("Você está escalado em 1 data", ScheduleNotificationText.publishedText(1, RecipientGender.MALE))
        assertEquals("Você está escalado em 3 datas", ScheduleNotificationText.publishedText(3, RecipientGender.MALE))
        assertEquals(
            "Você está escalada em 1 data",
            ScheduleNotificationText.publishedText(1, RecipientGender.FEMALE),
        )
        assertEquals(
            "Você está escalada em 3 datas",
            ScheduleNotificationText.publishedText(3, RecipientGender.FEMALE),
        )
    }
}
