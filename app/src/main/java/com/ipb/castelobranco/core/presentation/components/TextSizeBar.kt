package com.ipb.castelobranco.core.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.domain.model.TextScale
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.core.presentation.theme.onPrimaryContainerLight

private const val TEXT_SIZE_LABEL = "Tamanho do texto"
private const val SHRINK_LABEL = "Diminuir texto"
private const val GROW_LABEL = "Aumentar texto"
private const val ON_BRAND_TRACK_ALPHA = 0.25f
private const val DISABLED_ALPHA = 0.38f

private val ThumbSize = 24.dp
private val ValueMinWidth = 44.dp

@Immutable
data class TextSizeBarColors(
    val container: Color,
    val content: Color,
    val activeTrack: Color,
    val inactiveTrack: Color,
    val thumb: Color,
)

object TextSizeBarDefaults {
    /** Over the app's surfaces (Bible, studies) and, with a clear [container], inside the three dots' panel. */
    @Composable
    fun colors(container: Color = MaterialTheme.colorScheme.surfaceContainer): TextSizeBarColors = TextSizeBarColors(
        container = container,
        content = MaterialTheme.colorScheme.onSurfaceVariant,
        activeTrack = MaterialTheme.colorScheme.primary,
        inactiveTrack = MaterialTheme.colorScheme.surfaceVariant,
        thumb = MaterialTheme.colorScheme.primary,
    )

    /** Over the brand green (hymnal). */
    fun onBrandColors(): TextSizeBarColors = TextSizeBarColors(
        container = Color.Transparent,
        content = Color.White,
        activeTrack = onPrimaryContainerLight,
        inactiveTrack = Color.White.copy(alpha = ON_BRAND_TRACK_ALPHA),
        thumb = onPrimaryContainerLight,
    )
}

/**
 * The app's one reading text size control (`specs/core/spec.md` §6.7.1): "−", a slider and "+", with the value in
 * percent. Slider and buttons move in [TextScale.STEP]s; each end disables its button. While dragging, every new stop
 * reaches [onPercentChange] at once, so the text follows the finger.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextSizeBar(
    percent: Int,
    onPercentChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    colors: TextSizeBarColors = TextSizeBarDefaults.colors(),
) {
    // The finger's position while dragging, so the thumb never waits for the saved value to come back.
    var dragValue by remember { mutableStateOf<Float?>(null) }
    val currentPercent by rememberUpdatedState(percent)
    val currentOnChange by rememberUpdatedState(onPercentChange)
    val sliderColors = SliderDefaults.colors(
        thumbColor = colors.thumb,
        activeTrackColor = colors.activeTrack,
        inactiveTrackColor = colors.inactiveTrack,
        activeTickColor = Color.Transparent,
        inactiveTickColor = Color.Transparent,
    )
    val buttonColors = IconButtonDefaults.iconButtonColors(
        contentColor = colors.content,
        disabledContentColor = colors.content.copy(alpha = DISABLED_ALPHA),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.container)
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onPercentChange(TextScale.shrunk(percent)) },
            enabled = TextScale.canShrink(percent),
            colors = buttonColors,
        ) {
            Icon(Icons.Filled.Remove, contentDescription = SHRINK_LABEL)
        }
        Slider(
            value = dragValue ?: percent.toFloat(),
            onValueChange = { value ->
                dragValue = value
                val snapped = TextScale.normalize(value)
                if (snapped != currentPercent) currentOnChange(snapped)
            },
            onValueChangeFinished = { dragValue = null },
            valueRange = TextScale.MIN.toFloat()..TextScale.MAX.toFloat(),
            steps = TextScale.SLIDER_STEPS,
            colors = sliderColors,
            thumb = { Box(Modifier.size(ThumbSize).background(colors.thumb, CircleShape)) },
            track = { state ->
                SliderDefaults.Track(
                    sliderState = state,
                    colors = sliderColors,
                    drawStopIndicator = null,
                    drawTick = { _, _ -> },
                    thumbTrackGapSize = 0.dp,
                )
            },
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { onPercentChange(TextScale.grown(percent)) },
            enabled = TextScale.canGrow(percent),
            colors = buttonColors,
        ) {
            Icon(Icons.Filled.Add, contentDescription = GROW_LABEL)
        }
        Text(
            text = "$percent%",
            modifier = Modifier.widthIn(min = ValueMinWidth).padding(end = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = colors.content,
            textAlign = TextAlign.End,
        )
    }
}

/** The `tT` top-bar button that opens and closes the [TextSizeBar]. */
@Composable
fun TextSizeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(Icons.Filled.FormatSize, contentDescription = TEXT_SIZE_LABEL, tint = tint)
    }
}

@Preview
@Composable
private fun TextSizeBarPreview() {
    IPBCasteloBrancoTheme(dynamicColor = false) {
        Column {
            TextSizeBar(percent = TextScale.DEFAULT, onPercentChange = {})
            TextSizeBar(percent = TextScale.MIN, onPercentChange = {})
            TextSizeBar(
                percent = 140,
                onPercentChange = {},
                modifier = Modifier.background(ipbGreen),
                colors = TextSizeBarDefaults.onBrandColors(),
            )
        }
    }
}
