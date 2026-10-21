package com.ipb.castelobranco.features.admin.events.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class EventVisibility(val wireValue: String) {
    PUBLIC("public"),
    MEMBERS("members");

    companion object {
        /** Anything unknown reads as public: the server's default. */
        fun fromWire(value: String): EventVisibility = entries.firstOrNull { it.wireValue == value } ?: PUBLIC
    }
}

/** The management list's filter (`?status=`). */
enum class EventStatusFilter(val wireValue: String, val label: String) {
    ACTIVE("active", "Ativos"),
    PAST("past", "Passados"),
    ALL("all", "Todos"),
}

/**
 * An event as management sees it: the public fields plus who made it, who last edited it and when.
 *
 * @param wasEdited the server named a last editor; [updatedByName] can still be null (no profile name).
 */
data class EventRecord(
    val id: Int,
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val startTime: LocalTime?,
    val location: String?,
    val description: String?,
    val imageUrl: String,
    val visibility: EventVisibility,
    val createdByName: String?,
    val wasEdited: Boolean,
    val updatedByName: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?,
) {
    /** Past = its last day is before [today]; it stays listed in management. */
    fun isPastOn(today: LocalDate): Boolean = (endDate ?: startDate).isBefore(today)
}

/** A form field, keyed as the server's `field_errors` names it. */
enum class EventField(val apiKey: String) {
    TITLE("title"),
    START_DATE("start_date"),
    END_DATE("end_date"),
    START_TIME("start_time"),
    LOCATION("location"),
    DESCRIPTION("description"),
    VISIBILITY("visibility"),
    IMAGE("image");

    companion object {
        fun fromApiKey(key: String): EventField? = entries.firstOrNull { it.apiKey == key }
    }
}
