package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.components.ChordBlockView
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.state.ChordChartCreateUiState
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel.ChordChartCreateViewModel
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.EditorSnippet
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.EditorTools
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormEditor
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormHelp
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormTab
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormTabs
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.InstrumentPicker
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.PreviewCard
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongFormScaffold
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongPicker
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.TonePicker
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.insert

private const val TITLE = "Nova cifra"
private const val SAVE_LABEL = "Salvar cifra"
private const val EDITOR_LABEL = "Texto da cifra"
private const val EDITOR_HELP = "Acorde entre colchetes antes da sílaba: [G]Santo. " +
    "Seção numa linha: {verse: Estrofe 1}, {chorus: Refrão}."
private const val NO_TONE = "Sem tom"
private const val NOTHING_YET = "Nada para mostrar ainda."

private val SNIPPETS = listOf(
    EditorSnippet("+ Estrofe", "{verse: Estrofe}", block = true),
    EditorSnippet("+ Refrão", "{chorus: Refrão}", block = true),
    EditorSnippet("+ Intro", "{intro}", block = true),
    EditorSnippet("[ ] Acorde", "[]", block = false, cursorBack = 1),
)

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
    var tab by rememberSaveable { mutableStateOf(FormTab.WRITE) }
    // The editor keeps its cursor here, so the buttons insert where the user is typing.
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
            TonePicker(selected = state.tone, enabled = enabled, onSelect = onToneChange)
            InstrumentPicker(value = state.instrument, enabled = enabled, onChange = onInstrumentChange)
            FormTabs(selected = tab, onSelect = { tab = it })
            when (tab) {
                FormTab.WRITE -> {
                    EditorTools(SNIPPETS, enabled) { snippet ->
                        editor = editor.insert(snippet)
                        onContentChange(editor.text)
                    }
                    FormHelp(EDITOR_HELP)
                    FormEditor(EDITOR_LABEL, editor, enabled) { value ->
                        editor = value
                        onContentChange(value.text)
                    }
                }
                FormTab.PREVIEW -> ChordPreview(state)
            }
        }
    }
}

@Composable
private fun ChordPreview(state: ChordChartCreateUiState) {
    PreviewCard {
        val header = if (state.tone.isBlank()) NO_TONE else "Tom ${state.tone}"
        FormHelp(listOf(header, state.instrument.trim()).filter { it.isNotEmpty() }.joinToString(" · "))
        Spacer(Modifier.height(4.dp))
        val blocks = state.previewBlocks
        if (blocks.isEmpty()) FormHelp(NOTHING_YET)
        blocks.forEachIndexed { index, block ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            ChordBlockView(block)
        }
        val unknown = state.unknownDirectives
        if (unknown > 0) {
            Text(
                text     = unknownText(unknown),
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private fun unknownText(count: Int): String =
    if (count == 1) {
        "1 linha entre { } não foi reconhecida como seção."
    } else {
        "$count linhas entre { } não foram reconhecidas como seção."
    }
