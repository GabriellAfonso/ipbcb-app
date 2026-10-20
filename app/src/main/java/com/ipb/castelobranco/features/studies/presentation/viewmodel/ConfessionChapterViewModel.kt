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
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository
import com.ipb.castelobranco.features.studies.domain.repository.DoctrinePreferencesRepository.Companion.DEFAULT_FONT_SIZE
import com.ipb.castelobranco.features.studies.domain.usecase.ObserveDoctrineDocumentUseCase
import com.ipb.castelobranco.features.studies.domain.usecase.SearchDoctrineUseCase
import com.ipb.castelobranco.features.studies.presentation.navigation.StudiesRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ConfessionChapterUiState(
    val documentTitle: String = "",
    val chapter: ConfessionChapter? = null,
    val hasPrevious: Boolean = false,
    val hasNext: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val fontSize: DoctrineFontSize = DoctrineFontSize(DEFAULT_FONT_SIZE),
    val isSearching: Boolean = false,
    val query: String = "",
    val results: List<DoctrineSearchResult>? = null,
)

/** Paragraph index inside the current chapter's list to bring to the top. */
data class ScrollToParagraph(val index: Int)

/** One chapter of the Confession; previous/next change the chapter in place. */
@OptIn(FlowPreview::class)
@HiltViewModel
class ConfessionChapterViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val observeDocument: ObserveDoctrineDocumentUseCase,
    private val search: SearchDoctrineUseCase,
    private val preferences: DoctrinePreferencesRepository,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val slug: String = checkNotNull(savedStateHandle[StudiesRoutes.ARG_SLUG])
    private val initialParagraph: Int = savedStateHandle[StudiesRoutes.ARG_PARAGRAPH] ?: 0

    private val document = observeDocument(slug)
        .stateIn(viewModelScope, SharingStarted.Eagerly, SnapshotState.Loading)

    private val chapterNumber = MutableStateFlow(savedStateHandle[StudiesRoutes.ARG_CHAPTER] ?: 1)
    private val isSearching = MutableStateFlow(false)
    private val query = MutableStateFlow("")

    private val results = combine(
        document,
        query.debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS },
    ) { state, term ->
        val confession = (state as? SnapshotState.Data)?.value as? DoctrineDocument.Confession
        if (confession == null || term.isBlank()) null else search(confession, term)
    }.flowOn(defaultDispatcher)

    private val fontSize = preferences.fontSizeFlow.map { DoctrineFontSize(it) }

    private val reading = combine(document, chapterNumber, fontSize) { state, number, font ->
        val base = ConfessionChapterUiState(fontSize = font)
        when (state) {
            SnapshotState.Loading -> base.copy(isLoading = true)
            is SnapshotState.Error -> base.copy(isLoading = false, error = state.error.toUserMessage())
            is SnapshotState.Data -> {
                val chapters = (state.value as? DoctrineDocument.Confession)?.chapters.orEmpty()
                val index = chapters.indexOfFirst { it.number == number }
                base.copy(
                    documentTitle = state.value.title,
                    chapter = chapters.getOrNull(index),
                    hasPrevious = index > 0,
                    hasNext = index in 0 until chapters.lastIndex,
                    isLoading = false,
                )
            }
        }
    }

    val uiState: StateFlow<ConfessionChapterUiState> = combine(
        reading, isSearching, query, results,
    ) { base, searching, term, found ->
        base.copy(isSearching = searching, query = term, results = found)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ConfessionChapterUiState())

    private val _scroll = Channel<ScrollToParagraph>(capacity = Channel.BUFFERED)
    val scroll: Flow<ScrollToParagraph> = _scroll.receiveAsFlow()

    init {
        viewModelScope.launch { observeDocument.load(slug) }
        if (initialParagraph > 0) {
            viewModelScope.launch {
                val chapter = uiState.first { it.chapter != null }.chapter ?: return@launch
                scrollToParagraph(chapter, initialParagraph)
            }
        }
    }

    fun retry() {
        viewModelScope.launch { observeDocument.refresh(slug) }
    }

    fun next() = moveBy(1)

    fun previous() = moveBy(-1)

    private fun moveBy(step: Int) {
        val chapters = ((document.value as? SnapshotState.Data)?.value as? DoctrineDocument.Confession)
            ?.chapters ?: return
        val index = chapters.indexOfFirst { it.number == chapterNumber.value }
        val target = chapters.getOrNull(index + step) ?: return
        chapterNumber.value = target.number
        _scroll.trySend(ScrollToParagraph(0))
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
        val paragraph = result as? DoctrineSearchResult.Paragraph ?: return
        closeSearch()
        val chapters = ((document.value as? SnapshotState.Data)?.value as? DoctrineDocument.Confession)
            ?.chapters ?: return
        val chapter = chapters.firstOrNull { it.number == paragraph.chapter } ?: return
        chapterNumber.value = chapter.number
        scrollToParagraph(chapter, paragraph.paragraph)
    }

    fun increaseFont() {
        viewModelScope.launch { preferences.setFontSize(uiState.value.fontSize.increased()) }
    }

    fun decreaseFont() {
        viewModelScope.launch { preferences.setFontSize(uiState.value.fontSize.decreased()) }
    }

    private fun scrollToParagraph(chapter: ConfessionChapter, number: Int) {
        val index = chapter.paragraphs.indexOfFirst { it.number == number }
        if (index >= 0) _scroll.trySend(ScrollToParagraph(index))
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 200L
    }
}
