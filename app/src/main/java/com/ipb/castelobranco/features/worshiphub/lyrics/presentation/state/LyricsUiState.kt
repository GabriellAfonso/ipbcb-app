package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.state

data class LyricsUiState(
    val lyrics: List<LyricsListItem> = emptyList(),
    val filteredLyrics: List<LyricsListItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val canEdit: Boolean = false,
)

data class LyricsListItem(
    val id: Int,
    val songId: Int,
    val songName: String,
    val isPinned: Boolean = false,
)
