package com.ipb.castelobranco.core.presentation.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

// The actions panel that drops from the top bar: the gallery's albums (`specs/gallery/spec.md` §1.5) and the
// worship hub's chord charts and lyrics (`specs/worshiphub/spec.md` §4.3).

private const val MORE_LABEL = "Mais opções"
private const val MANAGE_GROUP = "Gerenciar"
private const val DANGER_GROUP = "Cuidado"
private const val SCRIM_ALPHA = 0.4f

private val PanelCorner = 28.dp
private val HandleWidth = 32.dp
private val HandleHeight = 4.dp
private val PanelIconCircle = 40.dp
private val HeaderCover = 44.dp

/** One option of the actions panel; [description] is the line under the name. */
data class PanelAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
    val description: String? = null,
    val isDanger: Boolean = false,
)

/**
 * What the panel acts on: a name and a detail line ("date · count"), with a square [cover] when [showCover] (black
 * without a file).
 */
data class PanelHeader(
    val name: String,
    val detail: String?,
    val cover: File? = null,
    val showCover: Boolean = false,
)

/** The three dots that open the actions panel; nothing when there is no action. */
@Composable
fun PanelMoreButton(hasActions: Boolean, onClick: () -> Unit) {
    if (!hasActions) return
    IconButton(onClick = onClick) { Icon(Icons.Filled.MoreVert, contentDescription = MORE_LABEL) }
}

/**
 * The actions panel: comes down right under the top bar, over a scrim that darkens the rest of the screen; a tap
 * outside or back closes it. With a [header] the options split into "Gerenciar" and "Cuidado".
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
        if (header.showCover) Box(
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
