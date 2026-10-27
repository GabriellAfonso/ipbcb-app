package com.ipb.castelobranco.features.studies.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolYearAdmin
import com.ipb.castelobranco.features.studies.domain.usecase.ListSundaySchoolYearsUseCase
import com.ipb.castelobranco.features.studies.domain.usecase.SetSundaySchoolThemeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SundaySchoolYearsUiState(
    val years: List<SundaySchoolYearAdmin> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val canManage: Boolean = false,
    /** Year whose theme dialog is open. */
    val editing: SundaySchoolYearAdmin? = null,
    val isSaving: Boolean = false,
)

/** "Anos da EBD": every year with lessons and its theme. Years are never created here (FR-024). */
@HiltViewModel
class SundaySchoolYearsViewModel @Inject constructor(
    observeAccess: ObserveAccessUseCase,
    private val listYears: ListSundaySchoolYearsUseCase,
    private val setTheme: SetSundaySchoolThemeUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SundaySchoolYearsUiState())
    val uiState: StateFlow<SundaySchoolYearsUiState> = _uiState.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    init {
        viewModelScope.launch {
            observeAccess().collect { access ->
                _uiState.update { it.copy(canManage = access.allows(Scope.SUNDAY_SCHOOL, AccessLevel.MANAGE)) }
            }
        }
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            listYears().fold(
                onSuccess = { years -> _uiState.update { it.copy(years = years, isLoading = false) } },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, error = error.toAppError().toUserMessage()) }
                },
            )
        }
    }

    fun edit(year: SundaySchoolYearAdmin) {
        if (_uiState.value.canManage) _uiState.update { it.copy(editing = year) }
    }

    fun dismissEdit() {
        _uiState.update { it.copy(editing = null) }
    }

    /** A blank theme removes it. */
    fun saveTheme(theme: String) {
        val year = _uiState.value.editing ?: return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            setTheme(year.year, theme).fold(
                onSuccess = { updated ->
                    _uiState.update { state ->
                        state.copy(
                            years = state.years.map { if (it.year == updated.year) updated else it },
                            editing = null,
                            isSaving = false,
                        )
                    }
                    _messages.tryEmit(if (updated.theme.isBlank()) "Tema removido" else "Tema salvo")
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isSaving = false) }
                    _messages.tryEmit(error.toAppError().toUserMessage())
                },
            )
        }
    }
}
