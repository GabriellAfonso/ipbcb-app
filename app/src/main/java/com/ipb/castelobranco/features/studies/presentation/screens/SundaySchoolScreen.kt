package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
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
    BaseScreen(tabName = "Escola Bíblica Dominical", showBackArrow = true, onBackClick = onBackClick) { innerPadding ->
        SundaySchoolContent(
            state = state,
            onSelectYear = viewModel::selectYear,
            onOpenLesson = onOpenLesson,
            onRetry = viewModel::retry,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun SundaySchoolContent(
    state: SundaySchoolUiState,
    onSelectYear: (Int) -> Unit,
    onOpenLesson: (LessonSummary) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = sundaySchoolColors
    Surface(modifier = modifier.fillMaxSize(), color = colors.page, contentColor = colors.text) {
        Column {
            if (state.years.size > 1) {
                val selectedIndex = state.years.indexOfFirst { it.year == state.selectedYear }.coerceAtLeast(0)
                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedIndex,
                    containerColor = colors.page,
                    contentColor = colors.text,
                    edgePadding = 16.dp,
                ) {
                    state.years.forEachIndexed { index, year ->
                        Tab(
                            selected = index == selectedIndex,
                            onClick = { onSelectYear(year.year) },
                            text = { Text(year.year.toString()) },
                        )
                    }
                }
            }
            when {
                state.lessons.isNotEmpty() -> LessonList(state, onOpenLesson)
                state.isEmpty -> EmptyLessons()
                state.isLoading -> DoctrineLoadingView()
                state.error != null -> DoctrineErrorView(message = state.error, onRetry = onRetry)
                else -> EmptyLessons()
            }
        }
    }
}

@Composable
private fun LessonList(state: SundaySchoolUiState, onOpenLesson: (LessonSummary) -> Unit) {
    val style = LessonStyle(colors = sundaySchoolColors, fontSize = LIST_FONT_SIZE)
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        if (state.yearLabel.isNotBlank()) {
            item {
                Text(
                    text = state.yearLabel,
                    style = style.title,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 8.dp),
                )
            }
        }
        items(state.lessons, key = { it.id }) { lesson ->
            Column(
                modifier = Modifier
                    .padding(horizontal = 4.dp),
            ) {
                Surface(onClick = { onOpenLesson(lesson) }, color = style.colors.page) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(text = lesson.title, style = style.cardTitle)
                        lessonMeta(lesson.date, lesson.teacher)?.let {
                            Text(text = it, style = style.small.copy(color = style.colors.muted))
                        }
                    }
                }
                HorizontalDivider(color = style.colors.rule, modifier = Modifier.padding(horizontal = 16.dp))
            }
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

@Preview(showBackground = true)
@Composable
private fun SundaySchoolContentPreview() {
    MaterialTheme {
        SundaySchoolContent(
            state = SundaySchoolUiState(
                years = listOf(SundaySchoolYear(2027, ""), SundaySchoolYear(2026, "O Drama das Escrituras")),
                selectedYear = 2026,
                yearLabel = "2026 — O Drama das Escrituras",
                lessons = listOf(
                    LessonSummary(1, "Panorama de Josué", null, "", 0),
                    LessonSummary(11, "Panorama do Livro de Daniel", LocalDate.of(2026, 8, 16), "", 10),
                ),
                isLoading = false,
            ),
            onSelectYear = {},
            onOpenLesson = {},
            onRetry = {},
        )
    }
}
