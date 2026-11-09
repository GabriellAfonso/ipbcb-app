package com.ipb.castelobranco.features.worshiphub.shared.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
private const val TEXT_SIZE_LABEL = "Tamanho da letra"
private const val SMALLER_LABEL = "A−"
private const val BIGGER_LABEL = "A+"
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

/** Lyrics text size, as a percentage of 22sp: 80% to 160% in steps of 10 (`specs/worshiphub/spec.md` §5.3). */
object LyricsTextSize {
    const val DEFAULT = 100
    const val MIN = 80
    const val MAX = 160
    const val STEP = 10
    private const val BASE_SP = 22f

    fun sp(percent: Int): Float = BASE_SP * percent / DEFAULT
}

/**
 * The chords' color: the brand orange read 2:1 on the light background, so the light theme takes a darker orange and
 * the dark theme a lighter one.
 */
@Composable
fun chordColor(): Color =
    if (MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE) ChordOnDark else ChordOnLight

/**
 * Thin green band under the top bar: the artist, then [controls] (the chart's tone) and the "Páginas | Rolagem"
 * switch.
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

/** "Tamanho da letra  A−  100%  A+" in the three dots' panel; a side is disabled at its limit. */
@Composable
fun TextSizeRow(percent: Int, canShrink: Boolean, canGrow: Boolean, onShrink: () -> Unit, onGrow: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Filled.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(TEXT_SIZE_LABEL, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = onShrink, enabled = canShrink, contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text(SMALLER_LABEL, fontWeight = FontWeight.SemiBold)
        }
        Text("$percent%", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        OutlinedButton(onClick = onGrow, enabled = canGrow, contentPadding = PaddingValues(horizontal = 12.dp)) {
            Text(BIGGER_LABEL, fontWeight = FontWeight.SemiBold)
        }
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

/** While editing in a form with its own save bar, the top bar only shows "Cancelar". */
@Composable
fun CancelBarAction(isSaving: Boolean, onCancel: () -> Unit) {
    TextButton(onClick = onCancel, enabled = !isSaving) { Text(CANCEL_LABEL, color = Color.White) }
}

/** While editing, the top bar shows "Cancelar" and "Salvar" instead of the three dots. */
@Composable
fun EditBarActions(isSaving: Boolean, onCancel: () -> Unit, onSave: () -> Unit) {
    CancelBarAction(isSaving, onCancel)
    TextButton(onClick = onSave, enabled = !isSaving) {
        Text(SAVE_LABEL, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}
