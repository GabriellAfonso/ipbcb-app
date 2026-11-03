package com.ipb.castelobranco.features.events.presentation.state

data class EventListItemUi(
    val id: Int,
    val title: String,
    val dateLabel: String,
    val location: String?,
    val imageUrl: String,
    val isMembersOnly: Boolean,
)

/**
 * @param loadFailed the fetch failed with nothing saved to show. A failure over a saved list keeps the list.
 */
data class EventsListUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val events: List<EventListItemUi> = emptyList(),
    val loadFailed: Boolean = false,
)
