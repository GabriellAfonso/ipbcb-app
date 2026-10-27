package com.ipb.castelobranco.features.schedule.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import java.util.Locale

private val PT_BR = Locale.forLanguageTag("pt-BR")
private val CardSideMargin = 10.dp
private val CardRadius = 12.dp
private const val PAST_ALPHA = 0.45f
private const val RULE_ALPHA = 0.35f
private const val DIVIDER_ALPHA = 0.45f
private const val DARK_SURFACE_LUMINANCE = 0.5f
private const val ALL_DATES_TITLE = "Todas as datas"
private const val NEXT_LABEL = "Próximo"
private val FOOTNOTES = listOf(
    "*Cafezinho pós culto de Adoração (Ceia) todo 4° Domingo.",
    "*Aberto à participação de qualquer irmão.",
)

/** Colors of the upcoming-date row; they must stand out on `surfaceContainer` in both themes. */
private data class NextRowColors(val background: Color, val stripe: Color, val text: Color)

private val NextRowLight = NextRowColors(Color(0xFFC4DFD3), BrandColors.Green, Color(0xFF0B5A4C))
private val NextRowDark = NextRowColors(Color(0xFF1D3A32), Color(0xFF5FB59C), Color(0xFF8ED1BA))

@Composable
private fun nextRowColors(): NextRowColors =
    if (MaterialTheme.colorScheme.surface.luminance() < DARK_SURFACE_LUMINANCE) NextRowDark else NextRowLight

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

@Composable
private fun NextServiceCard(section: ScheduleSectionUi, nextDay: Int?, month: Int, monthName: String) {
    val nextRow = section.rows.firstOrNull { it.day == nextDay }
    val shortLower = section.shortTitle.lowercase(PT_BR)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = CardSideMargin)
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
private fun ScheduleDateRow(row: ScheduleRowUi, isNext: Boolean, isPast: Boolean) {
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
            .heightIn(min = 58.dp)
            .padding(horizontal = 12.dp, vertical = 12.dp),
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
