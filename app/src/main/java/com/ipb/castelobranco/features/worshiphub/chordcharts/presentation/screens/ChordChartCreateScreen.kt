package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.components.ChordChartEditor
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state.ChordChartCreateUiState
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel.ChordChartCreateViewModel
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.InstrumentPicker
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongFormScaffold
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongFormSaveAction
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongPicker
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.TonePicker

private const val TITLE = "Nova cifra"
private const val SAVE_LABEL = "Salvar cifra"

@Composable
fun ChordChartCreateScreen(
    viewModel: ChordChartCreateViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onBackClick()
    }

    ChordChartCreateContent(
        state              = state,
        onSongQuery        = viewModel::onSongQueryChange,
        onSongSelected     = viewModel::onSongSelected,
        onSongCleared      = viewModel::onSongCleared,
        onContentChange    = viewModel::onContentChange,
        onToneChange       = viewModel::onToneChange,
        onInstrumentChange = viewModel::onInstrumentChange,
        onSave             = viewModel::onSave,
        onBackClick        = onBackClick,
    )
}

/** "Com prévia" (`specs/worshiphub/spec.md` §4.2). */
@Composable
private fun ChordChartCreateContent(
    state: ChordChartCreateUiState,
    onSongQuery: (String) -> Unit,
    onSongSelected: (Song) -> Unit,
    onSongCleared: () -> Unit,
    onContentChange: (String) -> Unit,
    onToneChange: (String) -> Unit,
    onInstrumentChange: (String) -> Unit,
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
                ChordChartEditor(
                    content           = state.content,
                    tone              = state.tone,
                    instrument        = state.instrument,
                    previewBlocks     = state.previewBlocks,
                    unknownDirectives = state.unknownDirectives,
                    enabled           = enabled,
                    onContentChange   = onContentChange,
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
            TonePicker(selected = state.tone, enabled = enabled, onSelect = onToneChange)
            InstrumentPicker(value = state.instrument, enabled = enabled, onChange = onInstrumentChange)
        }
    }
}
