package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state

import com.ipb.castelobranco.features.worshiphub.shared.domain.SundaySection

data class ChordChartsUiState(
    val charts: List<ChordChartListItem> = emptyList(),
    val filteredCharts: List<ChordChartListItem> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val canEdit: Boolean = false,
    /** "Repertório de domingo" above the list; `null` when there is none or while searching. */
    val sundaySection: SundaySection? = null,
)

data class ChordChartListItem(
    val id: Int,
    val songId: Int,
    val songName: String,
    val tone: String,
    val instrument: String,
    val isPinned: Boolean = false,
)
