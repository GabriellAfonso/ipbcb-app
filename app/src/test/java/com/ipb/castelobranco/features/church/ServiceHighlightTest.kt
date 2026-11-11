package com.ipb.castelobranco.features.church

import com.ipb.castelobranco.features.church.presentation.state.ChurchInfo
import com.ipb.castelobranco.features.church.presentation.state.ChurchService
import com.ipb.castelobranco.features.church.presentation.state.ServiceBadge
import com.ipb.castelobranco.features.church.presentation.state.ServiceHighlight
import com.ipb.castelobranco.features.church.presentation.state.highlightAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class ServiceHighlightTest {

    private val services = ChurchInfo.services
    private val tuesdayPrayer = services[0]
    private val thursdayPrayer = services[1]
    private val sundaySchool = services[2]
    private val sundayWorship = services[3]

    // 2026-11-11 is a Wednesday.
    private fun at(day: Int, hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, day, hour, minute)

    @Test
    fun `on wednesday the next one is thursday's prayer`() {
        assertEquals(ServiceHighlight(thursdayPrayer, ServiceBadge.NEXT), services.highlightAt(at(11, 10)))
    }

    @Test
    fun `a service that started less than 90 minutes ago is live`() {
        assertEquals(ServiceHighlight(sundaySchool, ServiceBadge.LIVE), services.highlightAt(at(15, 10, 29)))
    }

    @Test
    fun `at 90 minutes it is no longer live and the next one takes the badge`() {
        assertEquals(ServiceHighlight(sundayWorship, ServiceBadge.NEXT), services.highlightAt(at(15, 10, 30)))
    }

    @Test
    fun `at the exact start time the service is live`() {
        assertEquals(ServiceHighlight(sundayWorship, ServiceBadge.LIVE), services.highlightAt(at(15, 19, 30)))
    }

    @Test
    fun `after sunday's last service the next one is in the following week`() {
        assertEquals(ServiceHighlight(tuesdayPrayer, ServiceBadge.NEXT), services.highlightAt(at(15, 22)))
    }

    @Test
    fun `an empty list has no highlight`() {
        assertNull(emptyList<ChurchService>().highlightAt(at(11, 10)))
    }
}
