package com.ipb.castelobranco.features.studies.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSummary
import com.ipb.castelobranco.features.studies.domain.usecase.ObserveDoctrineListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DoctrineListUiState(
    val documents: List<DoctrineSummary> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
)

@HiltViewModel
class DoctrineListViewModel @Inject constructor(
    private val observeList: ObserveDoctrineListUseCase,
) : ViewModel() {

    val uiState: StateFlow<DoctrineListUiState> = observeList()
        .map { state ->
            when (state) {
                SnapshotState.Loading -> DoctrineListUiState(isLoading = true)
                is SnapshotState.Data -> DoctrineListUiState(documents = state.value, isLoading = false)
                is SnapshotState.Error -> DoctrineListUiState(isLoading = false, error = state.error.toUserMessage())
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, DoctrineListUiState())

    init {
        viewModelScope.launch { observeList.load() }
    }

    fun retry() {
        viewModelScope.launch { observeList.refresh() }
    }
}
