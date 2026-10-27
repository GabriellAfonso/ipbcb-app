package com.ipb.castelobranco.features.studies.presentation.components.sundayschool

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.features.studies.domain.model.BibleReference
import com.ipb.castelobranco.features.studies.domain.model.BookFacts
import com.ipb.castelobranco.features.studies.domain.model.LessonBlock
import com.ipb.castelobranco.features.studies.domain.model.LessonContent
import com.ipb.castelobranco.features.studies.domain.model.LessonIcon
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** What the parchment header of a lesson shows. */
data class LessonHeaderInfo(
    val title: String,
    val yearLabel: String,
    val date: LocalDate?,
    val teacher: String,
)

/**
 * A whole lesson as members read it: the member reader and the panel preview both draw it, so the preview is
 * exactly what members will see (FR-020).
 */
@Composable
fun LessonContentView(
    header: LessonHeaderInfo,
    content: LessonContent,
    books: Map<String, BookFacts>,
    style: LessonStyle,
    onReferenceClick: (BibleReference) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(bottom = 32.dp),
) {
    LazyColumn(modifier = modifier, state = state, contentPadding = contentPadding) {
        item(key = HEADER_KEY) { LessonHeader(header, style) }
        lessonSections(content, books, style, onReferenceClick)
    }
}

/** One item per section title and per block, so long lessons scroll smoothly. */
fun LazyListScope.lessonSections(
    content: LessonContent,
    books: Map<String, BookFacts>,
    style: LessonStyle,
    onReferenceClick: (BibleReference) -> Unit,
) {
    content.sections.forEachIndexed { s, section ->
        item(key = "section-$s") {
            SectionTitle(title = section.title, icon = section.icon, style = style, first = s == 0)
        }
        section.blocks.forEachIndexed { b, block ->
            if (block !is LessonBlock.Unknown) {
                item(key = "block-$s-$b") {
                    LessonBlockView(
                        block = block,
                        books = books,
                        style = style,
                        onReferenceClick = onReferenceClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PAGE_PADDING, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun LessonHeader(header: LessonHeaderInfo, style: LessonStyle) {
    val colors = style.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .parchment(colors)
            .padding(horizontal = PAGE_PADDING, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "ESCOLA BÍBLICA DOMINICAL",
            style = style.label,
            textAlign = TextAlign.Center,
        )
        Text(
            text = header.title,
            style = style.title.copy(
                fontSize = (style.fontSize * HEADER_TITLE).sp,
                lineHeight = (style.fontSize * HEADER_TITLE * HEADER_LINE_HEIGHT).sp,
            ),
            textAlign = TextAlign.Center,
        )
        Text(
            text = header.yearLabel,
            style = style.small.copy(color = colors.muted, fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
        )
        lessonMeta(header.date, header.teacher)?.let {
            Text(text = it, style = style.small.copy(color = colors.muted), textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun SectionTitle(
    title: String,
    icon: LessonIcon?,
    style: LessonStyle,
    first: Boolean,
) {
    Column(Modifier.padding(horizontal = PAGE_PADDING).padding(top = if (first) 24.dp else 32.dp, bottom = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            icon?.let { IconBadge(it, style) }
            Text(text = title, style = style.title, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = style.colors.rule)
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** "16/08/2026 · Prof. Fulano", each part only when known; null when neither is. */
fun lessonMeta(date: LocalDate?, teacher: String): String? =
    listOfNotNull(date?.format(DATE_FORMAT), teacher.trim().ifBlank { null }?.let { "Prof. $it" })
        .joinToString(" · ")
        .ifBlank { null }

private val PAGE_PADDING = 20.dp
private const val HEADER_KEY = "header"
private const val HEADER_TITLE = 1.7f
private const val HEADER_LINE_HEIGHT = 1.15f
