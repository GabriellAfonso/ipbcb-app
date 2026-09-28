package com.ipb.castelobranco.features.admin.members.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberCardUi

private const val INVALID_ALPHA = 0.5f
private const val CARD_TAG_FONT = 10

/** A grid card: large photo or initials, name on up to two lines, status, invalid tag. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MemberCard(
    member: MemberCardUi,
    imageLoader: ImageLoader?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(shape)
            .background(colors.surfaceContainerLowest)
            .border(1.dp, colors.outlineVariant.copy(alpha = 0.6f), shape)
            .clickable(onClick = onClick),
    ) {
        MemberAvatar(
            initials = member.initials,
            photoUrl = member.photoUrl,
            imageLoader = imageLoader,
            shape = RectangleShape,
            initialsSize = 40.sp,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .alpha(if (member.isValid) 1f else INVALID_ALPHA),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
        ) {
            Text(
                text = member.name,
                fontSize = 15.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = colors.onSurface,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                StatusChip(member.statusLabel, fontSize = CARD_TAG_FONT, compact = true)
                if (!member.isValid) InvalidTag(fontSize = CARD_TAG_FONT, compact = true)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 180)
@Composable
private fun MemberCardPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        MemberCard(
            member = MemberCardUi(3, "Bruno Carvalho", "BC", null, "Visitante", isValid = false),
            imageLoader = null,
            onClick = {},
            modifier = Modifier.width(180.dp),
        )
    }
}
