package com.ipb.castelobranco.features.gallery.presentation.state

import com.ipb.castelobranco.features.gallery.domain.tags.MediaFilter

/** A person of the filter list: tagged in [photoCount] photos on the device. */
data class PersonRow(
    val id: Long,
    val name: String,
    /** "1 foto", "3 fotos". */
    val countText: String,
    val isChecked: Boolean,
    /** The first name, for the band's tags. */
    val firstName: String = name,
    /** The avatar's letters: the member photo is never shown here. */
    val initials: String = "",
)

/**
 * The people filter: the band with the search and the chosen people, the photos with every chosen person (when
 * someone is chosen and no search is typed), then the people matching the search.
 */
data class PeopleUiState(
    val isLoading: Boolean = true,
    val query: String = "",
    /** Search field and chips: the filter, once someone is tagged. */
    val showSearch: Boolean = false,
    /** The band: "9 pessoas" and "214 fotos com alguém marcado". */
    val peopleCountText: String = "",
    val taggedPhotosText: String = "",
    /** People matching [query], listed under the results. */
    val people: List<PersonRow> = emptyList(),
    val selected: List<PersonRow> = emptyList(),
    val showResults: Boolean = false,
    val results: List<PhotoTile> = emptyList(),
    /** The people the results were computed for — what the viewer pages through. */
    val memberIds: Set<Long> = emptySet(),
    /** "Tudo / Fotos / Vídeos": shown only when a video is on the device. */
    val showMediaFilter: Boolean = false,
    val mediaFilter: MediaFilter = MediaFilter.ALL,
    /** "Fotos com Ana e João", over the results. */
    val resultTitle: String? = null,
    /** The text in place of the results grid when it is empty. */
    val resultEmptyText: String? = null,
    /** The text in place of the people list, when there is nobody to show. */
    val emptyText: String? = null,
)
