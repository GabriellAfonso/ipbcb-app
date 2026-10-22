package com.ipb.castelobranco.features.worshiphub.tables.presentation.tabs
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import com.ipb.castelobranco.features.worshiphub.tables.domain.model.TopSong
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.ColumnAlignment
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.Header
import com.ipb.castelobranco.features.worshiphub.tables.presentation.components.TableColumn

private val columns = listOf(
    TableColumn("#", 0.3f, ColumnAlignment.Center),
    TableColumn("Nome", 3f,ColumnAlignment.Start),
    TableColumn("Vezes", 0.5f,ColumnAlignment.Center),
)
@Composable
fun TopSongsTab(topSongs: List<TopSong>, onSongClick: (songId: Int) -> Unit = {}) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Header(columns)
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(0.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            itemsIndexed(topSongs) { index, song ->
                TopSongsRow(
                    index = index,
                    song = song,
                    onSongClick = onSongClick,
                )

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
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = Modifier
        .fillMaxWidth()
        .background(color = MaterialTheme.colorScheme.surfaceContainer)
        .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Box(Modifier.weight(columns[0].weight), contentAlignment = Alignment.Center) {
            Text(
                text = "${index + 1}",
                color = textColor,
            )
        }
        Box(Modifier.weight(columns[1].weight)) {
            Text(
                text = song.title,
                color = textColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onSongClick(song.songId) }
            )
        }
        Box(Modifier.weight(columns[2].weight), contentAlignment = Alignment.Center) {
            Text(
                text = song.playCount.toString(),
                color = textColor,
            )
        }
    }
}