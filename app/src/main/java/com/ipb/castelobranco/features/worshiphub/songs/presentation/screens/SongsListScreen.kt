package com.ipb.castelobranco.features.worshiphub.songs.presentation.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongContentChip
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongContentListScreen
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongContentRow
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongListItem
import com.ipb.castelobranco.features.worshiphub.songs.presentation.viewmodel.SongsListViewModel

private val Accent = BrandColors.DarkGreen

@Composable
fun SongsListScreen(
    viewModel: SongsListViewModel,
    onSongClick: (id: Int) -> Unit,
    onCreateClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val secondary = MaterialTheme.colorScheme.secondary

    SongContentListScreen(
        tabName           = "Músicas",
        searchPlaceholder = "Pesquisar músicas...",
        addItemLabel      = "Adicionar Música",
        accentColor       = Accent,
        leadingIcon       = Icons.Default.LibraryMusic,
        rows              = state.filteredSongs.map { it.toRow(secondary) },
        query             = state.query,
        isLoading         = state.isLoading,
        error             = state.error,
        isRefreshing      = isRefreshing,
        onQueryChange     = viewModel::onQueryChange,
        onItemClick       = onSongClick,
        onRefresh         = viewModel::refresh,
        onBackClick       = onBackClick,
        canEdit           = state.canEdit,
        onCreateClick     = onCreateClick,
    )
}

private fun SongListItem.toRow(artistColor: Color) = SongContentRow(
    id       = id,
    songName = title,
    chips    = if (artist.isBlank()) emptyList()
               else listOf(SongContentChip(text = artist, color = artistColor)),
)
