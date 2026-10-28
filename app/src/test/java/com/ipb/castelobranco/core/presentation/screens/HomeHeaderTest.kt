package com.ipb.castelobranco.core.presentation.screens

import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleBandUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HomeHeaderTest {

    private val wednesday = LocalDate.of(2026, 10, 28)
    private val schedule = ScheduleBandUi(true, "NOV", 15, "Domingo, 15/11 · 18h", "Faltam 18 dias")

    @Test
    fun `greeting follows the hour`() {
        assertEquals("Boa noite", greetingFor(4))
        assertEquals("Bom dia", greetingFor(5))
        assertEquals("Bom dia", greetingFor(11))
        assertEquals("Boa tarde", greetingFor(12))
        assertEquals("Boa tarde", greetingFor(17))
        assertEquals("Boa noite", greetingFor(18))
    }

    @Test
    fun `date label drops feira and capitalizes the weekday`() {
        assertEquals("Quarta, 28 de outubro", homeDateLabel(wednesday))
        assertEquals("Domingo, 1 de novembro", homeDateLabel(LocalDate.of(2026, 11, 1)))
    }

    @Test
    fun `logged in greets by first name`() {
        val header = buildHomeHeader(wednesday, hour = 21, isLoggedIn = true, firstName = "Gabriel", schedule = schedule)

        assertEquals("Boa noite, Gabriel", header.greeting)
        assertEquals(schedule, header.schedule)
    }

    @Test
    fun `logged in without a name greets plainly`() {
        assertEquals("Bom dia", buildHomeHeader(wednesday, 9, true, firstName = null, schedule = null).greeting)
    }

    @Test
    fun `visitor gets the welcome and no schedule card`() {
        val header = buildHomeHeader(wednesday, hour = 21, isLoggedIn = false, firstName = "Gabriel", schedule = schedule)

        assertEquals(VISITOR_GREETING, header.greeting)
        assertNull(header.schedule)
    }
}
