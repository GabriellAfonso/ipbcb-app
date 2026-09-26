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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Amber marks an invalid profile — distinct from any status the server may name. */
val InvalidContainer = Color(0xFFFBE7CF)
val OnInvalidContainer = Color(0xFF6E3B00)

const val INVALID_PROFILE = "Perfil inválido"

@Composable
fun Tag(
    text: String,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    fontSize: Int = 11,
) {
    Text(
        text = text,
        color = content,
        fontSize = fontSize.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(container)
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/** The status as the server names it. No status name is ever hardcoded in the app. */
@Composable
fun StatusChip(label: String, modifier: Modifier = Modifier, fontSize: Int = 11) {
    Tag(
        text = label,
        container = MaterialTheme.colorScheme.secondaryContainer,
        content = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier,
        fontSize = fontSize,
    )
}

@Composable
fun InvalidTag(modifier: Modifier = Modifier, fontSize: Int = 11) {
    Tag(INVALID_PROFILE, InvalidContainer, OnInvalidContainer, modifier, fontSize)
}
