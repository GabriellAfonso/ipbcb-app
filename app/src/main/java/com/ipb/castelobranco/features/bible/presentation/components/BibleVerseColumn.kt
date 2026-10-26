package com.ipb.castelobranco.features.bible.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.features.bible.domain.model.VerseRef

data class VerseRowData(
    val ref: VerseRef,
    val text: String,
    val isSelected: Boolean,
    /** Cited by whoever opened the passage; selection paints over it. */
    val isHighlighted: Boolean = false,
)

@Composable
fun BibleVerseColumn(
    bookName: String,
    chapter: Int,
    verses: List<VerseRowData>,
    fontSizeSp: Float,
    onVerseClick: (VerseRef) -> Unit,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    SelectionContainer(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = 16.dp, vertical = 12.dp,
            ),
        ) {
            item(key = "header") {
                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    Text(
                        text = "$bookName $chapter",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            }
            items(verses, key = { it.ref.verse }) { row ->
                VerseRow(
                    row = row,
                    fontSizeSp = fontSizeSp,
                    onClick = { onVerseClick(row.ref) },
                )
            }
        }
    }
}

@Composable
private fun VerseRow(
    row: VerseRowData,
    fontSizeSp: Float,
    onClick: () -> Unit,
) {
    val bg = when {
        row.isSelected -> MaterialTheme.colorScheme.primaryContainer
        row.isHighlighted -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val numberColor = MaterialTheme.colorScheme.primary
    val textColor = MaterialTheme.colorScheme.onSurface

    val annotated = buildAnnotatedString {
        withStyle(
            SpanStyle(
                fontSize = (fontSizeSp * 0.65f).sp,
                color = numberColor,
                fontWeight = FontWeight.Bold,
                baselineShift = androidx.compose.ui.text.style.BaselineShift.Superscript,
            )
        ) {
            append("${row.ref.verse} ")
        }
        append(row.text)
    }

    Text(
        text = annotated,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(bg, RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        color = textColor,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp + 8f).sp,
        style = MaterialTheme.typography.bodyLarge,
    )
}

/** Lines of each placeholder verse; the last line of each is shorter, like real text. */
private val PlaceholderVerseLines = listOf(3, 2, 3, 1, 2, 3, 2, 2, 3, 1, 2, 3)
private const val PlaceholderLastLineFraction = 0.6f
private const val PlaceholderPulseMs = 800

/**
 * Stands in for [BibleVerseColumn] while the chapter's text is read: same paddings, header and line height for
 * [fontSizeSp], so nothing moves when the text arrives. [title] is null while even the book is unknown.
 */
@Composable
fun BibleVerseColumnPlaceholder(
    title: String?,
    fontSizeSp: Float,
    modifier: Modifier = Modifier,
) {
    val pulse by rememberInfiniteTransition(label = "versePlaceholder").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(PlaceholderPulseMs), RepeatMode.Reverse),
        label = "versePlaceholderAlpha",
    )
    val barColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val barShape = RoundedCornerShape(4.dp)
    val lineHeight = with(LocalDensity.current) { (fontSizeSp + 8f).sp.toDp() }

    Column(
        modifier = modifier
            .clipToBounds()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Column(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                text = title.orEmpty(),
                modifier = if (title == null) {
                    Modifier
                        .width(160.dp)
                        .graphicsLayer { alpha = pulse }
                        .background(barColor, barShape)
                } else {
                    Modifier
                },
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.headlineSmall,
            )
        }
        PlaceholderVerseLines.forEach { lines ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { alpha = pulse }
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                repeat(lines) { line ->
                    val fraction = if (line == lines - 1) PlaceholderLastLineFraction else 1f
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(lineHeight)
                            .padding(vertical = lineHeight * 0.2f)
                            .background(barColor, barShape),
                    )
                }
            }
        }
    }
}
