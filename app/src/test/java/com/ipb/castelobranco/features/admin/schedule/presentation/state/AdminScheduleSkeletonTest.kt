package com.ipb.castelobranco.features.admin.schedule.presentation.state

import com.ipb.castelobranco.features.admin.schedule.domain.model.ScheduleType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminScheduleSkeletonTest {

    @Test
    fun `build returns expected count of Tuesdays Thursdays and Sundays for May 2026`() {
        val items = AdminScheduleSkeleton.build(2026, 5)

        // May 2026: Tue 5,12,19,26 / Thu 7,14,21,28 / Sun 3,10,17,24,31
        val tuesdays = items.filter { it.scheduleTypeId == ScheduleType.TUESDAY_PRAYER.id }
        val thursdays = items.filter { it.scheduleTypeId == ScheduleType.THURSDAY_PRAYER.id }
        val sundays = items.filter { it.scheduleTypeId == ScheduleType.SUNDAY_LITURGY.id }

        assertEquals(4, tuesdays.size)
        assertEquals(4, thursdays.size)
        assertEquals(5, sundays.size)
        assertEquals(13, items.size)
    }

    @Test
    fun `build assigns null member to every item`() {
        val items = AdminScheduleSkeleton.build(2026, 5)
        assertTrue(items.all { it.selectedMember == null })
    }

    @Test
    fun `build emits date in ISO yyyy-MM-dd format`() {
        val items = AdminScheduleSkeleton.build(2026, 5)
        val firstSunday = items.first { it.scheduleTypeId == ScheduleType.SUNDAY_LITURGY.id }
        assertEquals("2026-05-03", firstSunday.date)
        assertEquals(3, firstSunday.day)
    }

    @Test
    fun `build maps day-of-week to correct schedule type display name`() {
        val items = AdminScheduleSkeleton.build(2026, 5)
        val byDay = items.associateBy { it.day }

        assertEquals(ScheduleType.TUESDAY_PRAYER.displayName, byDay[5]?.scheduleTypeName)
        assertEquals(ScheduleType.THURSDAY_PRAYER.displayName, byDay[7]?.scheduleTypeName)
        assertEquals(ScheduleType.SUNDAY_LITURGY.displayName, byDay[3]?.scheduleTypeName)
    }

    @Test
    fun `build handles month with 28 days February 2026`() {
        // Feb 2026: Tue 3,10,17,24 / Thu 5,12,19,26 / Sun 1,8,15,22
        val items = AdminScheduleSkeleton.build(2026, 2)
        assertEquals(12, items.size)
    }

    @Test
    fun `build handles leap month February 2024`() {
        // Feb 2024 (leap, 29 days): Tue 6,13,20,27 / Thu 1,8,15,22,29 / Sun 4,11,18,25
        val items = AdminScheduleSkeleton.build(2024, 2)
        val byType = items.groupBy { it.scheduleTypeId }
        assertEquals(4, byType[ScheduleType.TUESDAY_PRAYER.id]?.size)
        assertEquals(5, byType[ScheduleType.THURSDAY_PRAYER.id]?.size)
        assertEquals(4, byType[ScheduleType.SUNDAY_LITURGY.id]?.size)
    }
}
