package com.ipb.castelobranco.features.worshiphub.register.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.domain.constants.MUSIC_TONES
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.core.domain.util.normalize
import com.ipb.castelobranco.features.worshiphub.register.presentation.components.SundayDatePickerDialog
import com.ipb.castelobranco.features.worshiphub.register.presentation.state.PrefillState
import com.ipb.castelobranco.features.worshiphub.register.presentation.state.SundayRegistrationEvent
import com.ipb.castelobranco.features.worshiphub.register.presentation.state.SundayRegistrationUiState
import com.ipb.castelobranco.features.worshiphub.register.presentation.state.SundaySongRowState
import com.ipb.castelobranco.features.worshiphub.register.presentation.viewmodel.SundayRegistrationViewModel
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormSaveBar
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.FormTextField

private const val TITLE = "Registrar domingo"
private const val BAND_TITLE = "Culto de domingo"
private const val CHANGE_DATE = "Trocar"
private const val PICK_SONG = "Escolher música"
private const val TONE_PLACEHOLDER = "Tom"
private const val ADD_SONG = "Adicionar música"
private const val REMOVE_LABEL = "Remover"
private const val SAVING_LABEL = "Registrando…"
private const val MISSING_PREFIX = "Para registrar: "
private const val SEARCH_LABEL = "Buscar música ou artista"
private const val NO_SONG_FOUND = "Nenhuma música encontrada"
private const val NO_SETLIST = "Nenhum repertório salvo para esse domingo."
private const val LOADING_SETLIST = "Carregando o repertório do domingo..."
private const val RETRY_LABEL = "Tentar novamente"
private const val CONFIRM_TITLE = "Registrar domingo?"
private const val CONFIRM_LABEL = "Registrar"
private const val REVIEW_LABEL = "Revisar"
private const val TONE_COLUMNS = 6

private const val MUTED_ALPHA = 0.75f
private const val CHIP_FILL_ALPHA = 0.14f
private const val CHIP_BORDER_ALPHA = 0.22f
private const val SHADOW_ALPHA = 0.12f

private val BandCorner = 24.dp
private val CardCorner = 16.dp
private val PrefilledColor = Color(0xFF8ED1BA)
private val SheetListMaxHeight = 420.dp
private val DASH = floatArrayOf(12f, 8f)

// ── Actions por domínio ───────────────────────────────────────────────────────

data class SundayRegistrationScreenActions(
    val onSubmit: () -> Unit,
    val onRetryPrefill: () -> Unit = {},
)

data class SundayRegistrationActions(
    val onOpenDatePicker: () -> Unit,
    val onSongSelect: (position: Int, song: Song) -> Unit,
    val onToneChange: (position: Int, tone: String) -> Unit,
    val onAddRow: () -> Unit,
    val onRemoveRow: (position: Int) -> Unit,
)

/** Which picker is open over the screen, for which row. */
private sealed interface RowPicker {
    val position: Int

    data class SongPicker(override val position: Int) : RowPicker
    data class TonePicker(override val position: Int) : RowPicker
}

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun SundayRegistrationScreen(
    onBackClick: () -> Unit,
    viewModel: SundayRegistrationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.onEvent(SundayRegistrationEvent.Init) }

    LaunchedEffect(state.snackbarMessage) {
        val msg = state.snackbarMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.onEvent(SundayRegistrationEvent.SnackbarShown)
    }

    if (state.showDatePicker) {
        SundayDatePickerDialog(
            initialDate = state.selectedDate,
            onDismiss = { viewModel.onEvent(SundayRegistrationEvent.DismissDatePicker) },
            onConfirm = { viewModel.onEvent(SundayRegistrationEvent.DatePicked(it)) }
        )
    }

    val screenActions = SundayRegistrationScreenActions(
        onSubmit = { viewModel.onEvent(SundayRegistrationEvent.Submit) },
        onRetryPrefill = { viewModel.onEvent(SundayRegistrationEvent.RetryPrefill) },
    )

    val sundayActions = SundayRegistrationActions(
        onOpenDatePicker = { viewModel.onEvent(SundayRegistrationEvent.OpenDatePicker) },
        onSongSelect = { pos, song -> viewModel.onEvent(SundayRegistrationEvent.SundaySongSelected(pos, song)) },
        onToneChange = { pos, tone -> viewModel.onEvent(SundayRegistrationEvent.SundayToneChanged(pos, tone)) },
        onAddRow = { viewModel.onEvent(SundayRegistrationEvent.AddSundayRow) },
        onRemoveRow = { viewModel.onEvent(SundayRegistrationEvent.RemoveSundayRow(it)) },
    )

    SundayRegistrationContent(
        state = state,
        screenActions = screenActions,
        sundayActions = sundayActions,
        snackbarHostState = snackbarHostState,
        onBackClick = onBackClick
    )
}

// ── Stateless screen ──────────────────────────────────────────────────────────

/** "Faixa e cards numerados" (`specs/worshiphub/spec.md` §2.6). */
@Composable
fun SundayRegistrationContent(
    state: SundayRegistrationUiState,
    screenActions: SundayRegistrationScreenActions,
    sundayActions: SundayRegistrationActions,
    snackbarHostState: SnackbarHostState,
    onBackClick: () -> Unit
) {
    var picker by remember { mutableStateOf<RowPicker?>(null) }
    var showConfirm by rememberSaveable { mutableStateOf(false) }

    BaseScreen(
        tabName = TITLE,
        logoRes = R.drawable.ic_register,
        showBackArrow = true,
        onBackClick = onBackClick
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                ) {
                    RegisterBand(state, sundayActions.onOpenDatePicker, screenActions.onRetryPrefill)
                    Column(
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        state.sundayRows.forEach { row ->
                            SongCard(
                                row = row,
                                song = state.songOf(row),
                                error = state.sundayRowErrors[row.position],
                                canRemove = state.sundayRows.size > 1,
                                onSongClick = { picker = RowPicker.SongPicker(row.position) },
                                onToneClick = { picker = RowPicker.TonePicker(row.position) },
                                onRemove = { sundayActions.onRemoveRow(row.position) },
                            )
                        }
                        AddSongButton(sundayActions.onAddRow)
                    }
                }
                FormSaveBar(
                    label = TITLE,
                    missing = listOfNotNull(state.missingText),
                    isSaving = state.isSubmitting,
                    error = null,
                    onSave = { showConfirm = true },
                    savingLabel = SAVING_LABEL,
                    missingPrefix = MISSING_PREFIX,
                )
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp)
            )
        }
    }

    when (val open = picker) {
        is RowPicker.SongPicker -> SongPickerSheet(
            position = open.position,
            songs = state.availableSongs,
            onPick = { song ->
                sundayActions.onSongSelect(open.position, song)
                picker = null
            },
            onDismiss = { picker = null },
        )
        is RowPicker.TonePicker -> TonePickerSheet(
            position = open.position,
            selected = state.sundayRows.firstOrNull { it.position == open.position }?.tone.orEmpty(),
            onPick = { tone ->
                sundayActions.onToneChange(open.position, tone)
                picker = null
            },
            onDismiss = { picker = null },
        )
        null -> Unit
    }

    if (showConfirm) {
        ConfirmDialog(
            state = state,
            onConfirm = {
                showConfirm = false
                screenActions.onSubmit()
            },
            onDismiss = { showConfirm = false },
        )
    }
}

/** The date with "Trocar", and where the rows came from. */
@Composable
private fun RegisterBand(state: SundayRegistrationUiState, onChangeDate: () -> Unit, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = BandCorner, bottomEnd = BandCorner))
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 16.dp),
    ) {
        Text(BAND_TITLE, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = MUTED_ALPHA))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.dateTitle,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            // Confirming a Sunday: the date belongs to the setlist and cannot change.
            if (state.prefillDate == null) {
                Text(
                    text = CHANGE_DATE,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(percent = 50))
                        .background(Color.White.copy(alpha = CHIP_FILL_ALPHA))
                        .border(1.dp, Color.White.copy(alpha = CHIP_BORDER_ALPHA), RoundedCornerShape(percent = 50))
                        .clickable(onClick = onChangeDate)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        PrefillLine(state.prefill, state.prefillSavedBy, onRetry)
    }
}

@Composable
private fun PrefillLine(prefill: PrefillState, savedBy: String?, onRetry: () -> Unit) {
    when (prefill) {
        PrefillState.Loaded -> Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = PrefilledColor, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = if (savedBy != null) {
                    "Repertório salvo por $savedBy. Confira e ajuste."
                } else {
                    "Repertório salvo. Confira e ajuste."
                },
                style = MaterialTheme.typography.bodySmall,
                color = PrefilledColor,
            )
        }
        PrefillState.NotFound -> Text(
            text = NO_SETLIST,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = MUTED_ALPHA),
            modifier = Modifier.padding(top = 10.dp),
        )
        PrefillState.Loading -> Column(Modifier.padding(top = 10.dp)) {
            Text(
                text = LOADING_SETLIST,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = MUTED_ALPHA),
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White,
                trackColor = Color.White.copy(alpha = CHIP_FILL_ALPHA),
            )
        }
        is PrefillState.Failed -> Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = prefill.message,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRetry) {
                Text(RETRY_LABEL, color = Color.White, fontWeight = FontWeight.SemiBold)
            }
        }
        PrefillState.None -> Unit
    }
}

/** A numbered song: tap the song to pick it, the tone button for the tone, "×" to remove it. */
@Composable
private fun SongCard(
    row: SundaySongRowState,
    song: Song?,
    error: String?,
    canRemove: Boolean,
    onSongClick: () -> Unit,
    onToneClick: () -> Unit,
    onRemove: () -> Unit,
) {
    val shape = RoundedCornerShape(CardCorner)
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(6.dp, shape, spotColor = Color.Black.copy(alpha = SHADOW_ALPHA))
                .clip(shape)
                .background(highlightCardColor())
                .padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = row.position.toString(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onSongClick)
                    .padding(vertical = 4.dp),
            ) {
                if (song == null) {
                    Text(
                        text = PICK_SONG,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (song.artist.isNotBlank()) {
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            ToneButton(row.tone, onToneClick)
            if (canRemove) {
                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = REMOVE_LABEL,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            } else {
                Spacer(Modifier.width(8.dp))
            }
        }
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 12.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun ToneButton(tone: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    val isSet = tone.isNotBlank()
    Box(
        modifier = Modifier
            .widthIn(min = 46.dp)
            .height(34.dp)
            .clip(shape)
            .background(if (isSet) ipbGreen else Color.Transparent)
            .border(BorderStroke(1.dp, if (isSet) ipbGreen else MaterialTheme.colorScheme.outlineVariant), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tone.ifBlank { TONE_PLACEHOLDER },
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (isSet) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** "Adicionar música": a dashed button under the cards. */
@Composable
private fun AddSongButton(onClick: () -> Unit) {
    val outline = MaterialTheme.colorScheme.outlineVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .drawBehind {
                drawRoundRect(
                    color = outline,
                    cornerRadius = CornerRadius(CardCorner.toPx()),
                    style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(DASH)),
                )
            }
            .clip(RoundedCornerShape(CardCorner))
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = ADD_SONG,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SongPickerSheet(position: Int, songs: List<Song>, onPick: (Song) -> Unit, onDismiss: () -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val matches = remember(songs, query) { songsMatching(songs, query) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)) {
            SheetTitle("${position}ª música")
            FormTextField(label = SEARCH_LABEL, value = query, onValueChange = { query = it })
            Spacer(Modifier.height(8.dp))
            if (matches.isEmpty()) {
                Text(
                    text = NO_SONG_FOUND,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                )
            }
            LazyColumn(Modifier.heightIn(max = SheetListMaxHeight)) {
                items(matches, key = { it.id }) { song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(song) }
                            .padding(horizontal = 4.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

/** By title or artist, accents and case ignored. */
private fun songsMatching(songs: List<Song>, query: String): List<Song> {
    val q = query.normalize()
    return songs.filter { song ->
        song.title.normalize().contains(q, ignoreCase = true) || song.artist.normalize().contains(q, ignoreCase = true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TonePickerSheet(position: Int, selected: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SheetTitle("Tom da ${position}ª música")
            MUSIC_TONES.chunked(TONE_COLUMNS).forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    line.forEach { tone ->
                        Box(Modifier.weight(1f)) { ToneChoice(tone, tone == selected) { onPick(tone) } }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToneChoice(tone: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(if (selected) ipbGreen else Color.Transparent)
            .border(BorderStroke(1.dp, if (selected) ipbGreen else MaterialTheme.colorScheme.outlineVariant), shape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = tone,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 12.dp),
    )
}

/** "Registrar domingo?" with what goes to the Tables; nothing is sent without it. */
@Composable
private fun ConfirmDialog(state: SundayRegistrationUiState, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val lines = state.confirmLines
    val count = if (lines.size == 1) "1 música" else "${lines.size} músicas"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(CONFIRM_TITLE) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("$count de ${state.dateTitle} vão para as Tabelas:")
                Spacer(Modifier.height(4.dp))
                lines.forEach { line ->
                    Text(line, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(CONFIRM_LABEL, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(REVIEW_LABEL) } },
    )
}
