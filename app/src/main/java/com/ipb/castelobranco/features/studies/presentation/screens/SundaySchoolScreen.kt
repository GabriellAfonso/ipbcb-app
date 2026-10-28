package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.GreenTab
import com.ipb.castelobranco.core.presentation.components.GreenTabBar
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.studies.domain.model.LessonSummary
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolYear
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.DEFAULT_FONT_SIZE
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineErrorView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineLoadingView
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.LessonStyle
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.lessonMeta
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.sundaySchoolColors
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolUiState
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolViewModel
import java.time.LocalDate

@Composable
fun SundaySchoolScreen(
    viewModel: SundaySchoolViewModel,
    onBackClick: () -> Unit,
    onOpenLesson: (LessonSummary) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BaseScreen(
        tabName = "Escola Bíblica Dominical",
        showBackArrow = true,
        onBackClick = onBackClick,
        // The green behind the sheet's rounded corners continues the top bar (R-13).
        containerColor = ipbGreen,
        topBarExtension = if (state.years.size > 1) {
            { YearTabs(state, onSelectYear = viewModel::selectYear) }
        } else {
            null
        },
    ) { innerPadding ->
        SundaySchoolContent(
            state = state,
            onOpenLesson = onOpenLesson,
            onRetry = viewModel::retry,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
private fun YearTabs(state: SundaySchoolUiState, onSelectYear: (Int) -> Unit) {
    val selectedIndex = state.years.indexOfFirst { it.year == state.selectedYear }.coerceAtLeast(0)
    GreenTabBar(
        tabs = state.years.map { GreenTab(it.year.toString()) },
        selectedIndex = selectedIndex,
        onSelect = { onSelectYear(state.years[it].year) },
    )
}

@Composable
fun SundaySchoolContent(
    state: SundaySchoolUiState,
    onOpenLesson: (LessonSummary) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = sundaySchoolColors
    Surface(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(topStart = SHEET_RADIUS, topEnd = SHEET_RADIUS)),
        color = colors.page,
        contentColor = colors.text,
    ) {
        when {
            state.lessons.isNotEmpty() -> LessonList(state, onOpenLesson)
            state.isEmpty -> EmptyLessons()
            state.isLoading -> DoctrineLoadingView()
            state.error != null -> DoctrineErrorView(message = state.error, onRetry = onRetry)
            else -> EmptyLessons()
        }
    }
}

@Composable
private fun LessonList(state: SundaySchoolUiState, onOpenLesson: (LessonSummary) -> Unit) {
    val style = LessonStyle(colors = sundaySchoolColors, fontSize = LIST_FONT_SIZE)
    LazyColumn(
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = HEADER_KEY) { YearHeader(state, style) }
        itemsIndexed(state.lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
            LessonCard(number = index + 1, lesson = lesson, style = style, onClick = { onOpenLesson(lesson) })
        }
    }
}

/** "ANO 2026", the theme and the lesson count, on parchment (R-13). */
@Composable
private fun YearHeader(state: SundaySchoolUiState, style: LessonStyle) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(style.colors.parchment)
            .border(1.dp, style.colors.parchmentEdge, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "ANO ${state.selectedYear ?: ""}",
            style = style.label.copy(fontSize = 11.sp, letterSpacing = 0.12.em, fontWeight = FontWeight.Bold),
        )
        if (state.yearTheme.isNotBlank()) {
            Text(text = state.yearTheme, style = style.title.copy(fontSize = 29.sp, lineHeight = 32.sp))
        }
        Text(
            text = state.lessonCountLabel,
            style = style.small.copy(fontSize = 13.sp, fontStyle = FontStyle.Italic, color = style.colors.muted),
        )
    }
}

/** One lesson: its number in the year in a gold ring, title, date · teacher and a chevron (R-13a). */
@Composable
private fun LessonCard(number: Int, lesson: LessonSummary, style: LessonStyle, onClick: () -> Unit) {
    val isDark = style.colors.page.luminance() < DARK_LUMINANCE
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (isDark) CARD_DARK else CARD_LIGHT,
        border = BorderStroke(1.dp, style.colors.rule),
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .border(1.5.dp, style.colors.gold, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number.toString(),
                    style = style.title.copy(
                        fontSize = 20.sp,
                        lineHeight = 22.sp,
                        color = if (isDark) style.colors.gold else GOLD_INK_LIGHT,
                    ),
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(text = lesson.title, style = style.title.copy(fontSize = 19.sp, lineHeight = 23.sp))
                lessonMeta(lesson.date, lesson.teacher)?.let {
                    Text(text = it, style = style.small.copy(fontSize = 13.sp, color = style.colors.muted))
                }
            }
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = style.colors.muted,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun EmptyLessons() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(
            text = "Nenhuma aula publicada ainda",
            style = MaterialTheme.typography.bodyLarge,
            color = sundaySchoolColors.muted,
            textAlign = TextAlign.Center,
        )
    }
}

private const val LIST_FONT_SIZE = DEFAULT_FONT_SIZE
private const val HEADER_KEY = "year-header"
private val SHEET_RADIUS = 18.dp
private const val DARK_LUMINANCE = 0.5f

// Card a shade lighter than the page in both themes, and a darker gold for the number on cream: the reader's gold
// (#A8781E) is below 4.5:1 on the light card.
private val CARD_LIGHT = Color(0xFFFFFDF7)
private val CARD_DARK = Color(0xFF251D16)
private val GOLD_INK_LIGHT = Color(0xFF6E4F12)

@Preview(showBackground = true)
@Composable
private fun SundaySchoolContentPreview() {
    MaterialTheme {
        SundaySchoolContent(
            state = SundaySchoolUiState(
                years = listOf(SundaySchoolYear(2026, "O Drama das Escrituras")),
                selectedYear = 2026,
                yearLabel = "2026 — O Drama das Escrituras",
                yearTheme = "O Drama das Escrituras",
                lessons = listOf(
                    LessonSummary(1, "Josué", null, "", 0),
                    LessonSummary(3, "2 Samuel", LocalDate.of(2026, 5, 17), "", 2),
                    LessonSummary(11, "Daniel", LocalDate.of(2026, 8, 16), "", 10),
                ),
                isLoading = false,
            ),
            onOpenLesson = {},
            onRetry = {},
        )
    }
}
