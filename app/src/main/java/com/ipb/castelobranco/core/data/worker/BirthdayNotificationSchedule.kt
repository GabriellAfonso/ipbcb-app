package com.ipb.castelobranco.core.data.worker

import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * When the birthday notification runs and what it posts. Noon, not early morning: an 8:00 notification lands in
 * the overnight pile the user clears without reading. The 18:00 run only covers a noon run that did not post.
 */
object BirthdayNotificationSchedule {

    val NOON: LocalTime = LocalTime.of(12, 0)
    val EVENING: LocalTime = LocalTime.of(18, 0)

    /** Time until the next [target], today if it is still ahead, else tomorrow. */
    fun initialDelay(now: LocalDateTime, target: LocalTime): Duration {
        val targetToday = now.toLocalDate().atTime(target)
        val nextRun = if (now.isBefore(targetToday)) targetToday else targetToday.plusDays(1)
        return Duration.between(now, nextRun)
    }

    /** Today's birthdays to post, none when a run already posted them today. */
    fun birthdaysToNotify(
        snapshot: SnapshotState<List<Birthday>>,
        today: LocalDate,
        lastNotified: LocalDate?,
    ): List<Birthday> {
        if (lastNotified == today) return emptyList()
        val birthdays = (snapshot as? SnapshotState.Data)?.value ?: return emptyList()
        return birthdays.filter { it.month == today.monthValue && it.day == today.dayOfMonth }
    }
}
