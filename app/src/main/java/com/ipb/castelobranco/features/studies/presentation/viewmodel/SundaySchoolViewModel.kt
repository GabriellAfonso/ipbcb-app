package com.ipb.castelobranco.features.studies.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.studies.domain.model.LessonSummary
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolIndex
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolYear
import com.ipb.castelobranco.features.studies.domain.usecase.ObserveSundaySchoolIndexUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SundaySchoolUiState(
    val years: List<SundaySchoolYear> = emptyList(),
    val selectedYear: Int? = null,
    /** "2026 — O Drama das Escrituras" for the header above the list. */
    val yearLabel: String = "",
    val lessons: List<LessonSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
) {
    /** No published lesson at all. */
    val isEmpty: Boolean get() = !isLoading && error == null && years.isEmpty()
}

/**
 * The EBD screen: the default index gives the tabs and the opening year; another tab reads its own snapshot
 * (`specs/020-sunday-school/research.md` R-03, R-13).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SundaySchoolViewModel @Inject constructor(
    private val observeIndex: ObserveSundaySchoolIndexUseCase,
) : ViewModel() {

    /** Null = the server's default year. */
    private val selectedYear = MutableStateFlow<Int?>(null)

    private val defaultIndex = observeIndex(null)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SnapshotState.Loading)

    private val yearIndex = combine(selectedYear, defaultIndex) { year, default ->
        year.takeUnless { it == null || it == default.dataOrNull()?.defaultYear }
    }.flatMapLatest { year -> if (year == null) flowOf(null) else observeIndex(year) }

    val uiState: StateFlow<SundaySchoolUiState> = combine(
        defaultIndex,
        yearIndex,
        selectedYear,
    ) { default, other, year ->
        val base = SundaySchoolUiState(selectedYear = year)
        val defaultData = default.dataOrNull()
        val years = defaultData?.years.orEmpty()
        val shown = other ?: default
        when (shown) {
            SnapshotState.Loading -> base.copy(years = years, isLoading = true)
            is SnapshotState.Error -> base.copy(
                years = years,
                isLoading = false,
                error = shown.error.toUserMessage(),
            )
            is SnapshotState.Data -> {
                val index = shown.value
                base.copy(
                    years = years.ifEmpty { index.years },
                    selectedYear = index.year?.year ?: year,
                    yearLabel = index.year?.label.orEmpty(),
                    lessons = index.lessons,
                    isLoading = false,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SundaySchoolUiState())

    init {
        viewModelScope.launch { observeIndex.load(null) }
    }

    fun selectYear(year: Int) {
        val default = defaultIndex.value.dataOrNull()?.defaultYear
        selectedYear.value = if (year == default) null else year
        if (year != default) viewModelScope.launch { observeIndex.load(year) }
    }

    fun retry() {
        val year = selectedYear.value
        viewModelScope.launch {
            observeIndex.refresh(null)
            if (year != null) observeIndex.refresh(year)
        }
    }

    private fun SnapshotState<SundaySchoolIndex>.dataOrNull(): SundaySchoolIndex? =
        (this as? SnapshotState.Data)?.value
}
