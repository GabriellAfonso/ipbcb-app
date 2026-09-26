package com.ipb.castelobranco.features.admin.members.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.features.admin.members.domain.usecase.GetMemberHistoryUseCase
import com.ipb.castelobranco.features.admin.members.presentation.navigation.MembersRoutes
import com.ipb.castelobranco.features.admin.members.presentation.state.HistoryLineUi
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberHistoryUiState
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.presentation.util.formatDateTime
import com.ipb.castelobranco.features.admin.members.presentation.util.toMembersEvent
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

/** Who changed what and when, newest first, already as Portuguese sentences. */
@HiltViewModel
class MemberHistoryViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getHistory: GetMemberHistoryUseCase,
) : ViewModel() {

    private val memberId: Int = checkNotNull(savedStateHandle[MembersRoutes.ARG_MEMBER_ID])

    private val _uiState = MutableStateFlow(MemberHistoryUiState())
    val uiState: StateFlow<MemberHistoryUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<MembersEvent>()
    val events: SharedFlow<MembersEvent> = _events.asSharedFlow()

    init {
        load()
    }

    fun load() {
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            getHistory(memberId)
                .onSuccess { lines ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            lines = lines.map { line ->
                                HistoryLineUi(line.editorName, line.text, formatDateTime(line.changedAt))
                            },
                        )
                    }
                }
                .onFailure { throwable ->
                    val event = throwable.toMembersEvent()
                    _uiState.update {
                        it.copy(isLoading = false, error = (event as? MembersEvent.ShowMessage)?.message)
                    }
                    if (event !is MembersEvent.ShowMessage) _events.emit(event)
                }
        }
    }
}
