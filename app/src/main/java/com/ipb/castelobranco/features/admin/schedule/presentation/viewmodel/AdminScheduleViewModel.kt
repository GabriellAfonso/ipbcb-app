package com.ipb.castelobranco.features.admin.schedule.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.admin.schedule.domain.model.Member
import com.ipb.castelobranco.features.admin.schedule.domain.model.ScheduleItem
import com.ipb.castelobranco.features.admin.schedule.domain.repository.AdminScheduleRepository
import com.ipb.castelobranco.features.admin.schedule.presentation.state.AdminScheduleEvent
import com.ipb.castelobranco.features.admin.schedule.presentation.state.AdminScheduleSkeleton
import com.ipb.castelobranco.features.admin.schedule.presentation.state.AdminScheduleUiState
import com.ipb.castelobranco.features.admin.schedule.presentation.state.EditableScheduleUiState
import com.ipb.castelobranco.features.admin.schedule.presentation.state.SaveResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AdminScheduleViewModel @Inject constructor(
    private val repository: AdminScheduleRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AdminScheduleUiState().let { initial ->
            initial.copy(items = AdminScheduleSkeleton.build(initial.year, initial.month))
        }
    )
    val uiState: StateFlow<AdminScheduleUiState> = _uiState.asStateFlow()

    fun onEvent(event: AdminScheduleEvent) {
        when (event) {
            AdminScheduleEvent.LoadMembers -> loadMembers()
            is AdminScheduleEvent.MonthChanged -> changeMonth(event.year, event.month)
            is AdminScheduleEvent.MemberSelected -> selectMember(event.itemIndex, event.member)
            AdminScheduleEvent.GenerateSchedule -> generateSchedule()
            AdminScheduleEvent.SaveRequested -> _uiState.update {
                it.copy(showSaveConfirmation = it.canSave && !it.isSaving)
            }
            AdminScheduleEvent.SaveConfirmationDismissed -> _uiState.update { it.copy(showSaveConfirmation = false) }
            AdminScheduleEvent.SaveSchedule -> saveSchedule()
            AdminScheduleEvent.SaveResultDismissed -> _uiState.update { it.copy(saveResult = null) }
            AdminScheduleEvent.SnackbarShown -> _uiState.update { it.copy(snackbarMessage = null) }
        }
    }

    private fun loadMembers() {
        if (_uiState.value.members.isNotEmpty() || _uiState.value.isLoadingMembers) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMembers = true) }
            repository.getMembers()
                .onSuccess { members ->
                    _uiState.update { it.copy(members = members, isLoadingMembers = false) }
                }
                .onFailure { e ->
                    Timber.w(e, "Failed to load members")
                    _uiState.update {
                        it.copy(
                            isLoadingMembers = false,
                            snackbarMessage = "Falha ao carregar membros."
                        )
                    }
                }
        }
    }

    private fun changeMonth(year: Int, month: Int) {
        _uiState.update {
            it.copy(
                year = year,
                month = month,
                items = AdminScheduleSkeleton.build(year, month),
                hasUnsavedChanges = false
            )
        }
    }

    private fun selectMember(index: Int, member: Member) {
        _uiState.update { state ->
            state.copy(
                items = state.items.mapIndexed { i, item ->
                    if (i == index) item.copy(selectedMember = member) else item
                },
                hasUnsavedChanges = true
            )
        }
    }

    private fun generateSchedule() {
        val state = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isGenerating = true) }
            repository.generateSchedule(year = state.year, month = state.month)
                .onSuccess { items ->
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            hasUnsavedChanges = true,
                            items = items.map { item ->
                                EditableScheduleUiState(
                                    date = item.date,
                                    day = item.day,
                                    scheduleTypeName = item.scheduleTypeName,
                                    scheduleTypeId = item.scheduleTypeId,
                                    selectedMember = item.selectedMember
                                )
                            }
                        )
                    }
                }
                .onFailure { e ->
                    Timber.w(e, "Failed to generate schedule")
                    _uiState.update {
                        it.copy(
                            isGenerating = false,
                            snackbarMessage = "Falha ao gerar escala."
                        )
                    }
                }
        }
    }

    private fun saveSchedule() {
        _uiState.update { it.copy(showSaveConfirmation = false) }
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val domainItems = state.items.map { item ->
                ScheduleItem(
                    date = item.date,
                    day = item.day,
                    scheduleTypeName = item.scheduleTypeName,
                    scheduleTypeId = item.scheduleTypeId,
                    selectedMember = item.selectedMember
                )
            }
            repository.saveSchedule(year = state.year, month = state.month, items = domainItems)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            hasUnsavedChanges = false,
                            saveResult = SaveResult.Success
                        )
                    }
                }
                .onFailure { error ->
                    Timber.e(error, "Failed to save schedule")
                    val message = error.toAppError().toUserMessage()
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            saveResult = SaveResult.Error(message)
                        )
                    }
                }
        }
    }
}
