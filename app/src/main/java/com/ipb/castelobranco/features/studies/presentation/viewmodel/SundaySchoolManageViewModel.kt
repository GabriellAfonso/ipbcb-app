package com.ipb.castelobranco.features.studies.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.studies.domain.model.LessonSummary
import com.ipb.castelobranco.features.studies.domain.model.ReorderResult
import com.ipb.castelobranco.features.studies.domain.usecase.DeleteSundaySchoolLessonUseCase
import com.ipb.castelobranco.features.studies.domain.usecase.ListSundaySchoolLessonsUseCase
import com.ipb.castelobranco.features.studies.domain.usecase.ListSundaySchoolYearsUseCase
import com.ipb.castelobranco.features.studies.domain.usecase.ReorderSundaySchoolLessonsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SundaySchoolManageUiState(
    /** Years with lessons, newest first, plus the selected one when it has none yet. */
    val years: List<Int> = emptyList(),
    val selectedYear: Int? = null,
    val lessons: List<LessonSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val canManage: Boolean = false,
    val canDelete: Boolean = false,
    /** Lesson waiting for the delete confirmation. */
    val pendingDelete: LessonSummary? = null,
    val isSavingOrder: Boolean = false,
)

sealed interface SundaySchoolManageEvent {
    data class ShowMessage(val text: String) : SundaySchoolManageEvent
}

@HiltViewModel
class SundaySchoolManageViewModel @Inject constructor(
    observeAccess: ObserveAccessUseCase,
    private val listYears: ListSundaySchoolYearsUseCase,
    private val listLessons: ListSundaySchoolLessonsUseCase,
    private val reorderLessons: ReorderSundaySchoolLessonsUseCase,
    private val deleteLesson: DeleteSundaySchoolLessonUseCase,
    private val dateProvider: DateProvider,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SundaySchoolManageUiState())
    val uiState: StateFlow<SundaySchoolManageUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<SundaySchoolManageEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<SundaySchoolManageEvent> = _events.asSharedFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            observeAccess().collect { access ->
                _uiState.update {
                    it.copy(
                        canManage = access.allows(Scope.SUNDAY_SCHOOL, AccessLevel.MANAGE),
                        canDelete = access.allows(Scope.SUNDAY_SCHOOL, AccessLevel.OWNER),
                    )
                }
            }
        }
    }

    /** Called whenever the screen is shown, so a lesson saved in the form appears on return. */
    fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = it.lessons.isEmpty(), error = null) }
            listYears().fold(
                onSuccess = { years ->
                    val withLessons = years.map { it.year }
                    val selected = _uiState.value.selectedYear
                        ?: withLessons.firstOrNull()
                        ?: dateProvider.today().year
                    _uiState.update { it.copy(years = (withLessons + selected).distinct().sortedDescending()) }
                    loadLessons(selected)
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.toAppError().toUserMessage()) }
                },
            )
        }
    }

    fun selectYear(year: Int) {
        if (year == _uiState.value.selectedYear) return
        _uiState.update { it.copy(selectedYear = year, lessons = emptyList(), isLoading = true, error = null) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadLessons(year) }
    }

    private suspend fun loadLessons(year: Int) {
        listLessons(year).fold(
            onSuccess = { lessons ->
                _uiState.update { it.copy(selectedYear = year, lessons = lessons, isLoading = false, error = null) }
            },
            onFailure = { error ->
                val message = error.toAppError().toUserMessage()
                _uiState.update { it.copy(selectedYear = year, isLoading = false, error = message) }
            },
        )
    }

    /** While dragging: the list moves on screen only. */
    fun move(from: Int, to: Int) {
        _uiState.update { state ->
            val lessons = state.lessons.toMutableList()
            if (from !in lessons.indices || to !in lessons.indices) return@update state
            lessons.add(to, lessons.removeAt(from))
            state.copy(lessons = lessons)
        }
    }

    /** On drop: the whole order of the year is saved (FR-022). */
    fun saveOrder() {
        val state = _uiState.value
        val year = state.selectedYear ?: return
        val ids = state.lessons.map { it.id }
        _uiState.update { it.copy(isSavingOrder = true) }
        viewModelScope.launch {
            val message = when (val result = reorderLessons(year, ids)) {
                ReorderResult.Saved -> null
                ReorderResult.Stale -> ORDER_STALE
                is ReorderResult.Failed -> result.error.toUserMessage()
            }
            _uiState.update { it.copy(isSavingOrder = false) }
            if (message != null) {
                _events.tryEmit(SundaySchoolManageEvent.ShowMessage(message))
                loadLessons(year)
            }
        }
    }

    fun askDelete(lesson: LessonSummary) {
        _uiState.update { it.copy(pendingDelete = lesson) }
    }

    fun dismissDelete() {
        _uiState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val lesson = _uiState.value.pendingDelete ?: return
        _uiState.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            deleteLesson(lesson.id).fold(
                onSuccess = { _events.tryEmit(SundaySchoolManageEvent.ShowMessage("Aula apagada")) },
                onFailure = { _events.tryEmit(SundaySchoolManageEvent.ShowMessage(it.toAppError().toUserMessage())) },
            )
            reload()
        }
    }

    companion object {
        const val ORDER_STALE = "A lista mudou; tente de novo."
    }
}
