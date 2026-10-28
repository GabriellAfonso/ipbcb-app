package com.ipb.castelobranco.features.worshiphub.hub.presentation.screens

import android.view.SoundEffectConstants
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.worshiphub.hub.presentation.navigation.WorshipHubNav
import com.ipb.castelobranco.features.worshiphub.hub.presentation.state.HubBandUi
import com.ipb.castelobranco.features.worshiphub.hub.presentation.state.HubSongUi
import com.ipb.castelobranco.features.worshiphub.hub.presentation.state.WorshipHubUiState
import com.ipb.castelobranco.features.worshiphub.hub.presentation.state.removeSetlistTitle
import java.time.LocalDate
import com.ipb.castelobranco.features.worshiphub.hub.presentation.viewmodel.WorshipHubViewModel

private const val TITLE = "Louvor"
private const val BAND_LABEL = "Repertório de domingo"
private const val NOT_SAVED = "Ainda não foi salvo."
private const val BUILD_SETLIST = "Montar repertório"
private const val CHORD_CHARTS = "Cifras"
private const val LYRICS = "Letras"
private const val ARTIST_SEPARATOR = " · "
private const val REMOVE = "Remover"
private const val PLAIN_LABEL = "Ministério de Louvor"
private const val PLAIN_TITLE = "Músicas, cifras e letras"
private const val REMOVE_BODY = "As músicas planejadas para esse domingo serão apagadas."
private const val CANCEL = "Cancelar"

private const val MUTED_ALPHA = 0.75f
private const val FAINT_ALPHA = 0.65f
private const val CARD_ALPHA = 0.10f
private const val BORDER_ALPHA = 0.14f
private const val TONE_ALPHA = 0.16f
private const val OUTLINE_ALPHA = 0.40f
private const val SHADOW_AMBIENT_ALPHA = 0.08f
private const val SHADOW_SPOT_ALPHA = 0.12f

private val BandCorner = 28.dp
private val TileOverlap = 24.dp
private val TileHeight = 104.dp
private val TileCorner = 18.dp
private val TileGap = 12.dp
private val TileElevation = 6.dp
private val TileCircle = 44.dp
private val TileIcon = 24.dp
private val SidePadding = 16.dp
private val PillPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
private val PillShape = RoundedCornerShape(percent = 50)

@Composable
fun WorshipHubScreen(nav: WorshipHubNav, viewModel: WorshipHubViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }
    WorshipHubContent(
        state = state,
        nav = nav,
        onRemoveSetlist = viewModel::removeSetlist,
        snackbarHostState = snackbarHostState,
    )
}

/** "Domingo em destaque" (`specs/worshiphub/spec.md` §1). */
@Composable
private fun WorshipHubContent(
    state: WorshipHubUiState,
    nav: WorshipHubNav,
    onRemoveSetlist: () -> Unit,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    var confirmingRemoval by rememberSaveable { mutableStateOf(false) }
    val band = state.band

    BaseScreen(
        tabName = TITLE,
        logoRes = R.drawable.ic_worshiphub,
        showBackArrow = true,
        onBackClick = nav.back,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
            ) {
                if (band != null) {
                    SundayBand(
                        band = band,
                        isDeleting = state.isDeleting,
                        onRemoveClick = { confirmingRemoval = true },
                        nav = nav,
                    )
                    HubTiles(nav = nav, modifier = Modifier.offset(y = -TileOverlap))
                } else {
                    PlainBand()
                    HubTiles(nav = nav, modifier = Modifier.offset(y = -TileOverlap))
                }
            }
            SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
        }
    }

    if (confirmingRemoval && band?.canDelete == true) {
        AlertDialog(
            onDismissRequest = { confirmingRemoval = false },
            title = { Text(removeSetlistTitle(band.date)) },
            text = { Text(REMOVE_BODY) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingRemoval = false
                    onRemoveSetlist()
                }) {
                    Text(REMOVE, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmingRemoval = false }) { Text(CANCEL) } },
        )
    }
}

/** The band for whoever is not a worship member: same shape as everywhere in the app, no setlist. */
@Composable
private fun PlainBand() {
    Column(modifier = Modifier.bandModifier()) {
        Text(PLAIN_LABEL, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = MUTED_ALPHA))
        Text(
            text = PLAIN_TITLE,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
    }
}

private fun Modifier.bandModifier(): Modifier = fillMaxWidth()
    .clip(RoundedCornerShape(bottomStart = BandCorner, bottomEnd = BandCorner))
    .background(ipbGreen)
    .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = TileOverlap + 18.dp)

@Composable
private fun SundayBand(band: HubBandUi, isDeleting: Boolean, onRemoveClick: () -> Unit, nav: WorshipHubNav) {
    Column(modifier = Modifier.bandModifier()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = BAND_LABEL,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = MUTED_ALPHA),
                )
                Text(
                    text = band.dateLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
            if (band.canDelete) {
                TextButton(
                    onClick = onRemoveClick,
                    enabled = !isDeleting,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color.White,
                        disabledContentColor = Color.White.copy(alpha = FAINT_ALPHA),
                    ),
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = LocalContentColor.current,
                        )
                    } else {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(REMOVE)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (band.songs.isEmpty()) {
            Text(NOT_SAVED, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
            if (band.canSave) {
                Spacer(modifier = Modifier.height(10.dp))
                WhitePill(label = BUILD_SETLIST, onClick = nav.repertoire)
            }
            return@Column
        }

        val cardShape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(cardShape)
                .background(Color.White.copy(alpha = CARD_ALPHA))
                .border(BorderStroke(1.dp, Color.White.copy(alpha = BORDER_ALPHA)), cardShape)
                .padding(vertical = 6.dp, horizontal = 4.dp),
        ) {
            band.songs.forEach { song ->
                SetlistRow(
                    song = song,
                    onClick = {
                        song.chordChartId?.let(nav.chordChart) ?: nav.songDetail(song.songId)
                    },
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WhitePill(label = CHORD_CHARTS, onClick = nav.button3)
            OutlinedButton(
                onClick = nav.button4,
                shape = PillShape,
                contentPadding = PillPadding,
                border = BorderStroke(1.dp, Color.White.copy(alpha = OUTLINE_ALPHA)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Text(LYRICS, fontWeight = FontWeight.SemiBold)
            }
        }
        band.savedLabel?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = FAINT_ALPHA))
        }
    }
}

@Composable
private fun SetlistRow(song: HubSongUi, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = song.position.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = MUTED_ALPHA),
            modifier = Modifier.width(18.dp),
        )
        Text(
            text = buildAnnotatedString {
                append(song.title)
                withStyle(SpanStyle(color = Color.White.copy(alpha = MUTED_ALPHA))) {
                    append(ARTIST_SEPARATOR + song.artist)
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = song.tone,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color.White.copy(alpha = TONE_ALPHA))
                .widthIn(min = 30.dp)
                .padding(horizontal = 7.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun WhitePill(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = PillShape,
        contentPadding = PillPadding,
        colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = ipbGreen),
    ) {
        Text(label, fontWeight = FontWeight.SemiBold)
    }
}

private data class HubTile(@DrawableRes val iconRes: Int, val label: String, val onClick: () -> Unit)

@Composable
private fun HubTiles(nav: WorshipHubNav, modifier: Modifier = Modifier) {
    val tiles = listOf(
        HubTile(R.drawable.ic_table, "Tabelas", nav.tables),
        HubTile(R.drawable.ic_songs, "Músicas", nav.songs),
        HubTile(R.drawable.ic_chord_chart, CHORD_CHARTS, nav.button3),
        HubTile(R.drawable.ic_lyrics, LYRICS, nav.button4),
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SidePadding),
        verticalArrangement = Arrangement.spacedBy(TileGap),
    ) {
        tiles.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(TileGap)) {
                row.forEach { tile -> HubTileCard(tile = tile, modifier = Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun HubTileCard(tile: HubTile, modifier: Modifier = Modifier) {
    val view = LocalView.current
    val shape = RoundedCornerShape(TileCorner)
    Column(
        modifier = modifier
            .height(TileHeight)
            .shadow(
                elevation = TileElevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = SHADOW_AMBIENT_ALPHA),
                spotColor = Color.Black.copy(alpha = SHADOW_SPOT_ALPHA),
            )
            .clip(shape)
            .background(highlightCardColor())
            .clickable {
                view.playSoundEffect(SoundEffectConstants.CLICK)
                tile.onClick()
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(TileCircle)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = tile.iconRes),
                contentDescription = null,
                modifier = Modifier.size(TileIcon),
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(tile.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

private val PreviewNav = WorshipHubNav(
    tables = {}, repertoire = {}, chordChart = {}, songDetail = {}, songs = {}, button3 = {}, button4 = {},
    button5 = {}, button6 = {}, button7 = {}, button8 = {}, back = {},
)

@Preview(showBackground = true)
@Composable
private fun WorshipHubWithSetlistPreview() {
    IPBCasteloBrancoTheme {
        WorshipHubContent(
            state = WorshipHubUiState(
                band = HubBandUi(
                    date = LocalDate.of(2026, 11, 1),
                    dateLabel = "Domingo, 01/11",
                    songs = listOf(
                        HubSongUi(1, 1, "Porque Ele Vive", "Bill Gaither", "G", 10),
                        HubSongUi(2, 2, "Agnus Dei", "Michael W. Smith", "A", null),
                        HubSongUi(3, 3, "Grandioso És Tu", "Carl Boberg", "Bb", 12),
                    ),
                    savedLabel = "Salvo por Marcos Vinícius · 27/10 às 21:40",
                    canSave = true,
                    canDelete = true,
                ),
            ),
            nav = PreviewNav,
            onRemoveSetlist = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorshipHubEmptyPreview() {
    IPBCasteloBrancoTheme {
        WorshipHubContent(
            state = WorshipHubUiState(
                band = HubBandUi(
                    date = LocalDate.of(2026, 11, 1),
                    dateLabel = "Domingo, 01/11",
                    songs = emptyList(),
                    savedLabel = null,
                    canSave = true,
                ),
            ),
            nav = PreviewNav,
            onRemoveSetlist = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun WorshipHubNotMemberPreview() {
    IPBCasteloBrancoTheme {
        WorshipHubContent(state = WorshipHubUiState(), nav = PreviewNav, onRemoveSetlist = {})
    }
}
