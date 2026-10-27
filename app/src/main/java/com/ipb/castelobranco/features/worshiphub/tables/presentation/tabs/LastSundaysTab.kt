package com.ipb.castelobranco.features.worshiphub.tables.presentation.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySet
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.SundaySetItem
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.CenteredDateHeader
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.PositionLabel
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.RowDivider
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.SongTitleArtist
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.TableCard
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.TableRow
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.ToneChip

/** Bottom room so the last card clears the search FAB. */
internal val ListBottomPadding = 96.dp

@Composable
fun LastSundaysTab(
    sundays: List<SundaySet>,
    onSongClick: (songId: Int) -> Unit = {},
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = ListBottomPadding),
    ) {
        items(sundays, key = { it.date }) { sunday ->
            SundaySection(sunday = sunday, onSongClick = onSongClick)
        }
    }
}

@Composable
fun SundaySection(sunday: SundaySet, onSongClick: (songId: Int) -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth()) {
        CenteredDateHeader(date = sunday.date)
        TableCard {
            sunday.songs.forEachIndexed { index, song ->
                if (index > 0) RowDivider()
                SundaySongRow(song = song, onSongClick = onSongClick)
            }
        }
    }
}

@Composable
fun SundaySongRow(
    song: SundaySetItem,
    onSongClick: (songId: Int) -> Unit = {},
) {
    TableRow(onClick = { onSongClick(song.songId) }) {
        PositionLabel(position = song.position)
        SongTitleArtist(title = song.title, artist = song.artist, modifier = Modifier.weight(1f))
        ToneChip(tone = song.tone)
    }
}
