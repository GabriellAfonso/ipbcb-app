package com.ipb.castelobranco.features.admin.events.domain.model

import java.time.LocalDate
import java.time.LocalTime

/** What the form holds. The picked image stays apart, in the form state: it is bytes, not a field. */
data class EventDraft(
    val title: String = "",
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val startTime: LocalTime? = null,
    val location: String = "",
    val description: String = "",
    val visibility: EventVisibility = EventVisibility.PUBLIC,
) {
    /**
     * Every field as the multipart text the server reads; an absent optional value is `""`, which on an
     * edit is also how the server is told to clear it.
     */
    fun toWire(): Map<EventField, String> = mapOf(
        EventField.TITLE to title.trim(),
        EventField.START_DATE to startDate?.toString().orEmpty(),
        EventField.END_DATE to endDate?.toString().orEmpty(),
        EventField.START_TIME to startTime?.let { "%02d:%02d".format(it.hour, it.minute) }.orEmpty(),
        EventField.LOCATION to location.trim(),
        EventField.DESCRIPTION to description.trim(),
        EventField.VISIBILITY to visibility.wireValue,
    )
}

fun EventRecord.toDraft(): EventDraft = EventDraft(
    title = title,
    startDate = startDate,
    endDate = endDate?.takeIf { it != startDate },
    startTime = startTime,
    location = location.orEmpty(),
    description = description.orEmpty(),
    visibility = visibility,
)

/** The text parts of one write. */
@JvmInline
value class EventChanges(val values: Map<EventField, String>) {
    val isEmpty: Boolean get() = values.isEmpty()

    companion object {
        /** A new event sends what is filled; an empty optional field is simply left out. */
        fun forCreate(draft: EventDraft): EventChanges =
            EventChanges(draft.toWire().filterValues { it.isNotEmpty() })

        /** An edit sends only what changed; a cleared optional field goes as `""`. */
        fun between(original: EventDraft, edited: EventDraft): EventChanges {
            val before = original.toWire()
            return EventChanges(edited.toWire().filter { (field, value) -> before[field] != value })
        }
    }
}
