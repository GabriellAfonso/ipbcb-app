package com.ipb.castelobranco.features.events.presentation.state

import com.ipb.castelobranco.features.events.domain.model.Event
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/** What "Agendar" hands to the calendar app. `endMillis` null lets that app pick its default length. */
data class EventCalendarEntry(
    val title: String,
    val location: String?,
    val description: String?,
    val beginMillis: Long,
    val endMillis: Long?,
    val isAllDay: Boolean,
)

/** Event times are Brasília's, whatever the phone's time zone (`specs/events/spec.md` §1). */
private val CHURCH_ZONE: ZoneId = ZoneId.of("America/Sao_Paulo")
private val LAST_MINUTE: LocalTime = LocalTime.of(23, 59)

/**
 * No time: an all-day event, which Android expects at UTC midnights with an exclusive end. With a time, a one-day
 * event has no known end; a longer one runs until the end of its last day.
 */
internal fun Event.toCalendarEntry(): EventCalendarEntry {
    val time = startTime
    val begin = when (time) {
        null -> startDate.atStartOfDay(ZoneOffset.UTC)
        else -> startDate.atTime(time).atZone(CHURCH_ZONE)
    }
    val end = when {
        time == null -> lastDay.plusDays(1).atStartOfDay(ZoneOffset.UTC)
        lastDay.isAfter(startDate) -> lastDay.atTime(LAST_MINUTE).atZone(CHURCH_ZONE)
        else -> null
    }
    return EventCalendarEntry(
        title = title,
        location = location,
        description = description,
        beginMillis = begin.toInstant().toEpochMilli(),
        endMillis = end?.toInstant()?.toEpochMilli(),
        isAllDay = time == null,
    )
}
