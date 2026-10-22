package com.ipb.castelobranco.features.worshiphub.songs.presentation.state

data class SongCreateUiState(
    val title: String = "",
    val artist: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedSuccessfully: Boolean = false,
) {
    val canSave: Boolean get() = !isSaving && title.isNotBlank() && artist.isNotBlank()
}
