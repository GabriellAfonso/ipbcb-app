package com.ipb.castelobranco.features.admin.register.presentation.components

import com.ipb.castelobranco.core.domain.util.normalize
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.domain.constants.MUSIC_TONES
import com.ipb.castelobranco.features.admin.register.presentation.state.SundaySongRowState
import com.ipb.castelobranco.features.admin.register.presentation.util.SongLabelFormatter
import com.ipb.castelobranco.core.domain.model.Song
import com.ipb.castelobranco.core.presentation.theme.BrandColors

private val Green = BrandColors.Green

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SundaySongRow(
    availableSongs: List<Song>,
    state: SundaySongRowState,
    onSongSelect: (Song) -> Unit,
    onToneChange: (String) -> Unit,
    onRemoveClick: (() -> Unit)?
) {
    var expandedSong by remember { mutableStateOf(false) }
    var expandedTone by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery, availableSongs) {
        val q = searchQuery.trim()
        if (q.isBlank()) availableSongs
        else availableSongs.filter {
            it.title.normalize().contains(q.normalize(), ignoreCase = true) ||
                it.artist.normalize().contains(q.normalize(), ignoreCase = true)
        }
    }

    val selectedSong = remember(state.selectedSongId, availableSongs) {
        availableSongs.find { it.id == state.selectedSongId }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color.Transparent,
        unfocusedBorderColor = Color.Transparent,
        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    )

    LaunchedEffect(state.selectedSongId) {
        if (state.selectedSongId != null) {
            expandedSong = false
            searchQuery = ""
        }
    }

    val songTriggerShape = if (expandedSong)
        RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
    else
        RoundedCornerShape(8.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        Text(
            text = "${state.position}.",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .width(18.dp)
                .padding(top = 14.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Top
        ) {
            // ── Song trigger ─────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(songTriggerShape)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .clickable { expandedSong = !expandedSong }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val label = selectedSong?.let { SongLabelFormatter.format(it) }
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
                Icon(
                    imageVector = if (expandedSong) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = null,
                    tint = if (expandedSong) Green else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.size(18.dp)
                )
            }

            // ── Song dropdown ─────────────────────────────────────────────────
            AnimatedVisibility(
                visible = expandedSong,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
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
                        filtered.forEach { song ->
                            val isSelected = song.id == state.selectedSongId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) Green.copy(alpha = 0.08f) else Color.Transparent)
                                    .clickable { onSongSelect(song) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = SongLabelFormatter.format(song),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) Green else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        ExposedDropdownMenuBox(
            expanded = expandedTone,
            onExpandedChange = { expandedTone = !expandedTone },
            modifier = Modifier.width(130.dp)
        ) {
            OutlinedTextField(
                value = state.tone,
                onValueChange = { },
                readOnly = true,
                textStyle = MaterialTheme.typography.bodySmall,
                placeholder = { Text("Tom", style = MaterialTheme.typography.bodySmall) },
                singleLine = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedTone) },
                colors = fieldColors,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(48.dp)
            )

            ExposedDropdownMenu(
                expanded = expandedTone,
                onDismissRequest = { expandedTone = false }
            ) {
                MUSIC_TONES.forEach { tone ->
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

        if (onRemoveClick != null) {
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = onRemoveClick,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remover",
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
