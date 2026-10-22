package com.ipb.castelobranco.features.bible.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.bible.data.local.BiblePreferences
import kotlin.math.roundToInt

/**
 * Barra com slider + / - para mudar tamanho da fonte. Mesma estética do hinário.
 * Use embaixo do reader, dentro de um Card sobreposto.
 */
@Composable
fun BibleFontSizeBar(
    fontSizeSp: Float,
    onFontSizeChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val minFont = BiblePreferences.MIN_FONT_SIZE
    val maxFont = BiblePreferences.MAX_FONT_SIZE
    val step = 1f

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onFontSizeChange((fontSizeSp - step).coerceIn(minFont, maxFont)) },
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Diminuir fonte")
            }
            Box(
                modifier = Modifier.weight(1f).height(48.dp),
                contentAlignment = Alignment.Center,
            ) {
                Slider(
                    value = fontSizeSp,
                    onValueChange = onFontSizeChange,
                    valueRange = minFont..maxFont,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Transparent,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
                val thumbColor = MaterialTheme.colorScheme.primary
                Canvas(
                    modifier = Modifier.matchParentSize().padding(horizontal = 12.dp),
                ) {
                    val fraction = (fontSizeSp - minFont) / (maxFont - minFont)
                    val x = size.width * fraction
                    val y = size.height / 2
                    drawCircle(color = thumbColor, radius = 14.dp.toPx(), center = Offset(x, y))
                }
            }
            IconButton(
                onClick = { onFontSizeChange((fontSizeSp + step).coerceIn(minFont, maxFont)) },
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Aumentar fonte")
            }
            Text(
                text = "${fontSizeSp.roundToInt()}sp",
                modifier = Modifier.padding(end = 6.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
