package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state

import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordBlock
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordProParser

data class ChordChartCreateUiState(
    val allSongs: List<Song> = emptyList(),
    val songQuery: String = "",
    val filteredSongs: List<Song> = emptyList(),
    val selectedSong: Song? = null,
    val content: String = "",
    val tone: String = "",
    val instrument: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,
    val savedSuccessfully: Boolean = false,
) {
    /** What still blocks "Salvar cifra", in the form's order. */
    val missing: List<String> get() = listOfNotNull(
        "música".takeIf { selectedSong == null },
        "tom".takeIf { tone.isBlank() },
        "instrumento".takeIf { instrument.isBlank() },
        "cifra".takeIf { content.isBlank() },
    )

    /** The preview: the chart as the chord chart screen draws it. */
    val previewBlocks: List<ChordBlock> get() = ChordProParser.parse(content).filter { block ->
        block.title != null || block.isIntro || block.lines.any { it.tokens.isNotEmpty() }
    }

    /** Lines between `{ }` the parser does not take as a section. */
    val unknownDirectives: Int get() = ChordProParser.unknownDirectives(content)
}
