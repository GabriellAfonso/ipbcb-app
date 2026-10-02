package com.ipb.castelobranco.features.worshiphub.shared.presentation.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.features.worshiphub.shared.domain.SundaySection
import java.time.format.DateTimeFormatter

/** The "Repertório de domingo" block shown above a song content list: a title and its rows. */
data class SundaySectionUi(
    val title: String,
    val rows: List<SongContentRow>,
)

private val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")

/** Each row opens the entry's lyrics or chord chart; the key goes as a chip. */
fun SundaySection.toUi(toneColor: Color = BrandColors.Orange): SundaySectionUi = SundaySectionUi(
    title = "Repertório de domingo ${date.format(DAY_MONTH)}",
    rows = entries.map { entry ->
        SongContentRow(
            id = entry.contentId,
            songId = entry.songId,
            songName = entry.title,
            chips = listOfNotNull(
                entry.tone.takeIf { it.isNotBlank() }?.let { SongContentChip("Tom $it", toneColor) },
            ),
        )
    },
)

@Composable
fun SundaySectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Filled.Event,
            contentDescription = null,
            tint = BrandColors.Orange,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Title of the list part below the Sunday section, so the two do not read as one list. */
@Composable
fun OtherSongsHeader(modifier: Modifier = Modifier) {
    Text(
        text = "Todas",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 6.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun SundaySectionHeaderPreview() {
    SundaySectionHeader(title = "Repertório de domingo 04/10")
}
