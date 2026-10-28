package com.ipb.castelobranco.features.worshiphub.songs.presentation.screens

import android.content.Intent
import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.PanelAction
import com.ipb.castelobranco.core.presentation.components.PanelHeader
import com.ipb.castelobranco.core.presentation.components.PanelMoreButton
import com.ipb.castelobranco.core.presentation.components.TopActionPanel
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.worshiphub.shared.presentation.components.EditBarActions
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongDetailEvent
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongDetailUiState
import com.ipb.castelobranco.features.worshiphub.songs.presentation.state.SongEditFormState
import com.ipb.castelobranco.features.worshiphub.songs.presentation.viewmodel.SongDetailViewModel

private val Green = BrandColors.Green
private val YouTubeRed = BrandColors.YouTubeRed

private const val TITLE = "Música"
private const val NO_VALUE = "—"
private const val OPEN_SECTION = "Abrir"
private const val SUNDAYS_SECTION = "Últimos domingos"
private const val CHORD_LABEL = "Cifra"
private const val LYRICS_LABEL = "Letra"
private const val YOUTUBE_LABEL = "YouTube"
private const val NOT_REGISTERED = "Ainda não cadastrada"
private const val EDIT_LABEL = "Editar música"
private const val EDIT_HINT = "Nome, artista e link do YouTube"
private const val DELETE_LABEL = "Excluir música"
private const val DELETE_HINT = "Apaga junto as cifras e a letra"
/** `dd/MM` of a `dd/MM/yyyy` date. */
private const val DAY_MONTH_LENGTH = 5

private const val ARTIST_ALPHA = 0.85f
private const val OFF_ALPHA = 0.45f
private const val DIVIDER_ALPHA = 0.08f
private const val SHADOW_AMBIENT_ALPHA = 0.08f
private const val SHADOW_SPOT_ALPHA = 0.12f

private val BandCorner = 28.dp
private val StatsOverlap = 28.dp
private val CardElevation = 6.dp
private val SidePadding = 16.dp

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
    var isPanelOpen by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isReady = !state.isLoading && state.error == null && !state.isDeleting

    BaseScreen(
        tabName       = state.songName.ifEmpty { TITLE },
        logoRes       = R.drawable.ic_sarca_ipb,
        showBackArrow = true,
        onBackClick   = onBackClick,
        extraActions  = {
            when {
                !state.canEdit || !isReady -> Unit
                state.isEditing -> EditBarActions(
                    isSaving = state.edit?.isSaving == true,
                    onCancel = onCancelEdit,
                    onSave   = onSaveEdit,
                )
                else -> PanelMoreButton(hasActions = true, onClick = { isPanelOpen = true })
            }
        },
    ) { innerPadding ->
        val edit = state.edit
        Box(modifier = Modifier.fillMaxSize()) {
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
                else -> SongDetailBody(
                    state             = state,
                    onChordChartClick = onChordChartClick,
                    onLyricsClick     = { state.lyricsId?.let(onLyricsClick) },
                    onYoutubeClick    = {
                        state.youtubeLink?.let { link ->
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                        }
                    },
                    modifier          = Modifier.padding(innerPadding),
                )
            }
            Box(Modifier.padding(innerPadding)) {
                TopActionPanel(
                    visible   = isPanelOpen && state.canEdit && isReady && !state.isEditing,
                    actions   = listOfNotNull(
                        PanelAction(Icons.Filled.Edit, EDIT_LABEL, onEnterEdit, EDIT_HINT),
                        PanelAction(
                            icon        = Icons.Filled.Delete,
                            label       = DELETE_LABEL,
                            onClick     = { showDeleteConfirm = true },
                            description = DELETE_HINT,
                            isDanger    = true,
                        ).takeIf { state.canDelete },
                    ),
                    onDismiss = { isPanelOpen = false },
                    header    = PanelHeader(name = state.songName, detail = state.artist.takeIf { it.isNotBlank() }),
                )
            }
        }
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

/** Band with the artist, the three numbers over it, the "Abrir" list and the last Sundays (§3.2). */
@Composable
private fun SongDetailBody(
    state: SongDetailUiState,
    onChordChartClick: (id: Int) -> Unit,
    onLyricsClick: () -> Unit,
    onYoutubeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // Artist, numbers and dates can be long-pressed to copy.
        SelectionContainer {
            Column {
                ArtistBand(state.artist)
                SongStats(state, Modifier.offset(y = -StatsOverlap))
            }
        }
        SectionTitle(OPEN_SECTION, Modifier.offset(y = -StatsOverlap))
        OpenList(
            state             = state,
            onChordChartClick = onChordChartClick,
            onLyricsClick     = onLyricsClick,
            onYoutubeClick    = onYoutubeClick,
            modifier          = Modifier.offset(y = -StatsOverlap),
        )
        if (state.lastSundays.isNotEmpty()) {
            SectionTitle(SUNDAYS_SECTION, Modifier.offset(y = -StatsOverlap))
            SelectionContainer(Modifier.offset(y = -StatsOverlap)) { SundayDates(state.lastSundays) }
        }
    }
}

@Composable
private fun ArtistBand(artist: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = BandCorner, bottomEnd = BandCorner))
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = StatsOverlap + 16.dp),
    ) {
        Text(
            text     = artist,
            style    = MaterialTheme.typography.bodyMedium,
            color    = Color.White.copy(alpha = ARTIST_ALPHA),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SongStats(state: SongDetailUiState, modifier: Modifier = Modifier) {
    Row(
        modifier              = modifier
            .fillMaxWidth()
            .padding(horizontal = SidePadding),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatCard(
            value    = state.playCount.toString(),
            label    = if (state.playCount == 1) "vez aos domingos" else "vezes aos domingos",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value    = state.tones.joinToString(" · ").ifEmpty { NO_VALUE },
            label    = if (state.tones.size == 1) "tom tocado" else "tons tocados",
            modifier = Modifier.weight(1f),
        )
        StatCard(
            value    = state.lastSundays.firstOrNull()?.take(DAY_MONTH_LENGTH) ?: NO_VALUE,
            label    = "última vez",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .whiteCard(RoundedCornerShape(16.dp))
            .padding(12.dp),
    ) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color      = MaterialTheme.colorScheme.primary,
            maxLines   = 1,
            overflow   = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionTitle(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier          = modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.primary),
        )
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
    }
}

/** One line per chord chart, then the lyrics and YouTube; what doesn't exist is dimmed. */
@Composable
private fun OpenList(
    state: SongDetailUiState,
    onChordChartClick: (id: Int) -> Unit,
    onLyricsClick: () -> Unit,
    onYoutubeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasYoutube = !state.youtubeLink.isNullOrBlank()
    Column(
        modifier = modifier
            .padding(horizontal = SidePadding)
            .fillMaxWidth()
            .whiteCard(RoundedCornerShape(18.dp)),
    ) {
        if (state.chordCharts.isEmpty()) {
            OpenRow(icon = RowIcon.Drawable(R.drawable.ic_chord_chart), title = CHORD_LABEL, detail = NOT_REGISTERED)
        }
        state.chordCharts.forEach { chart ->
            OpenRow(
                icon    = RowIcon.Drawable(R.drawable.ic_chord_chart),
                title   = CHORD_LABEL,
                detail  = chart.instrument,
                tone    = chart.tone.takeIf { it.isNotBlank() },
                onClick = { onChordChartClick(chart.id) },
            )
            RowDivider()
        }
        if (state.chordCharts.isEmpty()) RowDivider()
        OpenRow(
            icon    = RowIcon.Drawable(R.drawable.ic_lyrics),
            title   = LYRICS_LABEL,
            detail  = if (state.hasLyrics) "Para cantar" else NOT_REGISTERED,
            onClick = onLyricsClick.takeIf { state.hasLyrics },
        )
        RowDivider()
        OpenRow(
            icon    = RowIcon.Vector(Icons.Filled.PlayArrow),
            title   = YOUTUBE_LABEL,
            detail  = if (hasYoutube) "Abre no app do YouTube" else "Sem link",
            onClick = onYoutubeClick.takeIf { hasYoutube },
        )
    }
}

private sealed interface RowIcon {
    data class Drawable(@DrawableRes val res: Int) : RowIcon
    data class Vector(val image: ImageVector) : RowIcon
}

/** A line of "Abrir"; [onClick] null = dimmed, without "›". */
@Composable
private fun OpenRow(
    icon: RowIcon,
    title: String,
    detail: String,
    tone: String? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        modifier          = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier.alpha(OFF_ALPHA))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier         = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (icon is RowIcon.Vector) YouTubeRed else MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            when (icon) {
                is RowIcon.Drawable -> Image(painterResource(icon.res), null, Modifier.size(22.dp))
                is RowIcon.Vector -> Icon(icon.image, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (tone != null) {
            Text(
                text       = "Tom $tone",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier   = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = DIVIDER_ALPHA))
}

/** The last Sundays as tags; the most recent in green. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SundayDates(dates: List<String>) {
    FlowRow(
        modifier              = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
    ) {
        dates.forEachIndexed { index, date ->
            val isLast = index == 0
            Text(
                text     = date,
                style    = MaterialTheme.typography.labelLarge,
                color    = if (isLast) Color.White else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(if (isLast) ipbGreen else MaterialTheme.colorScheme.secondaryContainer)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun Modifier.whiteCard(shape: RoundedCornerShape): Modifier = this
    .shadow(
        elevation    = CardElevation,
        shape        = shape,
        ambientColor = Color.Black.copy(alpha = SHADOW_AMBIENT_ALPHA),
        spotColor    = Color.Black.copy(alpha = SHADOW_SPOT_ALPHA),
    )
    .clip(shape)
    .background(highlightCardColor())

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
