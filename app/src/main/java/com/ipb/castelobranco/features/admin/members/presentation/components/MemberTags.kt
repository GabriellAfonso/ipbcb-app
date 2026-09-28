package com.ipb.castelobranco.features.admin.members.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Amber marks an invalid profile — distinct from any status the server may name. */
val InvalidContainer = Color(0xFFFBE7CF)
val OnInvalidContainer = Color(0xFF6E3B00)

const val INVALID_PROFILE = "Perfil inválido"

private const val COMPACT_LINE_HEIGHT = 1.15f

@Composable
fun Tag(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    fontSize: Int = 11,
    compact: Boolean = false,
) {
    Text(
        text = text,
        color = content,
        fontSize = fontSize.sp,
        // Compact drops the font's extra line spacing so the tag hugs its text.
        lineHeight = if (compact) fontSize.sp * COMPACT_LINE_HEIGHT else TextUnit.Unspecified,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = if (compact) 1.dp else 2.dp),
    )
}

/** The status as the server names it. No status name is ever hardcoded in the app. */
@Composable
fun StatusChip(label: String, modifier: Modifier = Modifier, fontSize: Int = 11, compact: Boolean = false) {
    Tag(
        text = label,
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
        fontSize = fontSize,
        compact = compact,
    )
}

@Composable
fun InvalidTag(modifier: Modifier = Modifier, fontSize: Int = 11, compact: Boolean = false) {
    Tag(INVALID_PROFILE, InvalidContainer, OnInvalidContainer, modifier, fontSize, compact)
}
