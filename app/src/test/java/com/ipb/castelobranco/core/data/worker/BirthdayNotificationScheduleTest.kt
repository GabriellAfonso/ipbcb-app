package com.ipb.castelobranco.core.data.worker

import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

class BirthdayNotificationScheduleTest {

    private val today = LocalDate.of(2026, 10, 13)
    private val todays = Birthday(name = "Gabriel", month = 10, day = 13)
    private val other = Birthday(name = "Izalane", month = 10, day = 23)
    private val snapshot = SnapshotState.Data(listOf(todays, other))

    @Test
    fun `initialDelay targets today when the hour is still ahead`() {
        val now = today.atTime(9, 30)

        val delay = BirthdayNotificationSchedule.initialDelay(now, BirthdayNotificationSchedule.NOON)

        assertEquals(Duration.ofMinutes(150), delay)
    }

    @Test
    fun `initialDelay targets tomorrow when the hour has passed`() {
        val now = today.atTime(12, 0)

        val delay = BirthdayNotificationSchedule.initialDelay(now, BirthdayNotificationSchedule.NOON)

        assertEquals(Duration.ofDays(1), delay)
    }

    @Test
    fun `initialDelay to the evening run from the afternoon`() {
        val now = LocalDateTime.of(today, BirthdayNotificationSchedule.NOON)

        val delay = BirthdayNotificationSchedule.initialDelay(now, BirthdayNotificationSchedule.EVENING)

        assertEquals(Duration.ofHours(6), delay)
    }

    @Test
    fun `birthdaysToNotify keeps only today's birthdays`() {
        val result = BirthdayNotificationSchedule.birthdaysToNotify(snapshot, today, lastNotified = null)

        assertEquals(listOf(todays), result)
    }

    @Test
    fun `birthdaysToNotify posts when the last notification was another day`() {
        val result = BirthdayNotificationSchedule.birthdaysToNotify(snapshot, today, today.minusDays(1))

        assertEquals(listOf(todays), result)
    }

    @Test
    fun `birthdaysToNotify skips a day already notified`() {
        val result = BirthdayNotificationSchedule.birthdaysToNotify(snapshot, today, lastNotified = today)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `birthdaysToNotify is empty without cached data`() {
        val result = BirthdayNotificationSchedule.birthdaysToNotify(SnapshotState.Loading, today, lastNotified = null)

        assertTrue(result.isEmpty())
    }
}
