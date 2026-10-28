package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.state.LyricsCreateUiState
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.viewmodel.LyricsCreateViewModel
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormEditor
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormHelp
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormTab
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormTabs
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.PreviewCard
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongFormScaffold
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongPicker

private const val TITLE = "Nova letra"
private const val SAVE_LABEL = "Salvar letra"
private const val EDITOR_LABEL = "Texto da letra"
private const val EDITOR_HELP = "Linha em branco separa as estrofes."
private const val NOTHING_YET = "Nada para mostrar ainda."

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
    var tab by rememberSaveable { mutableStateOf(FormTab.WRITE) }
    var editor by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(state.content, TextRange(state.content.length)))
    }
    val enabled = !state.isSaving
    BaseScreen(
        tabName       = TITLE,
        logoRes       = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick   = onBackClick,
    ) { innerPadding ->
        SongFormScaffold(
            innerPadding = innerPadding,
            saveLabel    = SAVE_LABEL,
            missing      = state.missing,
            isSaving     = state.isSaving,
            error        = state.saveError,
            onSave       = onSave,
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
            FormTabs(selected = tab, onSelect = { tab = it })
            when (tab) {
                FormTab.WRITE -> {
                    FormHelp(EDITOR_HELP)
                    FormEditor(EDITOR_LABEL, editor, enabled) { value ->
                        editor = value
                        onContentChange(value.text)
                    }
                }
                FormTab.PREVIEW -> PreviewCard {
                    val stanzas = state.previewStanzas
                    if (stanzas.isEmpty()) FormHelp(NOTHING_YET)
                    stanzas.forEachIndexed { index, stanza ->
                        if (index > 0) Spacer(Modifier.height(12.dp))
                        stanza.lines.forEach { line ->
                            Text(line, style = MaterialTheme.typography.titleMedium)
                        }
                    }
                }
            }
        }
    }
}
