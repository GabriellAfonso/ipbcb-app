package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens

import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.toUi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state.ChordChartListItem
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel.ChordChartsViewModel
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongContentChip
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongContentListScreen
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongContentRow

private val Green = BrandColors.Green
private val DarkGreen = BrandColors.DarkGreen

@Composable
fun ChordChartsScreen(
    viewModel: ChordChartsViewModel,
    onChordChartClick: (id: Int) -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val secondary = MaterialTheme.colorScheme.secondary

    SongContentListScreen(
        tabName           = "Cifras",
        searchPlaceholder = "Pesquisar cifras...",
        addItemLabel      = "Adicionar Cifra",
        accentColor       = Green,
        leadingIcon       = Icons.Default.MusicNote,
        rows              = state.filteredCharts.map { it.toRow(secondary) },
        query             = state.query,
        searchLyrics      = state.searchLyrics,
        isLoading         = state.isLoading,
        error             = state.error,
        canEdit           = state.canEdit,
        isRefreshing      = isRefreshing,
        onQueryChange     = viewModel::onQueryChange,
        onSearchLyricsChange = viewModel::onSearchLyricsChange,
        onItemClick       = onChordChartClick,
        onTogglePin       = viewModel::onTogglePin,
        onRefresh         = viewModel::refresh,
        onCreateClick     = onCreateClick,
        onBackClick       = onBackClick,
        sundaySection     = state.sundaySection?.toUi(),
    )
}

private fun ChordChartListItem.toRow(instrumentColor: Color) = SongContentRow(
    id       = id,
    songId   = songId,
    songName = songName,
    isPinned = isPinned,
    snippet  = lyricsSnippet,
    chips    = listOf(
        SongContentChip(text = tone, color = DarkGreen),
        SongContentChip(text = instrument, color = instrumentColor),
    ),
)
