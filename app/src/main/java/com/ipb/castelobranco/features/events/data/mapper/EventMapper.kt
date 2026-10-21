package com.ipb.castelobranco.features.events.data.mapper

import com.ipb.castelobranco.features.events.data.dto.EventPublicDto
import com.ipb.castelobranco.features.events.domain.model.Event
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalTime

/** An item whose start date cannot be read is dropped: one bad event must not hide the others. */
fun List<EventPublicDto>.toDomain(): List<Event> = mapNotNull { it.toDomainOrNull() }

fun EventPublicDto.toDomainOrNull(): Event? {
    val start = parseDate(startDate) ?: run {
        Timber.w("Event %d dropped: unreadable start_date", id)
        return null
    }
    return Event(
        id = id,
        title = title,
        startDate = start,
        endDate = endDate?.let(::parseDate),
        startTime = startTime?.let(::parseTime),
        location = location?.takeIf { it.isNotBlank() },
        description = description?.takeIf { it.isNotBlank() },
        imageUrl = imageUrl,
        isMembersOnly = visibility == EventPublicDto.VISIBILITY_MEMBERS,
    )
}

private fun parseDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()

private fun parseTime(value: String): LocalTime? = runCatching { LocalTime.parse(value) }.getOrNull()
