package com.ipb.castelobranco.features.studies.presentation.components.sundayschool

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.features.studies.domain.model.BibleReference
import com.ipb.castelobranco.features.studies.domain.model.BookFacts
import com.ipb.castelobranco.features.studies.domain.model.InlineMarkup
import com.ipb.castelobranco.features.studies.domain.model.LessonBlock
import com.ipb.castelobranco.features.studies.domain.model.LessonIcon
import com.ipb.castelobranco.features.studies.domain.model.ListStyle
import com.ipb.castelobranco.features.studies.domain.model.ProofText
import com.ipb.castelobranco.features.studies.presentation.components.ProofTextChips

/** Text size and colours every block of one lesson is drawn with. */
data class LessonStyle(
    val colors: SundaySchoolColors,
    /** Body size chosen with "Aa", in sp. */
    val fontSize: Float,
) {
    val body: TextStyle
        get() = TextStyle(
            fontFamily = SundaySchoolTextFont,
            fontSize = fontSize.sp,
            lineHeight = (fontSize * BODY_LINE_HEIGHT).sp,
            color = colors.text,
            fontFeatureSettings = LINING_FIGURES,
        )

    val small: TextStyle
        get() = body.copy(fontSize = (fontSize * SMALL).sp, lineHeight = (fontSize * SMALL * SMALL_LINE_HEIGHT).sp)

    val title: TextStyle
        get() = TextStyle(
            fontFamily = SundaySchoolTitleFont,
            fontWeight = FontWeight.Bold,
            fontSize = (fontSize * TITLE).sp,
            lineHeight = (fontSize * TITLE * 1.2f).sp,
            color = colors.text,
            fontFeatureSettings = LINING_FIGURES,
        )

    val cardTitle: TextStyle
        get() = title.copy(fontSize = (fontSize * CARD_TITLE).sp, lineHeight = (fontSize * CARD_TITLE * 1.2f).sp)

    val label: TextStyle
        get() = TextStyle(
            fontFamily = SundaySchoolTextFont,
            fontWeight = FontWeight.SemiBold,
            fontSize = (fontSize * LABEL).sp,
            letterSpacing = 0.08.em,
            color = colors.gold,
            fontFeatureSettings = LINING_FIGURES,
        )

    private companion object {
        /** Cormorant Garamond defaults to old-style figures: the "2" of "2 Samuel" would be x-height tall (R-13a). */
        const val LINING_FIGURES = "lnum"
        const val BODY_LINE_HEIGHT = 1.6f
        const val SMALL = 0.85f
        const val SMALL_LINE_HEIGHT = 1.4f
        const val TITLE = 1.45f
        const val CARD_TITLE = 1.15f
        const val LABEL = 0.72f
    }
}

/** `**bold**` and `*italic*` only; everything else as typed. */
fun inlineText(text: String): AnnotatedString = buildAnnotatedString {
    InlineMarkup.parse(text).forEach { span ->
        val style = SpanStyle(
            fontWeight = if (span.bold) FontWeight.SemiBold else null,
            fontStyle = if (span.italic) FontStyle.Italic else null,
        )
        withStyle(style) { append(span.text) }
    }
}

@Composable
private fun InlineText(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val annotated = remember(text) { inlineText(text) }
    Text(text = annotated, style = style, modifier = modifier)
}

@Composable
private fun Chips(proofTexts: List<ProofText>, onReferenceClick: (BibleReference) -> Unit) {
    if (proofTexts.isEmpty()) return
    ProofTextChips(
        proofTexts = proofTexts,
        onReferenceClick = onReferenceClick,
        modifier = Modifier.padding(top = 6.dp),
    )
}

/** One block as members read it. [LessonBlock.Unknown] draws nothing. */
@Composable
fun LessonBlockView(
    block: LessonBlock,
    books: Map<String, BookFacts>,
    style: LessonStyle,
    onReferenceClick: (BibleReference) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (block) {
        is LessonBlock.Paragraph -> Column(modifier) {
            InlineText(block.text, style.body)
            Chips(block.proofTexts, onReferenceClick)
        }
        is LessonBlock.BulletList -> ListBlock(block, style, onReferenceClick, modifier)
        is LessonBlock.KeyVerse -> KeyVerseBlock(block, style, onReferenceClick, modifier)
        is LessonBlock.Cards -> CardsBlock(block, style, onReferenceClick, modifier)
        is LessonBlock.Comparison -> ComparisonBlock(block, style, modifier)
        is LessonBlock.Steps -> StepsBlock(block, style, modifier)
        is LessonBlock.Table -> TableBlock(block, style, modifier)
        is LessonBlock.Stats -> StatsBlock(block, style, modifier)
        is LessonBlock.Timeline -> TimelineBlock(block, style, modifier)
        is LessonBlock.BookFactsBlock -> BookFactsCard(block, books[block.book], style, modifier)
        is LessonBlock.Unknown -> Unit
    }
}

@Composable
private fun ListBlock(
    block: LessonBlock.BulletList,
    style: LessonStyle,
    onReferenceClick: (BibleReference) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        block.items.forEachIndexed { index, item ->
            Row {
                Text(
                    text = marker(block.style, index),
                    style = style.body.copy(color = style.colors.gold, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.widthIn(min = 28.dp),
                )
                Column(Modifier.weight(1f)) {
                    InlineText(item.text, style.body)
                    Chips(item.proofTexts, onReferenceClick)
                }
            }
        }
    }
}

private fun marker(style: ListStyle, index: Int): String = when (style) {
    ListStyle.BULLET -> "•"
    ListStyle.NUMBERED -> "${index + 1}."
    ListStyle.ROMAN -> "${roman(index + 1)}."
}

private val ROMAN_NUMERALS = listOf(
    1000 to "M", 900 to "CM", 500 to "D", 400 to "CD", 100 to "C", 90 to "XC",
    50 to "L", 40 to "XL", 10 to "X", 9 to "IX", 5 to "V", 4 to "IV", 1 to "I",
)

fun roman(number: Int): String {
    var rest = number
    return buildString {
        ROMAN_NUMERALS.forEach { (value, symbol) ->
            while (rest >= value) {
                append(symbol)
                rest -= value
            }
        }
    }
}

@Composable
private fun KeyVerseBlock(
    block: LessonBlock.KeyVerse,
    style: LessonStyle,
    onReferenceClick: (BibleReference) -> Unit,
    modifier: Modifier,
) {
    val colors = style.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(BLOCK_SHAPE)
            .parchment(colors)
            .border(1.dp, colors.parchmentEdge, BLOCK_SHAPE)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        block.label?.let { Text(it.uppercase(), style = style.label, modifier = Modifier.padding(bottom = 6.dp)) }
        InlineText(
            text = "“${block.text}”",
            style = style.body.copy(
                fontFamily = SundaySchoolTitleFont,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = (style.fontSize * KEY_VERSE).sp,
                lineHeight = (style.fontSize * KEY_VERSE * 1.35f).sp,
            ),
        )
        Chips(listOf(block.proof), onReferenceClick)
    }
}

@Composable
private fun CardsBlock(
    block: LessonBlock.Cards,
    style: LessonStyle,
    onReferenceClick: (BibleReference) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        block.items.forEach { card ->
            OutlinedBox(style) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    card.icon?.let { IconBadge(it, style) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        card.label?.let { Text(it.uppercase(), style = style.label) }
                        InlineText(card.title, style.cardTitle)
                        card.text?.let { InlineText(it, style.body) }
                        Chips(card.proofTexts, onReferenceClick)
                    }
                }
            }
        }
    }
}

@Composable
private fun ComparisonBlock(block: LessonBlock.Comparison, style: LessonStyle, modifier: Modifier) {
    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        block.items.forEach { item ->
            val colors = style.colors
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(BLOCK_SHAPE)
                    .background(if (item.highlight) colors.highlight else colors.page)
                    .border(
                        width = if (item.highlight) 2.dp else 1.dp,
                        color = if (item.highlight) colors.gold else colors.rule,
                        shape = BLOCK_SHAPE,
                    )
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(item.label.uppercase(), style = style.label)
                InlineText(item.title, style.cardTitle)
                item.text?.let { InlineText(it, style.small) }
            }
        }
    }
}

@Composable
private fun StepsBlock(block: LessonBlock.Steps, style: LessonStyle, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        block.items.forEachIndexed { index, step ->
            if (index > 0) {
                Icon(
                    imageVector = Icons.Filled.ArrowDownward,
                    contentDescription = null,
                    tint = style.colors.gold,
                    modifier = Modifier.padding(vertical = 4.dp).size(20.dp),
                )
            }
            OutlinedBox(style) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    step.label?.let { Text(it.uppercase(), style = style.label) }
                    InlineText(step.title, style.cardTitle)
                    step.text?.let { InlineText(it, style.body) }
                }
            }
        }
    }
}

@Composable
private fun TableBlock(block: LessonBlock.Table, style: LessonStyle, modifier: Modifier) {
    val colors = style.colors
    Box(
        modifier = modifier
            .clip(BLOCK_SHAPE)
            .border(1.dp, colors.rule, BLOCK_SHAPE)
            .horizontalScroll(rememberScrollState()),
    ) {
        Column {
            TableRow(block.header, style.small.copy(fontWeight = FontWeight.SemiBold), colors.highlight, colors)
            block.rows.forEach { row -> TableRow(row, style.small, colors.page, colors) }
        }
    }
}

@Composable
private fun TableRow(cells: List<String>, textStyle: TextStyle, background: Color, colors: SundaySchoolColors) {
    Row(Modifier.background(background).height(IntrinsicSize.Min)) {
        cells.forEach { cell ->
            Box(
                Modifier
                    .width(TABLE_CELL_WIDTH)
                    .fillMaxHeight()
                    .border(BorderStroke(0.5.dp, colors.rule))
                    .padding(8.dp),
            ) {
                InlineText(cell, textStyle)
            }
        }
    }
}

@Composable
private fun StatsBlock(block: LessonBlock.Stats, style: LessonStyle, modifier: Modifier) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        block.items.forEach { stat ->
            OutlinedBox(style, modifier = Modifier.widthIn(min = 120.dp, max = 200.dp), fill = false) {
                Column {
                    Text(
                        text = stat.value,
                        style = style.title.copy(
                            color = style.colors.gold,
                            fontSize = (style.fontSize * STAT_VALUE).sp,
                            lineHeight = (style.fontSize * STAT_VALUE).sp,
                        ),
                    )
                    InlineText(stat.label, style.small)
                }
            }
        }
    }
}

@Composable
private fun TimelineBlock(block: LessonBlock.Timeline, style: LessonStyle, modifier: Modifier) {
    val colors = style.colors
    Column(modifier) {
        block.events.forEachIndexed { index, event ->
            Row(Modifier.height(IntrinsicSize.Min)) {
                Column(
                    modifier = Modifier.width(20.dp).fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .padding(top = 6.dp)
                            .size(10.dp)
                            .background(colors.gold, CircleShape),
                    )
                    if (index < block.events.lastIndex) {
                        Box(Modifier.width(2.dp).weight(1f).background(colors.rule))
                    }
                }
                Column(Modifier.padding(start = 10.dp, bottom = 14.dp)) {
                    Text(event.whenLabel, style = style.label.copy(letterSpacing = TextUnit.Unspecified))
                    InlineText(event.text, style.body)
                }
            }
        }
    }
}

@Composable
private fun BookFactsCard(
    block: LessonBlock.BookFactsBlock,
    facts: BookFacts?,
    style: LessonStyle,
    modifier: Modifier,
) {
    OutlinedBox(style, modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(facts?.name ?: block.book, style = style.title)
            facts?.let {
                Text("Livro nº ${it.position} da Bíblia", style = style.label)
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Fact(it.chapters.toString(), if (it.chapters == 1) "capítulo" else "capítulos", style)
                    Fact(it.verses.toString(), if (it.verses == 1) "versículo" else "versículos", style)
                }
            }
            block.author?.let { FactLine("Autor", it, style) }
            block.date?.let { FactLine("Data", it, style) }
            block.style?.let { FactLine("Estilo", it, style) }
            block.readingTime?.let { FactLine("Tempo de leitura", it, style) }
        }
    }
}

@Composable
private fun Fact(value: String, label: String, style: LessonStyle) {
    Column {
        Text(value, style = style.title.copy(color = style.colors.gold))
        Text(label, style = style.small.copy(color = style.colors.muted))
    }
}

@Composable
private fun FactLine(label: String, value: String, style: LessonStyle) {
    Column {
        Text(label.uppercase(), style = style.label)
        InlineText(value, style.body)
    }
}

@Composable
private fun OutlinedBox(
    style: LessonStyle,
    modifier: Modifier = Modifier,
    fill: Boolean = true,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = if (fill) modifier.fillMaxWidth() else modifier,
        shape = BLOCK_SHAPE,
        color = style.colors.page,
        border = BorderStroke(1.dp, style.colors.rule),
    ) {
        Box(Modifier.padding(14.dp)) { content() }
    }
}

/** The section/card icon on a round cream badge, gold duotone. */
@Composable
fun IconBadge(icon: LessonIcon, style: LessonStyle) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(style.colors.highlight, CircleShape)
            .border(1.dp, style.colors.rule, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        LessonIconImage(icon = icon, tint = style.colors.gold, size = 24.dp)
    }
}

private val BLOCK_SHAPE = RoundedCornerShape(12.dp)
private val TABLE_CELL_WIDTH = 140.dp
private const val KEY_VERSE = 1.2f
private const val STAT_VALUE = 1.9f
