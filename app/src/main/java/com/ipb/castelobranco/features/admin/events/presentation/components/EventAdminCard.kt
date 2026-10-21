package com.ipb.castelobranco.features.admin.events.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.ipb.castelobranco.features.admin.events.presentation.state.EventAdminItemUi

private const val PAST_ALPHA = 0.55f
private const val THUMB_ASPECT = 16f / 9f
private const val MEMBERS_ONLY_LABEL = "Só membros"

/** A row of the management list: 16:9 thumbnail, title, date and the members-only mark. Past rows fade. */
@Composable
fun EventAdminCard(
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
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(10.dp)
            .alpha(if (item.isPast) PAST_ALPHA else 1f),
    ) {
        AsyncImage(
            model = item.imageUrl,
            imageLoader = imageLoader,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(112.dp)
                .aspectRatio(THUMB_ASPECT)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = item.dateLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            item.authorLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.isMembersOnly) {
                AssistChip(onClick = onClick, label = { Text(MEMBERS_ONLY_LABEL) })
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun EventAdminCardPreview() {
    EventAdminCard(
        item = EventAdminItemUi(
            id = 1,
            title = "Assembleia de membros",
            dateLabel = "30 nov · 10:00",
            imageUrl = "",
            isMembersOnly = true,
            isPast = false,
            authorLabel = "por Ana Paula",
        ),
        imageLoader = ImageLoader(LocalContext.current),
        onClick = {},
    )
}
