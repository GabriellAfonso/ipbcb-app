package com.ipb.castelobranco.features.worshiphub.tables.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.core.presentation.theme.BrandColors

/** Horizontal margin between the content sheet edge and a list card. */
val CardSideMargin = 10.dp
private val CardRadius = 12.dp
private val RowMinHeight = 58.dp
private const val HIGHLIGHTED_POSITIONS = 3
private const val DATE_RULE_ALPHA = 0.35f
private const val DIVIDER_ALPHA = 0.45f

/** Rounded card that groups the rows of one list (a Sunday, a ranking). */
@Composable
fun TableCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CardSideMargin)
            .clip(RoundedCornerShape(CardRadius))
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) { content() }
}

/**
 * Shape of a card row rendered as its own lazy item: only the first and last rows round their outer corners,
 * so consecutive items read as one card.
 */
fun cardItemShape(index: Int, lastIndex: Int): Shape {
    val top = if (index == 0) CardRadius else 0.dp
    val bottom = if (index == lastIndex) CardRadius else 0.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}

/** Modifier for a lazy item that is one row of a card (see [cardItemShape]). */
@Composable
fun Modifier.cardItem(index: Int, lastIndex: Int): Modifier = this
    .fillMaxWidth()
    .padding(horizontal = CardSideMargin)
    .clip(cardItemShape(index, lastIndex))
    .background(MaterialTheme.colorScheme.surfaceContainer)

@Composable
fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA))
}

/** Standard list row: position, a weighted middle slot and trailing content. */
@Composable
fun TableRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    background: Color = Color.Transparent,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(background)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .heightIn(min = RowMinHeight)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** Row number; the first ranked positions get a green circle when [highlightTop] is set. */
@Composable
fun PositionLabel(position: Int, highlightTop: Boolean = false) {
    Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
        if (highlightTop && position <= HIGHLIGHTED_POSITIONS) {
            Box(
                modifier = Modifier.size(20.dp).clip(CircleShape).background(BrandColors.Green),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = position.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = centeredDigitStyle(11.sp),
                )
            }
        } else {
            Text(
                text = position.toString(),
                color = MaterialTheme.colorScheme.outline,
                style = centeredDigitStyle(12.sp),
            )
        }
    }
}

/**
 * Text style whose box is exactly one line of [size], trimmed and centered, so digits sit in the visual middle
 * of a fixed-size container instead of riding high on the inherited line height and font padding.
 */
@Composable
private fun centeredDigitStyle(size: TextUnit): TextStyle = LocalTextStyle.current.merge(
    TextStyle(
        fontSize = size,
        lineHeight = size,
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
)

/** Title over artist. A blank [title] shows [placeholder] in italics. */
@Composable
fun SongTitleArtist(title: String, artist: String?, modifier: Modifier = Modifier, placeholder: String = "") {
    Column(modifier = modifier) {
        if (title.isBlank()) {
            Text(
                text = placeholder,
                color = MaterialTheme.colorScheme.outline,
                fontStyle = FontStyle.Italic,
                fontSize = 13.5.sp,
            )
            return@Column
        }
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (!artist.isNullOrBlank()) {
            Text(
                text = artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                lineHeight = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** Tone badge. A blank [tone] renders a dashed-looking "Tom" placeholder. */
@Composable
fun ToneChip(tone: String, modifier: Modifier = Modifier, large: Boolean = false) {
    val shape = RoundedCornerShape(if (large) 8.dp else 6.dp)
    val base = modifier
        .widthIn(min = if (large) 42.dp else 34.dp)
        .clip(shape)
    val styled = if (tone.isBlank()) {
        base.border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
    } else {
        base.background(MaterialTheme.colorScheme.secondaryContainer)
    }
    Text(
        text = tone.ifBlank { "Tom" },
        modifier = styled.padding(horizontal = 6.dp, vertical = if (large) 4.dp else 2.dp),
        color = if (tone.isBlank()) MaterialTheme.colorScheme.outline
        else MaterialTheme.colorScheme.onSecondaryContainer,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = if (large) 14.sp else 11.5.sp,
        textAlign = TextAlign.Center,
    )
}

/** "14 vezes" with the word muted. */
@Composable
fun PlayCount(count: Int) {
    val unitStyle = SpanStyle(
        color = MaterialTheme.colorScheme.outline,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
    )
    Text(
        text = buildAnnotatedString {
            append(count.toString())
            withStyle(unitStyle) { append(if (count == 1) " vez" else " vezes") }
        },
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
    )
}

/** Thin bar filled to [fraction] (0..1). */
@Composable
fun ProportionBar(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(BrandColors.Green)
        )
    }
}

/** Date centered between two hairlines, above a Sunday card. */
@Composable
fun CenteredDateHeader(date: String) {
    val rule = MaterialTheme.colorScheme.outline.copy(alpha = DATE_RULE_ALPHA)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = rule)
        Text(
            text = date,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = rule)
    }
}
