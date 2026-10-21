package com.ipb.castelobranco.features.admin.events.presentation.state

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
)

data class EventsAdminListUiState(
    val filter: EventStatusFilter = EventStatusFilter.ALL,
    val isLoading: Boolean = true,
    val error: String? = null,
    val items: List<EventAdminItemUi> = emptyList(),
    val canCreate: Boolean = false,
)

/**
 * @param isReadOnly a `view` level: everything shows, nothing can be changed.
 * @param fieldErrors local checks and server `field_errors` land here, on the same field.
 * @param generalError a server refusal that names no known field.
 * @param imageUrl the event's current flyer; edit only.
 * @param pickedImage a cropped flyer, sent only when the user saves.
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
)

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
