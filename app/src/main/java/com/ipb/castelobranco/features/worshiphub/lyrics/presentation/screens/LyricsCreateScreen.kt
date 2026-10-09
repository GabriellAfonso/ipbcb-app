package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.state.LyricsCreateUiState
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.viewmodel.LyricsCreateViewModel
import com.ipb.castelobranco.core.domain.model.Song

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
        onContentChange = viewModel::onContentChange,
        onSave          = viewModel::onSave,
        onBackClick     = onBackClick,
    )
}

@Composable
private fun LyricsCreateContent(
    state: LyricsCreateUiState,
    onSongQuery: (String) -> Unit,
    onSongSelected: (Song) -> Unit,
    onContentChange: (String) -> Unit,
    onSave: () -> Unit,
    onBackClick: () -> Unit,
) {
    BaseScreen(
        tabName       = "Nova Letra",
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
                value         = state.songQuery,
                onValueChange = onSongQuery,
                label         = { Text("Música") },
                modifier      = Modifier.fillMaxWidth(),
                singleLine    = true,
                enabled       = !state.isSaving,
            )

            if (state.filteredSongs.isNotEmpty()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                    items(state.filteredSongs) { song ->
                        Text(
                            text     = song.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSongSelected(song) }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            style    = MaterialTheme.typography.bodyMedium,
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value         = state.content,
                onValueChange = onContentChange,
                label         = { Text("Letra") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                enabled       = !state.isSaving,
                textStyle     = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                ),
            )

            Spacer(modifier = Modifier.height(12.dp))

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
                        enabled  = state.selectedSong != null && state.content.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Salvar")
                    }
                }
            }
        }
    }
}
