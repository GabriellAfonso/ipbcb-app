package com.ipb.castelobranco.features.schedule.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.core.presentation.components.isCompactHeight
import com.ipb.castelobranco.core.presentation.components.nextRowColors
import java.util.Locale

private val PT_BR = Locale.forLanguageTag("pt-BR")
private val CardSideMargin = 10.dp
private val CardRadius = 12.dp
private val CellGap = 8.dp
private val LandscapeNextWidth = 220.dp
private const val LANDSCAPE_DATE_COLUMNS = 2
private const val PAST_ALPHA = 0.45f
private const val RULE_ALPHA = 0.35f
private const val DIVIDER_ALPHA = 0.45f
private const val ALL_DATES_TITLE = "Todas as datas"
private const val NEXT_LABEL = "Próximo"
private val FOOTNOTES = listOf(
    "*Cafezinho pós culto de Adoração (Ceia) todo 4° Domingo.",
    "*Aberto à participação de qualquer irmão.",
)

/**
 * One service of the month: "next" card on top, then every date. [nextDay] is the upcoming day (null when all
 * dates have passed); [monthName] is the lowercase month name used in the "all passed" message.
 */
@Composable
fun ScheduleTabContent(
    section: ScheduleSectionUi,
    nextDay: Int?,
    month: Int,
    monthName: String,
    modifier: Modifier = Modifier,
) {
    val rows = section.rows
    if (isCompactHeight()) {
        LandscapeTabContent(section, nextDay, month, monthName, modifier)
        return
    }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 8.dp),
    ) {
        item(key = "next") {
            NextServiceCard(section = section, nextDay = nextDay, month = month, monthName = monthName)
        }
        item(key = "title") { RuleTitle(ALL_DATES_TITLE) }
        itemsIndexed(rows, key = { _, row -> row.day }) { index, row ->
            val isNext = row.day == nextDay
            val isPast = nextDay == null || row.day < nextDay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = CardSideMargin)
                    .clip(cardItemShape(index, rows.lastIndex))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
            ) {
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA))
                }
                ScheduleDateRow(row = row, isNext = isNext, isPast = isPast && !isNext)
            }
        }
        item(key = "foot") { Footnotes() }
    }
}

/**
 * Lying down (`specs/schedule/spec.md` §5): the "next" card and the footnotes in a column on the left, every date on
 * the right in two columns of compact cells. A month of dates fits without scrolling; each side scrolls if not.
 */
@Composable
private fun LandscapeTabContent(
    section: ScheduleSectionUi,
    nextDay: Int?,
    month: Int,
    monthName: String,
    modifier: Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = CardSideMargin, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(CellGap),
    ) {
        Column(
            modifier = Modifier
                .width(LandscapeNextWidth)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
        ) {
            NextServiceCard(section, nextDay, month, monthName, sideMargin = 0.dp)
            Footnotes()
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(CellGap),
        ) {
            section.rows.chunked(LANDSCAPE_DATE_COLUMNS).forEach { line ->
                Row(horizontalArrangement = Arrangement.spacedBy(CellGap)) {
                    line.forEach { row ->
                        val isNext = row.day == nextDay
                        val isPast = nextDay == null || row.day < nextDay
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(CardRadius))
                                .background(MaterialTheme.colorScheme.surfaceContainer),
                        ) {
                            ScheduleDateRow(row = row, isNext = isNext, isPast = isPast && !isNext, compact = true)
                        }
                    }
                    repeat(LANDSCAPE_DATE_COLUMNS - line.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun NextServiceCard(
    section: ScheduleSectionUi,
    nextDay: Int?,
    month: Int,
    monthName: String,
    sideMargin: Dp = CardSideMargin,
) {
    val nextRow = section.rows.firstOrNull { it.day == nextDay }
    val shortLower = section.shortTitle.lowercase(PT_BR)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = sideMargin)
            .clip(RoundedCornerShape(CardRadius))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val label = buildList {
            add(NEXT_LABEL)
            if (nextRow != null) add("$shortLower ${"%02d/%02d".format(nextRow.day, month)}")
            if (nextRow != null && section.time.isNotBlank()) add(section.time)
        }.joinToString(" · ")
        Text(
            text = label.uppercase(PT_BR),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.outline,
        )
        if (nextRow != null) {
            Text(
                text = nextRow.member,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(text = section.title, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Text(
                text = "Todos os ${shortLower}s de $monthName já passaram.",
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ScheduleDateRow(row: ScheduleRowUi, isNext: Boolean, isPast: Boolean, compact: Boolean = false) {
    val next = nextRowColors()
    val stripeWidth = 3.dp
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isNext) {
                    Modifier
                        .background(next.background)
                        .drawBehind {
                            drawRect(next.stripe, Offset.Zero, Size(stripeWidth.toPx(), size.height))
                        }
                } else Modifier
            )
            .alpha(if (isPast) PAST_ALPHA else 1f)
            .heightIn(min = if (compact) 48.dp else 58.dp)
            .padding(horizontal = 12.dp, vertical = if (compact) 6.dp else 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.width(44.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "%02d".format(row.day),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = if (isNext) next.text else MaterialTheme.colorScheme.onSurface,
            )
            if (row.weekday.isNotBlank()) {
                Text(
                    text = row.weekday.uppercase(PT_BR),
                    fontSize = 10.5.sp,
                    letterSpacing = 0.6.sp,
                    color = if (isNext) next.text else MaterialTheme.colorScheme.outline,
                )
            }
        }
        Text(
            text = row.member,
            modifier = Modifier.weight(1f),
            fontSize = 14.sp,
            fontWeight = if (isNext) FontWeight.Bold else FontWeight.Medium,
            color = if (isNext) next.text else MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RuleTitle(text: String) {
    val rule = MaterialTheme.colorScheme.outline.copy(alpha = RULE_ALPHA)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = rule)
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.weight(1f), color = rule)
    }
}

@Composable
private fun Footnotes() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FOOTNOTES.forEach { note ->
            Text(
                text = note,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private fun cardItemShape(index: Int, lastIndex: Int): RoundedCornerShape {
    val top = if (index == 0) CardRadius else 0.dp
    val bottom = if (index == lastIndex) CardRadius else 0.dp
    return RoundedCornerShape(topStart = top, topEnd = top, bottomStart = bottom, bottomEnd = bottom)
}
