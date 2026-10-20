package com.ipb.castelobranco.features.studies.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.di.DefaultDispatcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.studies.domain.model.ConfessionChapter
import com.ipb.castelobranco.features.studies.domain.model.DoctrineDocument
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSearchResult
import com.ipb.castelobranco.features.studies.domain.usecase.ObserveDoctrineDocumentUseCase
import com.ipb.castelobranco.features.studies.domain.usecase.SearchDoctrineUseCase
import com.ipb.castelobranco.features.studies.presentation.navigation.StudiesRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfessionUiState(
    val title: String = "",
    val chapters: List<ConfessionChapter> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val isSearching: Boolean = false,
    val query: String = "",
    val results: List<DoctrineSearchResult>? = null,
)

/** Chapter list of the Confession, with search across every paragraph. */
@OptIn(FlowPreview::class)
@HiltViewModel
class ConfessionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeDocument: ObserveDoctrineDocumentUseCase,
    private val search: SearchDoctrineUseCase,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    val slug: String = checkNotNull(savedStateHandle[StudiesRoutes.ARG_SLUG])

    private val document = observeDocument(slug)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SnapshotState.Loading)

    private val isSearching = MutableStateFlow(false)
    private val query = MutableStateFlow("")

    private val results = combine(
        document,
        query.debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS },
    ) { state, term ->
        val confession = (state as? SnapshotState.Data)?.value as? DoctrineDocument.Confession
        if (confession == null || term.isBlank()) null else search(confession, term)
    }.flowOn(defaultDispatcher)

    val uiState: StateFlow<ConfessionUiState> = combine(
        document, isSearching, query, results,
    ) { state, searching, term, found ->
        val base = ConfessionUiState(isSearching = searching, query = term, results = found)
        when (state) {
            SnapshotState.Loading -> base.copy(isLoading = true)
            is SnapshotState.Error -> base.copy(isLoading = false, error = state.error.toUserMessage())
            is SnapshotState.Data -> base.copy(
                title = state.value.title,
                chapters = (state.value as? DoctrineDocument.Confession)?.chapters.orEmpty(),
                isLoading = false,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConfessionUiState())

    init {
        viewModelScope.launch { observeDocument.load(slug) }
    }

    fun retry() {
        viewModelScope.launch { observeDocument.refresh(slug) }
    }

    fun openSearch() {
        isSearching.value = true
    }

    fun closeSearch() {
        isSearching.value = false
        query.value = ""
    }

    fun onQueryChange(term: String) {
        query.value = term
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 200L
    }
}
