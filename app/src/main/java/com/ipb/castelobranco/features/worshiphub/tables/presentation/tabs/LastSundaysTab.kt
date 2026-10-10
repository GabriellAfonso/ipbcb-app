package com.ipb.castelobranco.features.worshiphub.tables.presentation.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySet
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.Header

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow

import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySetItem
import androidx.compose.foundation.clickable
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.ColumnAlignment
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.TableColumn

private val columns = listOf(
    TableColumn("#", 0.3f, ColumnAlignment.Center),
    TableColumn("Nome", 3.5f),
    TableColumn("Tom", 0.7f, ColumnAlignment.Start),
    TableColumn("Artista", 1f)
)

@Composable
fun LastSundaysTab(
    sundays: List<SundaySet>,
    onSongClick: (songId: Int) -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Header(columns)

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(sundays, key = { it.date }) { sunday ->
                SundaySection(sunday = sunday, onSongClick = onSongClick)
            }
        }
    }
}



@Composable
fun SundaySection(sunday: SundaySet, onSongClick: (songId: Int) -> Unit = {}) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = sunday.date,
                modifier = Modifier.padding(vertical = 8.dp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        sunday.songs.forEachIndexed { index, song ->
            val isLast = index == sunday.songs.lastIndex
            SundaySongRow(
                song = song,
                onSongClick = onSongClick,
                modifier = Modifier.padding(bottom = if (isLast) 15.dp else 0.dp)
            )
        }
    }
}

@Composable
fun SundaySongRow(
    song: SundaySetItem,
    onSongClick: (songId: Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {

        Box(Modifier.weight(columns[0].weight), contentAlignment = Alignment.Center) {
            Text(song.position.toString(), color = textColor)
        }

        Box(
            Modifier
                .weight(columns[1].weight)
                .padding(end = 30.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                song.title,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onSongClick(song.songId) }
            )
        }

        Box(Modifier.weight(columns[2].weight), contentAlignment = Alignment.CenterStart) {
            Text(song.tone, color = textColor)
        }

        Box(Modifier.weight(columns[3].weight), contentAlignment = Alignment.CenterStart) {
            Text(
                song.artist,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
