package com.ipb.castelobranco.features.worshiphub.songs.presentation.state

data class SongsListUiState(
    val songs: List<SongListItem> = emptyList(),
    val filteredSongs: List<SongListItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    /** `songs` ≥ `manage`: shows the menu with "Adicionar Música". */
    val canEdit: Boolean = false,
)

data class SongListItem(
    val id: Int,
    val title: String,
    val artist: String,
)
