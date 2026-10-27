package com.ipb.castelobranco.features.worshiphub.tables.presentation.tabs

import com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel.RepertoireSaveState
import com.ipb.castelobranco.core.domain.util.normalize
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.core.domain.constants.MUSIC_TONES
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.PositionLabel
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.RowDivider
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.SongTitleArtist
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.TableCard
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.TableRow
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.ToneChip
import com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel.RepertoireRowState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = BrandColors.Green
private const val FIXED_ROW_ALPHA = 0.12f
private const val SELECTED_OPTION_ALPHA = 0.08f
private const val DISABLED_ALPHA = 0.6f
private const val EMPTY_SONG_LABEL = "Escolher música"
private const val HINT = "Toque na música para trocar e no tom para mudar."

@Composable
fun RepertoireTab(
    rows: List<RepertoireRowState>,
    availableSongs: List<Song>,
    isRefreshing: Boolean,
    onSongSelect: (position: Int, song: Song?) -> Unit,
    onToneChange: (position: Int, tone: String) -> Unit,
    onToggleFixed: (position: Int) -> Unit,
    onGenerateClick: () -> Unit,
    onClearClick: () -> Unit,
    save: RepertoireSaveState = RepertoireSaveState(),
    onSaveClick: () -> Unit = {},
    onSongInfoClick: (songId: Int) -> Unit = {},
) {
    val context = LocalContext.current

    fun buildShareText(): String {
        val date = SimpleDateFormat("dd/MM/yy", Locale.forLanguageTag("pt-BR")).format(Date())
        val header = "*Louvor — $date*"

        val lines = rows
            .filter { it.selectedSong != null }
            .joinToString(separator = "\n\n") { row ->
                val song = row.selectedSong!!
                val titlePart = if (row.tone.isBlank()) {
                    "*${song.title}*"
                } else {
                    "*${song.title} (${row.tone})*"
                }
                "$titlePart\n_- ${song.artist}_"
            }

        return if (lines.isBlank()) header else "$header\n\n$lines"
    }

    fun share() {
        val text = buildShareText()
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(
            Intent.createChooser(intent, "Compartilhar").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    // Only one row's song picker is open at a time.
    var openPosition by remember { mutableStateOf<Int?>(null) }
    val hasAnySelection = rows.any { it.selectedSong != null }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            RepertoireHeader(
                actionsEnabled = !isRefreshing && hasAnySelection,
                onClearClick = onClearClick,
                onShareClick = { share() },
            )

            TableCard {
                rows.forEachIndexed { index, row ->
                    if (index > 0) RowDivider()
                    RepertoireRow(
                        row = row,
                        enabled = !isRefreshing,
                        onSongClick = {
                            openPosition = if (openPosition == row.position) null else row.position
                        },
                        onToneChange = { tone -> onToneChange(row.position, tone) },
                        onToggleFixed = { onToggleFixed(row.position) },
                    )
                    AnimatedVisibility(
                        visible = openPosition == row.position,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        SongPicker(
                            selectedSong = row.selectedSong,
                            availableSongs = availableSongs,
                            onSongSelect = { song ->
                                onSongSelect(row.position, song)
                                openPosition = null
                            },
                            onSongInfoClick = onSongInfoClick,
                        )
                    }
                }
            }

            Text(
                text = HINT,
                color = MaterialTheme.colorScheme.outline,
                fontSize = 11.5.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                textAlign = TextAlign.Center,
            )
        }

        RepertoireActionBar(
            isRefreshing = isRefreshing,
            save = save,
            onGenerateClick = onGenerateClick,
            onSaveClick = onSaveClick,
        )
    }
}

@Composable
private fun RepertoireHeader(
    actionsEnabled: Boolean,
    onClearClick: () -> Unit,
    onShareClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 10.dp, top = 6.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Repertório",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        val iconColors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
        IconButton(onClick = onClearClick, enabled = actionsEnabled, colors = iconColors) {
            Icon(Icons.Outlined.DeleteSweep, contentDescription = "Limpar repertório", modifier = Modifier.size(22.dp))
        }
        IconButton(onClick = onShareClick, enabled = actionsEnabled, colors = iconColors) {
            Icon(Icons.Filled.Share, contentDescription = "Compartilhar", modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun RepertoireRow(
    row: RepertoireRowState,
    enabled: Boolean,
    onSongClick: () -> Unit,
    onToneChange: (String) -> Unit,
    onToggleFixed: () -> Unit,
) {
    val song = row.selectedSong
    val hasSong = song != null
    var toneMenuOpen by remember { mutableStateOf(false) }

    TableRow(background = if (row.isFixed) Green.copy(alpha = FIXED_ROW_ALPHA) else Color.Transparent) {
        PositionLabel(position = row.position)
        SongTitleArtist(
            title = song?.title.orEmpty(),
            artist = song?.artist,
            placeholder = EMPTY_SONG_LABEL,
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = enabled, onClick = onSongClick)
                .padding(vertical = 2.dp),
        )
        Box {
            ToneChip(
                tone = row.tone,
                modifier = Modifier.clickable(enabled = enabled && hasSong) { toneMenuOpen = true },
            )
            DropdownMenu(expanded = toneMenuOpen, onDismissRequest = { toneMenuOpen = false }) {
                MUSIC_TONES.forEach { tone ->
                    DropdownMenuItem(
                        text = { Text(tone) },
                        onClick = {
                            onToneChange(tone)
                            toneMenuOpen = false
                        },
                    )
                }
            }
        }
        IconButton(
            onClick = onToggleFixed,
            enabled = enabled && hasSong,
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.PushPin,
                contentDescription = if (row.isFixed) "Desafixar" else "Fixar",
                tint = when {
                    row.isFixed -> Green
                    hasSong -> MaterialTheme.colorScheme.outlineVariant
                    else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun SongPicker(
    selectedSong: Song?,
    availableSongs: List<Song>,
    onSongSelect: (Song) -> Unit,
    onSongInfoClick: (songId: Int) -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    LaunchedEffect(selectedSong) { searchQuery = "" }

    val filtered = remember(searchQuery, availableSongs) {
        val q = searchQuery.trim()
        if (q.isBlank()) availableSongs
        else {
            val nq = q.normalize()
            availableSongs.filter {
                it.title.normalize().contains(nq, ignoreCase = true) ||
                    it.artist.normalize().contains(nq, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceDim)
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(Green),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Buscar música...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                            inner()
                        }
                    },
                )
            }
            if (selectedSong != null) {
                IconButton(onClick = { onSongInfoClick(selectedSong.id) }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Detalhes da música",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            Text(
                text = "Nenhuma música encontrada",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 42.dp, vertical = 8.dp),
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 240.dp)) {
                items(filtered, key = { it.id }) { song ->
                    val isSelected = song.id == selectedSong?.id
                    val optionBackground =
                        if (isSelected) Green.copy(alpha = SELECTED_OPTION_ALPHA) else Color.Transparent
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(optionBackground)
                            .clickable { onSongSelect(song) }
                            .padding(start = 42.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
                    ) {
                        Text(
                            text = song.title,
                            fontSize = 13.5.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) Green else MaterialTheme.colorScheme.onSurface,
                        )
                        if (song.artist.isNotBlank()) {
                            Text(
                                text = song.artist,
                                fontSize = 11.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RepertoireActionBar(
    isRefreshing: Boolean,
    save: RepertoireSaveState,
    onGenerateClick: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        RowDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val tonal = MaterialTheme.colorScheme.secondaryContainer
            val onTonal = MaterialTheme.colorScheme.onSecondaryContainer
            Button(
                onClick = onGenerateClick,
                enabled = !isRefreshing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = tonal,
                    contentColor = onTonal,
                    disabledContainerColor = tonal.copy(alpha = DISABLED_ALPHA),
                    disabledContentColor = onTonal.copy(alpha = DISABLED_ALPHA),
                ),
                modifier = Modifier.weight(1f),
            ) {
                if (isRefreshing) ButtonSpinner(color = onTonal) else Text(text = "Gerar")
            }

            if (save.canSave) {
                Button(
                    onClick = onSaveClick,
                    enabled = !isRefreshing && save.isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Green,
                        contentColor = Color.White,
                        disabledContainerColor = Green.copy(alpha = DISABLED_ALPHA),
                        disabledContentColor = Color.White.copy(alpha = DISABLED_ALPHA),
                    ),
                    modifier = Modifier.weight(1f),
                ) {
                    if (save.isSaving) ButtonSpinner(color = Color.White) else Text(text = "Salvar")
                }
            }
        }
    }
}

@Composable
private fun ButtonSpinner(color: Color) {
    CircularProgressIndicator(color = color, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
}
