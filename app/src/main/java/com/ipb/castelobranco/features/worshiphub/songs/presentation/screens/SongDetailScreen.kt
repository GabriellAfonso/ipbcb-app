package com.ipb.castelobranco.features.worshiphub.songs.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.ChordChartOption
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongDetailEvent
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongDetailUiState
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongEditFormState
import com.ipb.castelobranco.features.worshiphub.songs.presentation.viewmodel.SongDetailViewModel

private val Green = BrandColors.Green
private val YouTubeRed = BrandColors.YouTubeRed

@Composable
fun SongDetailScreen(
    viewModel: SongDetailViewModel,
    onChordChartClick: (id: Int) -> Unit,
    onLyricsClick: (id: Int) -> Unit,
    onBackClick: () -> Unit,
    onDeleted: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                SongDetailEvent.Deleted -> onDeleted()
            }
        }
    }

    SongDetailContent(
        state                = state,
        onChordChartClick    = onChordChartClick,
        onLyricsClick        = onLyricsClick,
        onBackClick          = onBackClick,
        onEnterEdit          = viewModel::enterEditMode,
        onTitleChange        = viewModel::onTitleChange,
        onArtistChange       = viewModel::onArtistChange,
        onYoutubeLinkChange  = viewModel::onYoutubeLinkChange,
        onCancelEdit         = viewModel::cancelEdit,
        onSaveEdit           = viewModel::saveEdit,
        onDelete             = viewModel::deleteSong,
        onDismissDeleteError = viewModel::dismissDeleteError,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SongDetailContent(
    state: SongDetailUiState,
    onChordChartClick: (id: Int) -> Unit,
    onLyricsClick: (id: Int) -> Unit,
    onBackClick: () -> Unit,
    onEnterEdit: () -> Unit,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onYoutubeLinkChange: (String) -> Unit,
    onCancelEdit: () -> Unit,
    onSaveEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismissDeleteError: () -> Unit,
) {
    val context = LocalContext.current
    var showChordChartDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    BaseScreen(
        tabName       = "Música",
        logoRes       = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick   = onBackClick,
        extraActions  = {
            if (state.canEdit && !state.isLoading && state.error == null && !state.isDeleting) {
                SongOverflowMenu(
                    isEditing = state.isEditing,
                    isSaving  = state.edit?.isSaving == true,
                    canDelete = state.canDelete,
                    onEdit    = onEnterEdit,
                    onDelete  = { showDeleteConfirm = true },
                    onSave    = onSaveEdit,
                    onCancel  = onCancelEdit,
                )
            }
        },
    ) { innerPadding ->
        val edit = state.edit
        when {
            state.isLoading || state.isDeleting -> {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(color = Green)
                }
            }
            state.error != null -> {
                Column(
                    modifier            = Modifier.fillMaxSize().padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text  = state.error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            edit != null -> SongEditForm(
                form                = edit,
                onTitleChange       = onTitleChange,
                onArtistChange      = onArtistChange,
                onYoutubeLinkChange = onYoutubeLinkChange,
                modifier            = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .imePadding(),
            )
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    // Header and stats can be long-pressed to copy (song name, artist, tones, dates).
                    SelectionContainer {
                        Column {
                            Text(
                                text       = state.songName,
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color      = Green,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text  = state.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Stats
                            Text(
                                text       = "Estatísticas",
                                style      = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text  = "Tocada ${state.playCount} " +
                                    "${if (state.playCount == 1) "vez" else "vezes"} aos domingos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )

                            if (state.tones.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text  = "Tons: ${state.tones.joinToString(", ")}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            if (state.lastSundays.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text       = "Últimos domingos:",
                                    style      = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color      = MaterialTheme.colorScheme.onSurface,
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    state.lastSundays.forEach { date ->
                                        AssistChip(
                                            onClick = {},
                                            label   = { Text(date) },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons
                    Text(
                        text       = "Ações",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    ActionButton(
                        label      = "Cifra",
                        icon       = Icons.Filled.MusicNote,
                        enabled    = state.chordCharts.isNotEmpty(),
                        buttonColor = Green,
                        onClick    = {
                            when (state.chordCharts.size) {
                                1    -> onChordChartClick(state.chordCharts.first().id)
                                else -> showChordChartDialog = true
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    ActionButton(
                        label      = "Letra",
                        icon       = Icons.Filled.TextSnippet,
                        enabled    = state.hasLyrics,
                        buttonColor = Green,
                        onClick    = { state.lyricsId?.let(onLyricsClick) },
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val hasYoutube = !state.youtubeLink.isNullOrBlank()
                    ActionButton(
                        label      = "YouTube",
                        icon       = Icons.Filled.PlayArrow,
                        enabled    = hasYoutube,
                        buttonColor = YouTubeRed,
                        onClick = {
                            state.youtubeLink?.let { link ->
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                                context.startActivity(intent)
                            }
                        },
                    )
                }
            }
        }
    }

    if (showChordChartDialog) {
        ChordChartPickerDialog(
            options   = state.chordCharts,
            onSelect  = { id ->
                showChordChartDialog = false
                onChordChartClick(id)
            },
            onDismiss = { showChordChartDialog = false },
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title            = { Text("Excluir música?") },
            text             = {
                Text(
                    "“${state.songName}” será excluída junto com as cifras e a letra. " +
                        "Essa ação não pode ser desfeita.",
                )
            },
            confirmButton    = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                    },
                ) {
                    Text("Excluir", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton    = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    state.deleteError?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissDeleteError,
            title            = { Text("Não foi possível excluir") },
            text             = { Text(message) },
            confirmButton    = {
                TextButton(onClick = onDismissDeleteError) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun SongOverflowMenu(
    isEditing: Boolean,
    isSaving: Boolean,
    canDelete: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector        = Icons.Filled.MoreVert,
                contentDescription = "Menu",
                tint               = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (isEditing) {
                DropdownMenuItem(
                    text    = { Text("Salvar") },
                    onClick = { expanded = false; onSave() },
                    enabled = !isSaving,
                )
                DropdownMenuItem(
                    text    = { Text("Cancelar") },
                    onClick = { expanded = false; onCancel() },
                    enabled = !isSaving,
                )
            } else {
                DropdownMenuItem(
                    text    = { Text("Editar") },
                    onClick = { expanded = false; onEdit() },
                )
                if (canDelete) {
                    DropdownMenuItem(
                        text    = { Text("Excluir", color = MaterialTheme.colorScheme.error) },
                        onClick = { expanded = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun SongEditForm(
    form: SongEditFormState,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onYoutubeLinkChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier            = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (form.isSaving) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Green)
            }
        }
        form.saveError?.let { message ->
            Text(
                text  = message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        SongEditField(
            label         = "Nome",
            value         = form.title,
            error         = form.fieldErrors.title,
            enabled       = !form.isSaving,
            onValueChange = onTitleChange,
        )
        SongEditField(
            label         = "Artista",
            value         = form.artist,
            error         = form.fieldErrors.artist,
            enabled       = !form.isSaving,
            onValueChange = onArtistChange,
        )
        SongEditField(
            label           = "Link do YouTube",
            value           = form.youtubeLink,
            error           = form.fieldErrors.youtubeLink,
            enabled         = !form.isSaving,
            onValueChange   = onYoutubeLinkChange,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        )
    }
}

@Composable
private fun SongEditField(
    label: String,
    value: String,
    error: String?,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    keyboardOptions: KeyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
) {
    OutlinedTextField(
        value           = value,
        onValueChange   = onValueChange,
        label           = { Text(label) },
        isError         = error != null,
        supportingText  = error?.let { { Text(it) } },
        enabled         = enabled,
        singleLine      = true,
        keyboardOptions = keyboardOptions,
        modifier        = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    buttonColor: Color,
    onClick: () -> Unit,
) {
    Button(
        onClick  = onClick,
        enabled  = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(12.dp),
        colors   = ButtonDefaults.buttonColors(
            containerColor         = buttonColor,
            contentColor           = Color.White,
            disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
        ),
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null,
            modifier           = Modifier.size(20.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(label)
    }
}

@Composable
private fun ChordChartPickerDialog(
    options: List<ChordChartOption>,
    onSelect: (id: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title            = { Text("Escolher cifra") },
        text             = {
            Column {
                options.forEach { option ->
                    TextButton(
                        onClick  = { onSelect(option.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("Tom: ${option.tone}", color = Green)
                            Text(option.instrument, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
