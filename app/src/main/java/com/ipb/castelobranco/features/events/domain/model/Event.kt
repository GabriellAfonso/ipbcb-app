package com.ipb.castelobranco.features.events.domain.model

import java.time.LocalDate
import java.time.LocalTime

/**
 * A church event as the public list delivers it. `endDate` null — or equal to `startDate` — is a
 * one-day event.
 */
data class Event(
    val id: Int,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val startTime: LocalTime?,
    val location: String?,
    val description: String?,
    val imageUrl: String,
    val isMembersOnly: Boolean,
) {
    val lastDay: LocalDate get() = endDate ?: startDate

    /** Active until the end of its last day (`specs/events/spec.md` §1). */
    fun isActiveOn(day: LocalDate): Boolean = !lastDay.isBefore(day)
}
