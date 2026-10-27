package com.ipb.castelobranco.features.studies.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.studies.domain.model.BibleReference
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineErrorView
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineFontSizeBar
import com.ipb.castelobranco.features.studies.presentation.components.DoctrineLoadingView
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.LessonContentView
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.LessonHeaderInfo
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.LessonStyle
import com.ipb.castelobranco.features.studies.presentation.components.sundayschool.sundaySchoolColors
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolLessonUiState
import com.ipb.castelobranco.features.studies.presentation.viewmodel.SundaySchoolLessonViewModel

@Composable
fun SundaySchoolLessonScreen(
    viewModel: SundaySchoolLessonViewModel,
    onBackClick: () -> Unit,
    onReferenceClick: (BibleReference) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BaseScreen(
        tabName = "EBD",
        showBackArrow = true,
        onBackClick = onBackClick,
        extraActions = {
            if (state.lesson != null) {
                IconButton(onClick = viewModel::toggleFontBar) {
                    Text(text = "Aa", fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 19.sp)
                }
            }
        },
        topBarExtension = if (state.showFontBar) {
            {
                DoctrineFontSizeBar(
                    fontSize = state.fontSize.size,
                    canDecrease = state.fontSize.canDecrease,
                    canIncrease = state.fontSize.canIncrease,
                    onDecrease = viewModel::decreaseFont,
                    onIncrease = viewModel::increaseFont,
                )
            }
        } else {
            null
        },
    ) { innerPadding ->
        SundaySchoolLessonContent(
            state = state,
            onReferenceClick = onReferenceClick,
            onRetry = viewModel::retry,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Composable
fun SundaySchoolLessonContent(
    state: SundaySchoolLessonUiState,
    onReferenceClick: (BibleReference) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = sundaySchoolColors
    Surface(modifier = modifier.fillMaxSize(), color = colors.page, contentColor = colors.text) {
        val lesson = state.lesson
        when {
            lesson != null -> LessonContentView(
                header = LessonHeaderInfo(
                    title = lesson.title,
                    yearLabel = lesson.year.label,
                    date = lesson.date,
                    teacher = lesson.teacher,
                ),
                content = lesson.content,
                books = lesson.books,
                style = LessonStyle(colors = colors, fontSize = state.fontSize.size),
                onReferenceClick = onReferenceClick,
                modifier = Modifier.fillMaxSize(),
            )
            state.isLoading -> DoctrineLoadingView()
            state.error != null -> Column { DoctrineErrorView(message = state.error, onRetry = onRetry) }
            else -> DoctrineLoadingView()
        }
    }
}
