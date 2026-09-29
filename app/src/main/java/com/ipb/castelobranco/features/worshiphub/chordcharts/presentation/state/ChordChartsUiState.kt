package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state

data class ChordChartsUiState(
    val charts: List<ChordChartListItem> = emptyList(),
    val filteredCharts: List<ChordChartListItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val canEdit: Boolean = false,
)

data class ChordChartListItem(
    val id: Int,
    val songId: Int,
    val songName: String,
    val tone: String,
    val instrument: String,
    val isPinned: Boolean = false,
)
