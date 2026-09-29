package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.state.LyricsListItem
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.viewmodel.LyricsViewModel
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongContentListScreen
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongContentRow

private val Accent = BrandColors.Blue

@Composable
fun LyricsScreen(
    viewModel: LyricsViewModel,
    onLyricsClick: (id: Int) -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    SongContentListScreen(
        tabName           = "Letras",
        searchPlaceholder = "Pesquisar letras...",
        addItemLabel      = "Adicionar Letra",
        accentColor       = Accent,
        leadingIcon       = Icons.Default.Lyrics,
        rows              = state.filteredLyrics.map { it.toRow() },
        query             = state.query,
        isLoading         = state.isLoading,
        error             = state.error,
        canEdit           = state.canEdit,
        isRefreshing      = isRefreshing,
        onQueryChange     = viewModel::onQueryChange,
        onItemClick       = onLyricsClick,
        onTogglePin       = viewModel::onTogglePin,
        onRefresh         = viewModel::refresh,
        onCreateClick     = onCreateClick,
        onBackClick       = onBackClick,
    )
}

private fun LyricsListItem.toRow() = SongContentRow(
    id       = id,
    songId   = songId,
    songName = songName,
    isPinned = isPinned,
)
