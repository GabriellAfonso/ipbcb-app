package com.ipb.castelobranco.features.events.presentation.state

/** Rows that are null are not shown at all. */
data class EventDetailUi(
    val title: String,
    val dateLabel: String,
    val timeLabel: String?,
    val location: String?,
    val description: String?,
    val imageUrl: String,
    val isMembersOnly: Boolean,
)

/**
 * @param isGone the event is no longer among the active ones — it ended or was removed.
 */
data class EventDetailUiState(
    val isLoading: Boolean = true,
    val event: EventDetailUi? = null,
    val isGone: Boolean = false,
)
