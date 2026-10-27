package com.ipb.castelobranco.features.studies.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.studies.domain.model.Lesson
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.DEFAULT_FONT_SIZE
import com.ipb.castelobranco.features.studies.domain.usecase.ObserveSundaySchoolLessonUseCase
import com.ipb.castelobranco.features.studies.presentation.navigation.StudiesRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SundaySchoolLessonUiState(
    val lesson: Lesson? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    /** Shared with the doctrine readers: one reading size for Estudos (research R-04). */
    val fontSize: DoctrineFontSize = DoctrineFontSize(DEFAULT_FONT_SIZE),
    val showFontBar: Boolean = false,
)

@HiltViewModel
class SundaySchoolLessonViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeLesson: ObserveSundaySchoolLessonUseCase,
    private val preferences: DoctrinePreferencesRepository,
) : ViewModel() {

    private val lessonId: Int = checkNotNull(savedStateHandle[StudiesRoutes.ARG_LESSON_ID])

    private val showFontBar = MutableStateFlow(false)

    val uiState: StateFlow<SundaySchoolLessonUiState> = combine(
        observeLesson(lessonId),
        preferences.fontSizeFlow.map { DoctrineFontSize(it) },
        showFontBar,
    ) { state, font, fontBar ->
        val base = SundaySchoolLessonUiState(fontSize = font, showFontBar = fontBar)
        when (state) {
            SnapshotState.Loading -> base
            is SnapshotState.Error -> base.copy(isLoading = false, error = state.error.toUserMessage())
            is SnapshotState.Data -> base.copy(lesson = state.value, isLoading = false)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SundaySchoolLessonUiState())

    init {
        viewModelScope.launch { observeLesson.load(lessonId) }
    }

    fun retry() {
        viewModelScope.launch { observeLesson.refresh(lessonId) }
    }

    fun toggleFontBar() {
        showFontBar.value = !showFontBar.value
    }

    fun increaseFont() {
        viewModelScope.launch { preferences.setFontSize(uiState.value.fontSize.increased()) }
    }

    fun decreaseFont() {
        viewModelScope.launch { preferences.setFontSize(uiState.value.fontSize.decreased()) }
    }
}
