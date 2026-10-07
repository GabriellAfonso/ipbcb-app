package com.ipb.castelobranco.features.gallery.presentation.state

/** A person of the filter list: tagged in [photoCount] photos on the device. */
data class PersonRow(
    val id: Long,
    val name: String,
    /** "1 foto", "3 fotos". */
    val countText: String,
    val isChecked: Boolean,
)

/**
 * The people filter. It shows the matching people while nothing
 * is selected or a search is typed, and the photos with every selected person otherwise.
 */
data class PeopleUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    /** Search field and chips: the filter, once someone is tagged. */
    val showSearch: Boolean = false,
    /** People matching [query]; shown instead of the results while [showResults] is false. */
    val people: List<PersonRow> = emptyList(),
    val selected: List<PersonRow> = emptyList(),
    val showResults: Boolean = false,
    val results: List<PhotoTile> = emptyList(),
    /** The people the results were computed for — what the viewer pages through. */
    val memberIds: Set<Long> = emptySet(),
    val hint: String? = null,
    /** The text in place of the list or the grid, when there is nothing to show. */
    val emptyText: String? = null,
)
