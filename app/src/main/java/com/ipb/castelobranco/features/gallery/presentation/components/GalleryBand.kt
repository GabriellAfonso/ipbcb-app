package com.ipb.castelobranco.features.gallery.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.theme.ipbGreen

// The gallery's green band (`specs/gallery/spec.md` §1.1, §1.2).

private const val MUTED_ALPHA = 0.75f
private const val TEXT_ALPHA = 0.85f
private const val CARD_ALPHA = 0.10f
private const val BORDER_ALPHA = 0.14f
private const val CHIP_ALPHA = 0.14f
private const val TRACK_ALPHA = 0.20f

private val BandCorner = 28.dp

/** How far the root's album grid climbs over the band. */
val BandOverlap = 28.dp

/** The band's light green progress, the same as the home's band detail text. */
private val ProgressGreen = Color(0xFF8ED1BA)

/** Green band under the top bar, rounded at the bottom; [bottomPadding] grows when a grid climbs over it. */
@Composable
fun GalleryBand(
    modifier: Modifier = Modifier,
    bottomPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = BandCorner, bottomEnd = BandCorner))
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = bottomPadding),
        content = content,
    )
}

@Composable
fun BandLabel(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = MUTED_ALPHA))
}

@Composable
fun BandTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = Color.White,
    )
}

@Composable
fun BandText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = TEXT_ALPHA),
        modifier = modifier,
    )
}

/** Small translucent pills above the album's name: date and count. */
@Composable
fun BandChips(chips: List<String>) {
    if (chips.isEmpty()) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 6.dp)) {
        chips.forEach { chip ->
            Text(
                text = chip,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.White.copy(alpha = CHIP_ALPHA))
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            )
        }
    }
}

/** A notice inside the band, in a translucent card; [actionLabel] adds a white text button. */
@Composable
fun BandNotice(text: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    BandCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                modifier = Modifier.weight(1f),
            )
            if (actionLabel != null) {
                TextButton(onClick = onAction) {
                    Text(actionLabel, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

/** "Baixando fotos: X de N" with its bar, inside the band. */
@Composable
fun BandDownload(text: String, progress: Float?) {
    BandCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = Color.White)
            Text(text, style = MaterialTheme.typography.bodySmall, color = Color.White)
        }
        if (progress != null) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                color = ProgressGreen,
                trackColor = Color.White.copy(alpha = TRACK_ALPHA),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BandCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .padding(top = 10.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(Color.White.copy(alpha = CARD_ALPHA))
            .border(BorderStroke(1.dp, Color.White.copy(alpha = BORDER_ALPHA)), shape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        content = content,
    )
}

/**
 * Lays the element out [overlap] shorter than it draws, so what comes next in a list climbs over its bottom: the
 * root's album grid over the band.
 */
fun Modifier.overlappedBelow(overlap: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val height = (placeable.height - overlap.roundToPx()).coerceAtLeast(0)
    layout(placeable.width, height) { placeable.place(0, 0) }
}

/** Draws the element [padding] wider on each side than its list gives it: a band reaching the screen's edges. */
fun Modifier.fullBleed(padding: Dp): Modifier = layout { measurable, constraints ->
    val extra = padding.roundToPx() * 2
    val placeable = measurable.measure(
        constraints.copy(minWidth = constraints.maxWidth + extra, maxWidth = constraints.maxWidth + extra),
    )
    layout(constraints.maxWidth, placeable.height) { placeable.place(-padding.roundToPx(), 0) }
}
