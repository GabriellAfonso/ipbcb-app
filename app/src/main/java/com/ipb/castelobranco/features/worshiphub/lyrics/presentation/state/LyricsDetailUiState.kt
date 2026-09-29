package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.state

import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.parser.LyricsStanza

data class LyricsDetailUiState(
    val songName: String = "",
    val stanzas: List<LyricsStanza> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val rawContent: String = "",
    val canEdit: Boolean = false,
    val isEditing: Boolean = false,
    val editContent: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,
)
