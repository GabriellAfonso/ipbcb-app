package com.ipb.castelobranco.features.admin.events.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.events.presentation.state.EventAdminItemUi

private const val PAST_ALPHA = 0.55f
private const val THUMB_ASPECT = 3f / 2f
private const val MEMBERS_ONLY_LABEL = "só membros"

/**
 * A row inside a month's card: the start date in a box, title, "when · por … · só membros" and the 3:2 thumbnail
 * on the right. Past rows fade and their date box turns gray.
 */
@Composable
fun EventAdminRow(
    item: EventAdminItemUi,
    imageLoader: ImageLoader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .alpha(if (item.isPast) PAST_ALPHA else 1f),
    ) {
        DateBox(month = item.monthShort, day = item.dayLabel, isPast = item.isPast)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(
                    item.whenLabel,
                    item.authorLabel,
                    MEMBERS_ONLY_LABEL.takeIf { item.isMembersOnly },
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        AsyncImage(
            model = item.imageUrl,
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(64.dp)
                .aspectRatio(THUMB_ASPECT)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
    }
}

@Composable
private fun DateBox(month: String, day: String, isPast: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .size(width = 44.dp, height = 48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPast) MaterialTheme.colorScheme.outlineVariant else ipbGreen),
    ) {
        Text(text = month, color = Color.White, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold)
        Text(text = day, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, lineHeight = 20.sp)
    }
}

@Preview(showBackground = true)
@Composable
private fun EventAdminRowPreview() {
    EventAdminRow(
        item = EventAdminItemUi(
            id = 1,
            title = "Assembleia de membros",
            dateLabel = "29 nov · 10:00",
            imageUrl = "",
            isMembersOnly = true,
            isPast = false,
            authorLabel = "por Pr. João",
            monthLabel = "Novembro de 2026",
            monthShort = "NOV",
            dayLabel = "29",
            whenLabel = "10:00",
        ),
        imageLoader = ImageLoader(LocalContext.current),
        onClick = {},
    )
}
