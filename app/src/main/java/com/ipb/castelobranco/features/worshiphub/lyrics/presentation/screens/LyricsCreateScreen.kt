package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.components.LyricsEditor
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.state.LyricsCreateUiState
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.viewmodel.LyricsCreateViewModel
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongFormScaffold
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongFormSaveAction
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongPicker

private const val TITLE = "Nova letra"
private const val SAVE_LABEL = "Salvar letra"

@Composable
fun LyricsCreateScreen(
    viewModel: LyricsCreateViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onBackClick()
    }

    LyricsCreateContent(
        state           = state,
        onSongQuery     = viewModel::onSongQueryChange,
        onSongSelected  = viewModel::onSongSelected,
        onSongCleared   = viewModel::onSongCleared,
        onContentChange = viewModel::onContentChange,
        onSave          = viewModel::onSave,
        onBackClick     = onBackClick,
    )
}

/** "Com prévia" (`specs/worshiphub/spec.md` §5.2). */
@Composable
private fun LyricsCreateContent(
    state: LyricsCreateUiState,
    onSongQuery: (String) -> Unit,
    onSongSelected: (Song) -> Unit,
    onSongCleared: () -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onBackClick: () -> Unit,
) {
    val enabled = !state.isSaving
    BaseScreen(
        tabName       = TITLE,
        logoRes       = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick   = onBackClick,
        extraActions  = { SongFormSaveAction(SAVE_LABEL, state.missing, state.isSaving, onSave) },
    ) { innerPadding ->
        SongFormScaffold(
            innerPadding = innerPadding,
            saveLabel    = SAVE_LABEL,
            missing      = state.missing,
            isSaving     = state.isSaving,
            error        = state.saveError,
            onSave       = onSave,
            editor       = {
                LyricsEditor(
                    content         = state.content,
                    previewStanzas  = state.previewStanzas,
                    enabled         = enabled,
                    onContentChange = onContentChange,
                )
            },
        ) {
            SongPicker(
                query         = state.songQuery,
                results       = state.filteredSongs,
                selected      = state.selectedSong,
                enabled       = enabled,
                onQueryChange = onSongQuery,
                onSelect      = onSongSelected,
                onClear       = onSongCleared,
            )
        }
    }
}
