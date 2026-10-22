package com.ipb.castelobranco.features.bible.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
    val bg = if (row.isSelected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.surface
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
