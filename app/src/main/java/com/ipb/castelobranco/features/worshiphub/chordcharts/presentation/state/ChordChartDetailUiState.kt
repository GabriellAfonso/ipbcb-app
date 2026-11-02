package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state

import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordBlock
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordProParser

data class ChordChartDetailUiState(
    val songName: String = "",
    val artist: String = "",
    val tone: String = "",
    val instrument: String = "",
    val blocks: List<ChordBlock> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val rawContent: String = "",
    val canEdit: Boolean = false,
    val isEditing: Boolean = false,
    val editContent: String = "",
    val editTone: String = "",
    val editInstrument: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,
) {
    /** What still blocks "Salvar cifra" while editing, in the form's order. */
    val editMissing: List<String> get() = listOfNotNull(
        "tom".takeIf { editTone.isBlank() },
        "instrumento".takeIf { editInstrument.isBlank() },
        "cifra".takeIf { editContent.isBlank() },
    )

    val editPreviewBlocks: List<ChordBlock> get() = ChordProParser.previewBlocks(editContent)

    val editUnknownDirectives: Int get() = ChordProParser.unknownDirectives(editContent)
}
