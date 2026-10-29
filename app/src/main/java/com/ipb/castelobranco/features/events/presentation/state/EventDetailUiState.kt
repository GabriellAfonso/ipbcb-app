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
    /** "14 a 16 nov": the date tag under the title. */
    val shortDateLabel: String = dateLabel,
    /** "3 dias" under the full date; `null` for a one-day event. */
    val durationLabel: String? = null,
    /** What "Compartilhar" sends. */
    val shareText: String = title,
)

/**
 * @param isGone the event is no longer among the active ones — it ended or was removed.
 */
data class EventDetailUiState(
    val isLoading: Boolean = true,
    val event: EventDetailUi? = null,
    val isGone: Boolean = false,
)
