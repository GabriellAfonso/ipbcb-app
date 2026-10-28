package com.ipb.castelobranco.features.worshiphub.shared.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.data.local.SongScrollMode
import com.ipb.castelobranco.core.presentation.theme.ipbGreen

// The chord chart and lyrics screens' chrome, "ajuste fino com faixa" (`specs/worshiphub/spec.md` §4.3, §5.3).

private const val PAGES_LABEL = "Páginas"
private const val SCROLL_LABEL = "Rolagem"
private const val SMALLER_LABEL = "A−"
private const val BIGGER_LABEL = "A+"
private const val SMALLER_DESCRIPTION = "Diminuir texto"
private const val BIGGER_DESCRIPTION = "Aumentar texto"
private const val SAVE_LABEL = "Salvar"
private const val CANCEL_LABEL = "Cancelar"
private const val DARK_SURFACE_LUMINANCE = 0.5f
private const val ARTIST_ALPHA = 0.85f
private const val INACTIVE_ALPHA = 0.8f
private const val OUTLINE_ALPHA = 0.35f
private const val SELECTED_ALPHA = 0.2f

private val BandCorner = 20.dp
private val ChordOnLight = Color(0xFFA85F00)
private val ChordOnDark = Color(0xFFF7B733)

/** Lyrics text size: 22sp to start, 16 to 34 in steps of 2 (`specs/worshiphub/spec.md` §5.3). */
object LyricsTextSize {
    const val DEFAULT = 22
    const val MIN = 16
    const val MAX = 34
    const val STEP = 2
}

/**
 * The chords' color: the brand orange read 2:1 on the light background, so the light theme takes a darker orange and
 * the dark theme a lighter one.
 */
@Composable
fun chordColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE) ChordOnDark else ChordOnLight

/**
 * Thin green band under the top bar: the artist, then [controls] (the tone, or the text size) and the
 * "Páginas | Rolagem" switch.
 */
@Composable
fun SongBand(
    artist: String,
    scrollMode: SongScrollMode,
    onToggleScroll: () -> Unit,
    controls: @Composable RowScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = BandCorner, bottomEnd = BandCorner))
            .background(ipbGreen)
            .padding(start = 16.dp, end = 12.dp, bottom = 10.dp),
    ) {
        if (artist.isNotBlank()) {
            Text(
                text = artist,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = ARTIST_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            controls()
            Spacer(Modifier.weight(1f))
            ModeSwitch(
                isPages = scrollMode == SongScrollMode.HORIZONTAL,
                onPages = { if (scrollMode != SongScrollMode.HORIZONTAL) onToggleScroll() },
                onScroll = { if (scrollMode != SongScrollMode.VERTICAL) onToggleScroll() },
            )
        }
    }
}

/** "Tom D" on the band, white. */
@Composable
fun ToneTag(tone: String) {
    Text(
        text = "Tom $tone",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = ipbGreen,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** "A−" / "A+" on the band; a side is disabled at its limit. */
@Composable
fun TextSizeButtons(canShrink: Boolean, canGrow: Boolean, onShrink: () -> Unit, onGrow: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        SizeButton(SMALLER_LABEL, SMALLER_DESCRIPTION, canShrink, onShrink)
        SizeButton(BIGGER_LABEL, BIGGER_DESCRIPTION, canGrow, onGrow)
    }
}

@Composable
private fun SizeButton(label: String, description: String, enabled: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(width = 34.dp, height = 30.dp)
            .clip(shape)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = OUTLINE_ALPHA)), shape)
            .clickable(enabled = enabled, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = if (enabled) 1f else OUTLINE_ALPHA),
        )
    }
}

@Composable
private fun ModeSwitch(isPages: Boolean, onPages: () -> Unit, onScroll: () -> Unit) {
    val shape = RoundedCornerShape(percent = 50)
    Row(
        modifier = Modifier
            .clip(shape)
            .border(BorderStroke(1.dp, Color.White.copy(alpha = OUTLINE_ALPHA)), shape),
    ) {
        ModeOption(PAGES_LABEL, selected = isPages, onClick = onPages)
        ModeOption(SCROLL_LABEL, selected = !isPages, onClick = onScroll)
    }
}

@Composable
private fun ModeOption(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (selected) Color.White else Color.White.copy(alpha = INACTIVE_ALPHA),
        modifier = Modifier
            .background(if (selected) Color.White.copy(alpha = SELECTED_ALPHA) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}

/** A section's name (Verso, Coro...) as a small uppercase tag. */
@Composable
fun SectionTag(title: String) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

/** The pages mode's footer: a thin progress bar and "2 de 3". */
@Composable
fun PageProgress(currentPage: Int, pageCount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LinearProgressIndicator(
            progress = { if (pageCount > 0) (currentPage + 1f) / pageCount else 0f },
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
        )
        Text(
            text = "${currentPage + 1} de $pageCount",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

/** While editing, the top bar shows "Cancelar" and "Salvar" instead of the three dots. */
@Composable
fun EditBarActions(isSaving: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    TextButton(onClick = onCancel, enabled = !isSaving) { Text(CANCEL_LABEL, color = Color.White) }
    TextButton(onClick = onSave, enabled = !isSaving) {
        Text(SAVE_LABEL, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}
