package com.ipb.castelobranco.features.studies.presentation.components.sundayschool

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.features.studies.domain.model.InlineMarkup
import com.ipb.castelobranco.features.studies.domain.model.LessonBlock

/**
 * A `table` block. Each column is as wide as its share of the text, so a "1–12" column stays narrow next to a long
 * description; the table fills the width and scrolls sideways only when a column's longest word would not fit.
 */
@Composable
internal fun TableBlock(block: LessonBlock.Table, style: LessonStyle, modifier: Modifier) {
    val colors = style.colors
    val density = LocalDensity.current
    val columns = remember(block) { tableColumns(block.header, block.rows) }
    val minWidths = remember(columns, style.fontSize, density) {
        with(density) { columns.map { CELL_PADDING * 2 + (style.small.fontSize * CHAR_WIDTH * it.longestWord).toDp() } }
    }
    BoxWithConstraints(modifier = modifier.clip(BLOCK_SHAPE).border(1.dp, colors.rule, BLOCK_SHAPE)) {
        val widths = fitColumnWidths(columns.map { it.share }, minWidths.map { it.value }, maxWidth.value).map { it.dp }
        val scroll = if (widths.sumOf { it.value.toDouble() } > maxWidth.value + 0.5) {
            Modifier.horizontalScroll(rememberScrollState())
        } else {
            Modifier
        }
        Column(scroll) {
            TableRow(block.header, widths, style.small.copy(fontWeight = FontWeight.SemiBold), colors.highlight, colors)
            block.rows.forEach { row -> TableRow(row, widths, style.small, colors.page, colors) }
        }
    }
}

@Composable
private fun TableRow(
    cells: List<String>,
    widths: List<Dp>,
    textStyle: TextStyle,
    background: Color,
    colors: SundaySchoolColors,
) {
    Row(Modifier.background(background).height(IntrinsicSize.Min)) {
        cells.forEachIndexed { i, cell ->
            Box(
                Modifier
                    .width(widths.getOrElse(i) { widths.last() })
                    .fillMaxHeight()
                    .border(BorderStroke(0.5.dp, colors.rule))
                    .padding(CELL_PADDING),
            ) {
                InlineText(cell, textStyle)
            }
        }
    }
}

/** How much text a column holds: [share] sets its width, [longestWord] the narrowest it can be, in characters. */
internal data class TableColumn(val share: Float, val longestWord: Int)

/**
 * One entry per header cell, measured on the visible text (markup removed) of the header and every row.
 *
 * >>> tableColumns(listOf("Seção", "Capítulos"), listOf(listOf("I. A Conquista", "1–12"))).map { it.longestWord }
 * [9, 9]
 */
internal fun tableColumns(header: List<String>, rows: List<List<String>>): List<TableColumn> =
    header.indices.map { i ->
        val cells = (listOf(header) + rows).mapNotNull { it.getOrNull(i) }.map(::visibleText)
        TableColumn(
            share = cells.maxOf { it.length }.coerceIn(MIN_SHARE, MAX_SHARE).toFloat(),
            longestWord = cells.flatMap { it.split(' ') }.maxOf { it.length },
        )
    }

/**
 * Splits [available] by [shares], but no column below its minimum: a column that would be narrower gets its minimum
 * and the rest is shared again among the others. When the minimums alone exceed [available], each column gets its
 * minimum and the table scrolls.
 *
 * >>> fitColumnWidths(listOf(18f, 9f, 60f), listOf(110f, 85f, 110f), 350f)
 * [110.0, 85.0, 155.0]
 */
internal fun fitColumnWidths(shares: List<Float>, mins: List<Float>, available: Float): List<Float> {
    val fixed = BooleanArray(shares.size)
    while (true) {
        val free = available - shares.indices.filter { fixed[it] }.sumOf { mins[it].toDouble() }.toFloat()
        val openShare = shares.indices.filterNot { fixed[it] }.sumOf { shares[it].toDouble() }.toFloat()
        val widths = shares.indices.map { if (fixed[it]) mins[it] else free * shares[it] / openShare }
        val tooNarrow = shares.indices.filter { !fixed[it] && widths[it] < mins[it] }
        if (tooNarrow.isEmpty()) return widths
        tooNarrow.forEach { fixed[it] = true }
    }
}

private fun visibleText(cell: String): String = InlineMarkup.parse(cell).joinToString("") { it.text }

private val CELL_PADDING = 8.dp

/** Average glyph width of the text font, as a fraction of its size. */
private const val CHAR_WIDTH = 0.5f

// A column never takes less than a short label's share, nor more than a long sentence's, so one huge cell does not
// squeeze the others to their minimum.
private const val MIN_SHARE = 6
private const val MAX_SHARE = 60
