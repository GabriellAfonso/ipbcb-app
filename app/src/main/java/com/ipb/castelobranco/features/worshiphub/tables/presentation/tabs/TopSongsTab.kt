package com.ipb.castelobranco.features.worshiphub.tables.presentation.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopSong
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.PlayCount
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.PositionLabel
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.RowDivider
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.SongTitleArtist
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.TableRow
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.cardItem

@Composable
fun TopSongsTab(topSongs: List<TopSong>, onSongClick: (songId: Int) -> Unit = {}) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(top = 10.dp, bottom = ListBottomPadding),
    ) {
        itemsIndexed(topSongs) { index, song ->
            Column(modifier = Modifier.cardItem(index, topSongs.lastIndex)) {
                if (index > 0) RowDivider()
                TopSongsRow(index = index, song = song, onSongClick = onSongClick)
            }
        }
    }
}

@Composable
fun TopSongsRow(
    index: Int,
    song: TopSong,
    onSongClick: (songId: Int) -> Unit = {},
) {
    TableRow(onClick = { onSongClick(song.songId) }) {
        PositionLabel(position = index + 1, highlightTop = true)
        SongTitleArtist(title = song.title, artist = null, modifier = Modifier.weight(1f))
        PlayCount(count = song.playCount)
    }
}
