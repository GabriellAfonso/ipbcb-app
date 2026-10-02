package com.ipb.castelobranco.features.worshiphub.tables.presentation.tabs

import com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel.RepertoireSaveState
import com.ipb.castelobranco.core.domain.util.normalize
import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.domain.constants.NATURAL_TONES
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel.RepertoireRowState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = BrandColors.Green

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rows.forEach { row ->
            RepertoireRow(
                row = row,
                availableSongs = availableSongs,
                enabled = !isRefreshing,
                onSongSelect = { song -> onSongSelect(row.position, song) },
                onToneChange = { tone -> onToneChange(row.position, tone) },
                onToggleFixed = { onToggleFixed(row.position) },
                onSongInfoClick = onSongInfoClick,
            )
        }

        val hasAnySelection = rows.any { it.selectedSong != null }
        TextButton(
            onClick = onClearClick,
            enabled = !isRefreshing && hasAnySelection,
            modifier = Modifier.align(Alignment.End).padding(horizontal = 4.dp),
        ) {
            Text(text = "Limpar repertório")
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = onGenerateClick,
                enabled = !isRefreshing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                ),
                modifier = Modifier.weight(1f)
            ) {
                if (!isRefreshing) {
                    Text(text = "Gerar")
                } else {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (save.canSave) {
                Button(
                    onClick = onSaveClick,
                    enabled = !isRefreshing && save.isSaveEnabled,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                        disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    if (save.isSaving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text(text = "Salvar")
                    }
                }
            }

            Button(
                onClick = { share() },
                enabled = !isRefreshing && hasAnySelection,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.6f)
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text(text = "Compartilhar")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun RepertoireRow(
    row: RepertoireRowState,
    availableSongs: List<Song>,
    enabled: Boolean,
    onSongSelect: (Song?) -> Unit,
    onToneChange: (String) -> Unit,
    onToggleFixed: () -> Unit,
    onSongInfoClick: (songId: Int) -> Unit = {},
) {
    var expanded by remember { mutableStateOf(false) }
    var expandedTone by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

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

    LaunchedEffect(row.selectedSong) {
        expanded = false
        searchQuery = ""
    }

    val triggerShape = if (expanded)
        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    else
        RoundedCornerShape(8.dp)

    val rowBackground = if (row.isFixed) Green.copy(alpha = 0.85f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBackground)
            .combinedClickable(
                enabled = enabled && row.selectedSong != null,
                onClick = {},
                onLongClick = { onToggleFixed() }
            )
            .padding(vertical = 4.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "${row.position}.",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .width(18.dp)
                .padding(top = 14.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(triggerShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .combinedClickable(
                        enabled = enabled,
                        onClick = { expanded = !expanded },
                        onLongClick = {
                            if (row.selectedSong != null) onToggleFixed()
                        }
                    )
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val label = row.selectedSong?.let { formatSongLabel(it) }
                Text(
                    text = label ?: "Música",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (label != null)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                androidx.compose.animation.AnimatedVisibility(
                    visible = row.selectedSong != null && !expanded,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = "Detalhes da música",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { row.selectedSong?.let { onSongInfoClick(it.id) } }
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (expanded) Green else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(Green),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { inner ->
                                Box {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Buscar música...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                                        )
                                    }
                                    inner()
                                }
                            }
                        )
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    if (filtered.isEmpty()) {
                        Text(
                            text = "Nenhuma música encontrada",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                        ) {
                            items(filtered, key = { it.id }) { song ->
                                val isSelected = song.id == row.selectedSong?.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) Green.copy(alpha = 0.08f) else Color.Transparent)
                                        .clickable { onSongSelect(song) }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatSongLabel(song),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) Green else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        ExposedDropdownMenuBox(
            expanded = expandedTone,
            onExpandedChange = {
                if (enabled && row.selectedSong != null) expandedTone = !expandedTone
            },
            modifier = Modifier.width(72.dp)
        ) {
            OutlinedTextField(
                value = row.tone,
                onValueChange = {},
                readOnly = true,
                singleLine = true,
                placeholder = {
                    Text(
                        text = "Tom",
                        style = MaterialTheme.typography.bodySmall
                    )
                },
                textStyle = MaterialTheme.typography.bodySmall,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
                ),
                enabled = enabled && row.selectedSong != null,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(48.dp)
            )

            ExposedDropdownMenu(
                expanded = expandedTone,
                onDismissRequest = { expandedTone = false }
            ) {
                NATURAL_TONES.forEach { tone ->
                    DropdownMenuItem(
                        text = { Text(tone) },
                        onClick = {
                            onToneChange(tone)
                            expandedTone = false
                        }
                    )
                }
            }
        }
    }
}

private fun formatSongLabel(song: Song): String =
    if (song.artist.isBlank()) song.title else "${song.title} [${song.artist}]"
