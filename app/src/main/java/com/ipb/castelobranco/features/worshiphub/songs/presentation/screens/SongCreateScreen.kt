package com.ipb.castelobranco.features.worshiphub.songs.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongCreateUiState
import com.ipb.castelobranco.features.worshiphub.songs.presentation.viewmodel.SongCreateViewModel

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

@Composable
private fun SongCreateContent(
    state: SongCreateUiState,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onSave: () -> Unit,
    onBackClick: () -> Unit,
) {
    BaseScreen(
        tabName       = "Nova Música",
        logoRes       = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick   = onBackClick,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value           = state.title,
                onValueChange   = onTitleChange,
                label           = { Text("Título") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                enabled         = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Next,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value           = state.artist,
                onValueChange   = onArtistChange,
                label           = { Text("Artista") },
                modifier        = Modifier.fillMaxWidth(),
                singleLine      = true,
                enabled         = !state.isSaving,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction      = ImeAction.Done,
                ),
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (state.saveError != null) {
                Text(
                    text     = state.saveError,
                    color    = MaterialTheme.colorScheme.error,
                    style    = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                } else {
                    Button(
                        onClick  = onSave,
                        enabled  = state.canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}
