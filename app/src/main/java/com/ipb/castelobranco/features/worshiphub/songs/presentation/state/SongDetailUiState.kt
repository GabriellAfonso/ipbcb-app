package com.ipb.castelobranco.features.worshiphub.songs.presentation.state

import com.ipb.castelobranco.features.worshiphub.songs.domain.model.SongFieldErrors

data class SongDetailUiState(
    val songName: String = "",
    val artist: String = "",
    val playCount: Int = 0,
    val tones: List<String> = emptyList(),
    val lastSundays: List<String> = emptyList(),
    val chordCharts: List<ChordChartOption> = emptyList(),
    val hasLyrics: Boolean = false,
    val lyricsId: Int? = null,
    val youtubeLink: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val canEdit: Boolean = false,
    val canDelete: Boolean = false,
    val edit: SongEditFormState? = null,
    val isDeleting: Boolean = false,
    val deleteError: String? = null,
) {
    val isEditing: Boolean get() = edit != null
}

/** The edit form; present only while the song is being edited. */
data class SongEditFormState(
    val title: String = "",
    val artist: String = "",
    val youtubeLink: String = "",
    val fieldErrors: SongFieldErrors = SongFieldErrors(),
    val isSaving: Boolean = false,
    val saveError: String? = null,
)

data class ChordChartOption(
    val id: Int,
    val tone: String,
    val instrument: String,
)

sealed interface SongDetailEvent {
    /** The song no longer exists; the screen leaves. */
    data object Deleted : SongDetailEvent
}
