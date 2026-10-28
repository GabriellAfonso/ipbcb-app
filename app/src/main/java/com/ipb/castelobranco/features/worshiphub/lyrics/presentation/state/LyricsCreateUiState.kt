package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.state

import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.parser.LyricsParser
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.parser.LyricsStanza

data class LyricsCreateUiState(
    val allSongs: List<Song> = emptyList(),
    val songQuery: String = "",
    val filteredSongs: List<Song> = emptyList(),
    val selectedSong: Song? = null,
    val content: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedSuccessfully: Boolean = false,
) {
    /** What still blocks "Salvar letra". */
    val missing: List<String> get() = listOfNotNull(
        "música".takeIf { selectedSong == null },
        "letra".takeIf { content.isBlank() },
    )

    /** The preview's stanzas, split as the lyrics screen splits them. */
    val previewStanzas: List<LyricsStanza> get() = LyricsParser.parse(content)
}
