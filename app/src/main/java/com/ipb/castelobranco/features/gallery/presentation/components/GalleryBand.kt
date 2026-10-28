package com.ipb.castelobranco.features.gallery.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ipb.castelobranco.core.presentation.components.highlightCardColor
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import java.io.File

// The gallery's green band and its actions panel (`specs/gallery/spec.md` §1.1, §1.2, §1.5).

private const val MORE_LABEL = "Mais opções"
private const val MANAGE_GROUP = "Gerenciar"
private const val DANGER_GROUP = "Cuidado"
private const val MUTED_ALPHA = 0.75f
private const val TEXT_ALPHA = 0.85f
private const val CARD_ALPHA = 0.10f
private const val BORDER_ALPHA = 0.14f
private const val CHIP_ALPHA = 0.14f
private const val TRACK_ALPHA = 0.20f
private const val SCRIM_ALPHA = 0.4f

private val BandCorner = 28.dp
private val PanelCorner = 28.dp
private val HandleWidth = 32.dp
private val HandleHeight = 4.dp
private val PanelIconCircle = 40.dp
private val HeaderCover = 44.dp

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

/** One option of the actions panel; [description] is the line under the name. */
data class PanelAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val description: String? = null,
    val isDanger: Boolean = false,
)

/** The album the panel acts on: cover, name and "date · count". */
data class PanelHeader(val cover: File?, val name: String, val detail: String?)

/** The three dots that open the actions panel; nothing when there is no action. */
@Composable
fun PanelMoreButton(hasActions: Boolean, onClick: () -> Unit) {
    if (!hasActions) return
    IconButton(onClick = onClick) { Icon(Icons.Filled.MoreVert, contentDescription = MORE_LABEL) }
}

/**
 * The actions panel: comes down right under the top bar, over a scrim that darkens the rest of the screen; a tap
 * outside or back closes it. With a [header] (an album) the options split into "Gerenciar" and "Cuidado".
 * Place it at the top of the screen's content, under the top bar.
 */
@Composable
fun TopActionPanel(
    visible: Boolean,
    actions: List<PanelAction>,
    onDismiss: () -> Unit,
    header: PanelHeader? = null,
) {
    BackHandler(enabled = visible, onBack = onDismiss)
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = SCRIM_ALPHA))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
        )
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        val shape = RoundedCornerShape(bottomStart = PanelCorner, bottomEnd = PanelCorner)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, shape)
                .clip(shape)
                .background(highlightCardColor())
                .verticalScroll(rememberScrollState())
                .padding(top = 6.dp, bottom = 10.dp),
        ) {
            header?.let { PanelHeaderRow(it) }
            val (danger, normal) = actions.partition { it.isDanger }
            if (header != null && normal.isNotEmpty()) PanelGroup(MANAGE_GROUP)
            normal.forEach { PanelRow(it, onDismiss) }
            if (danger.isNotEmpty()) {
                PanelGroup(DANGER_GROUP)
                danger.forEach { PanelRow(it, onDismiss) }
            }
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
                    .size(width = HandleWidth, height = HandleHeight)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
        }
    }
}

@Composable
private fun PanelHeaderRow(header: PanelHeader) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(HeaderCover)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
        ) {
            if (header.cover != null) {
                AsyncImage(
                    model = header.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = header.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            header.detail?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PanelGroup(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun PanelRow(action: PanelAction, onDismiss: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val circle = if (action.isDanger) colors.errorContainer else colors.secondaryContainer
    val iconTint = if (action.isDanger) colors.onErrorContainer else colors.onSecondaryContainer
    val labelColor = if (action.isDanger) colors.error else colors.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onDismiss()
                action.onClick()
            }
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(PanelIconCircle)
                .clip(CircleShape)
                .background(circle),
            contentAlignment = Alignment.Center,
        ) {
            Icon(action.icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(action.label, style = MaterialTheme.typography.bodyLarge, color = labelColor)
            action.description?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
        }
    }
}
