package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state

import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.parser.ChordBlock

data class ChordChartDetailUiState(
    val songName: String = "",
    val tone: String = "",
    val blocks: List<ChordBlock> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val rawContent: String = "",
    val canEdit: Boolean = false,
    val isEditing: Boolean = false,
    val editContent: String = "",
    val isSaving: Boolean = false,
    val saveError: String? = null,
)
