package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.state

import com.ipb.castelobranco.features.worshiphub.shared.domain.SundaySection

data class LyricsUiState(
    val lyrics: List<LyricsListItem> = emptyList(),
    val filteredLyrics: List<LyricsListItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val canEdit: Boolean = false,
    /** "Repertório de domingo" above the list; `null` when there is none or while searching. */
    val sundaySection: SundaySection? = null,
)

data class LyricsListItem(
    val id: Int,
    val songId: Int,
    val songName: String,
    val isPinned: Boolean = false,
)
