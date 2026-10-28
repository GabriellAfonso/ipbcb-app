package com.ipb.castelobranco.features.worshiphub.songs.presentation.screens

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormHelp
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormTextField
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.SongFormScaffold
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongCreateUiState
import com.ipb.castelobranco.features.worshiphub.songs.presentation.viewmodel.SongCreateViewModel

private const val TITLE = "Nova música"
private const val SAVE_LABEL = "Salvar música"
private const val TITLE_LABEL = "Título"
private const val ARTIST_LABEL = "Artista"
private const val HELP = "Depois de salvar, a música aparece em Músicas e já pode receber cifra e letra."

@Composable
fun SongCreateScreen(
    viewModel: SongCreateViewModel,
    onBackClick: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.savedSuccessfully) {
        if (state.savedSuccessfully) onBackClick()
    }

    SongCreateContent(
        state          = state,
        onTitleChange  = viewModel::onTitleChange,
        onArtistChange = viewModel::onArtistChange,
        onSave         = viewModel::onSave,
        onBackClick    = onBackClick,
    )
}

/** "Com prévia" form, without a preview: title and artist (`specs/worshiphub/spec.md` §3.3). */
@Composable
private fun SongCreateContent(
    state: SongCreateUiState,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onSave: () -> Unit,
    onBackClick: () -> Unit,
) {
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
            FormTextField(
                label           = TITLE_LABEL,
                value           = state.title,
                onValueChange   = onTitleChange,
                enabled         = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Next,
                ),
            )
            FormTextField(
                label           = ARTIST_LABEL,
                value           = state.artist,
                onValueChange   = onArtistChange,
                enabled         = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Done,
                ),
            )
            FormHelp(HELP)
        }
    }
}
