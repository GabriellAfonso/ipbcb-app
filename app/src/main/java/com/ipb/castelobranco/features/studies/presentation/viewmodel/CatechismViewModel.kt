package com.ipb.castelobranco.features.studies.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.di.DefaultDispatcher
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.studies.domain.model.CatechismQuestion
import com.ipb.castelobranco.features.studies.domain.model.DoctrineDocument
import com.ipb.castelobranco.features.studies.domain.model.DoctrineSearchResult
import com.ipb.castelobranco.features.studies.domain.model.doctrineShortTitle
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.DEFAULT_FONT_SIZE
import com.ipb.castelobranco.features.studies.domain.usecase.ObserveDoctrineDocumentUseCase
import com.ipb.castelobranco.features.studies.domain.usecase.SearchDoctrineUseCase
import com.ipb.castelobranco.features.studies.presentation.navigation.StudiesRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CatechismUiState(
    /** Short title for the top bar ("Breve Catecismo"). */
    val title: String = "",
    val questions: List<CatechismQuestion> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val fontSize: DoctrineFontSize = DoctrineFontSize(DEFAULT_FONT_SIZE),
    val isSearching: Boolean = false,
    val query: String = "",
    /** Null while the term is blank: the reader stays visible under the search field. */
    val results: List<DoctrineSearchResult>? = null,
)

sealed interface CatechismEvent {
    data class ScrollTo(val index: Int) : CatechismEvent
    data class ShowMessage(val text: String) : CatechismEvent
}

@OptIn(FlowPreview::class)
@HiltViewModel
class CatechismViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeDocument: ObserveDoctrineDocumentUseCase,
    private val search: SearchDoctrineUseCase,
    private val preferences: DoctrinePreferencesRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val slug: String = checkNotNull(savedStateHandle[StudiesRoutes.ARG_SLUG])

    private val document = observeDocument(slug)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SnapshotState.Loading)

    private val isSearching = MutableStateFlow(false)
    private val query = MutableStateFlow("")

    private val results = combine(
        document,
        query.debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS },
    ) { state, term ->
        val catechism = (state as? SnapshotState.Data)?.value as? DoctrineDocument.Catechism
        if (catechism == null || term.isBlank()) null else search(catechism, term)
    }.flowOn(defaultDispatcher)

    private val fontSize = preferences.fontSizeFlow.map { DoctrineFontSize(it) }

    val uiState: StateFlow<CatechismUiState> = combine(
        document, fontSize, isSearching, query, results,
    ) { state, font, searching, term, found ->
        val base = CatechismUiState(fontSize = font, isSearching = searching, query = term, results = found)
        when (state) {
            SnapshotState.Loading -> base.copy(isLoading = true)
            is SnapshotState.Error -> base.copy(isLoading = false, error = state.error.toUserMessage())
            is SnapshotState.Data -> {
                val catechism = state.value as? DoctrineDocument.Catechism
                base.copy(
                    title = doctrineShortTitle(slug, state.value.title),
                    questions = catechism?.questions.orEmpty(),
                    isLoading = false,
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, CatechismUiState())

    private val _events = MutableSharedFlow<CatechismEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<CatechismEvent> = _events.asSharedFlow()

    init {
        viewModelScope.launch { observeDocument.load(slug) }
    }

    fun retry() {
        viewModelScope.launch { observeDocument.refresh(slug) }
    }

    /** "Ir para nº…": a number in range scrolls to that question; anything else explains the range. */
    fun goTo(input: String) {
        val questions = uiState.value.questions
        if (questions.isEmpty()) return
        val number = input.trim().toIntOrNull()
        val index = questions.indexOfFirst { it.number == number }
        if (index < 0) {
            val range = "${questions.first().number} a ${questions.last().number}"
            _events.tryEmit(CatechismEvent.ShowMessage("Digite um número de $range"))
            return
        }
        _events.tryEmit(CatechismEvent.ScrollTo(index))
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

    fun onResultClick(result: DoctrineSearchResult) {
        val number = (result as? DoctrineSearchResult.Question)?.number ?: return
        closeSearch()
        val index = uiState.value.questions.indexOfFirst { it.number == number }
        if (index >= 0) _events.tryEmit(CatechismEvent.ScrollTo(index))
    }

    fun increaseFont() {
        viewModelScope.launch { preferences.setFontSize(uiState.value.fontSize.increased()) }
    }

    fun decreaseFont() {
        viewModelScope.launch { preferences.setFontSize(uiState.value.fontSize.decreased()) }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 200L
    }
}
