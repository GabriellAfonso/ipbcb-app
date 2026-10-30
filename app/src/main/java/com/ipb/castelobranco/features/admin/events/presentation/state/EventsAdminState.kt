package com.ipb.castelobranco.features.admin.events.presentation.state

import com.ipb.castelobranco.core.presentation.format.EventDateLabel
import com.ipb.castelobranco.features.admin.events.domain.model.EventDraft
import com.ipb.castelobranco.features.admin.events.domain.model.EventField
import com.ipb.castelobranco.features.admin.events.domain.model.EventStatusFilter

/** One row of the management list, already formatted. */
data class EventAdminItemUi(
    val id: Int,
    val title: String,
    val dateLabel: String,
    val imageUrl: String,
    val isMembersOnly: Boolean,
    val isPast: Boolean,
    /** "por Ana Paula"; null when the name is unknown. */
    val authorLabel: String? = null,
    /** "Novembro de 2026": the group the event falls in, by its start date. */
    val monthLabel: String = "",
    /** The date box: "NOV" over "14". */
    val monthShort: String = "",
    val dayLabel: String = "",
    /** "14 a 16 nov · 19:30", "19:30" or "Dia todo": the date range only when it spans days. */
    val whenLabel: String = "",
)

/** One month of the list, in the list's order. */
data class EventMonthUi(val label: String, val items: List<EventAdminItemUi>)

data class EventsAdminListUiState(
    val filter: EventStatusFilter = EventStatusFilter.ALL,
    val isLoading: Boolean = true,
    val error: String? = null,
    val items: List<EventAdminItemUi> = emptyList(),
    val canCreate: Boolean = false,
) {
    /** Consecutive items of the same month, keeping the server's order (newest first). */
    val months: List<EventMonthUi>
        get() = items.fold(mutableListOf<EventMonthUi>()) { groups, item ->
            val last = groups.lastOrNull()
            if (last?.label == item.monthLabel) {
                groups[groups.lastIndex] = last.copy(items = last.items + item)
            } else {
                groups += EventMonthUi(item.monthLabel, listOf(item))
            }
            groups
        }
}

/**
 * @param isReadOnly a `view` level: everything shows, nothing can be changed.
 * @param fieldErrors local checks and server `field_errors` land here, on the same field.
 * @param generalError a server refusal that names no known field.
 * @param imageUrl the event's current flyer; edit only.
 * @param pickedImage a cropped flyer, sent only when the user saves.
 * @param historyLabel who created and last edited it, with dates; edit only.
 */
data class EventFormUiState(
    val isEditing: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val isReadOnly: Boolean = false,
    val canDelete: Boolean = false,
    val draft: EventDraft = EventDraft(),
    val fieldErrors: Map<EventField, String> = emptyMap(),
    val generalError: String? = null,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val imageUrl: String? = null,
    val pickedImage: ByteArray? = null,
    val historyLabel: String? = null,
) {
    /** "14 a 16 nov · 19:30" for the preview's highlight card; "Sem data" until a start date is picked. */
    val previewDateLabel: String
        get() = draft.startDate?.let { EventDateLabel.dateLabel(it, draft.endDate, draft.startTime) } ?: NO_DATE_LABEL

    /** The date tag of the preview's detail: the dates without the time. */
    val previewShortDate: String
        get() = draft.startDate?.let { EventDateLabel.dateLabel(it, draft.endDate, null) } ?: NO_DATE_LABEL
}

private const val NO_DATE_LABEL = "Sem data"

/** One-shot events shared by the events management screens. */
sealed interface EventsAdminEvent {
    data class ShowMessage(val message: String) : EventsAdminEvent

    /** The server refused a read (403): leave the events area. A refused write is a [ShowMessage]. */
    data class LeaveArea(val message: String) : EventsAdminEvent

    /** The event no longer exists (404): back to the list. */
    data class EventGone(val message: String) : EventsAdminEvent

    data object Saved : EventsAdminEvent

    data object Deleted : EventsAdminEvent
}
