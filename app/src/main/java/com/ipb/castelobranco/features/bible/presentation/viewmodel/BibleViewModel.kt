package com.ipb.castelobranco.features.bible.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.ipb.castelobranco.core.data.NetworkConnectivityObserver
import com.ipb.castelobranco.features.bible.data.work.BibleDownloadWorker
import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.domain.model.BibleReadingPosition
import com.ipb.castelobranco.features.bible.domain.model.BibleTranslation
import com.ipb.castelobranco.features.bible.domain.model.VerseRef
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import com.ipb.castelobranco.features.bible.domain.usecase.BibleAutoDownloadUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BibleDownloadStatus(
    val isDownloading: Boolean = false,
    val isPending: Boolean = false,
    val downloaded: Int = 0,
    val total: Int = 0,
    val error: String? = null,
    val errorCode: Int? = null,
    val isResolved: Boolean = false,
)

data class BibleUiState(
    val books: List<BibleBook> = emptyList(),
    /** The active translation is on its way into memory: not the same as "not downloaded". */
    val isLoadingBooks: Boolean = true,
    val activeTranslation: BibleTranslation = BibleTranslation.Default,
    val cachedTranslations: Set<BibleTranslation> = emptySet(),
    val position: BibleReadingPosition = BibleReadingPosition.default(),
    val fontSize: Float = 18f,
    val download: BibleDownloadStatus = BibleDownloadStatus(),
    val selectedVerses: Set<VerseRef> = emptySet(),
) {
    /** Livro indicado pela posição atual; null se a Bíblia ainda não foi baixada. */
    val currentBook: BibleBook?
        get() = books.firstOrNull { it.abbrev.equals(position.bookAbbrev, ignoreCase = true) }
            ?: books.firstOrNull()

    val currentChapterVerses: List<String>
        get() {
            val book = currentBook ?: return emptyList()
            return book.chapter(position.chapter) ?: book.chapter(1).orEmpty()
        }

    val isBibleReady: Boolean get() = books.isNotEmpty()
}

sealed interface BibleUiEvent {
    data class CopiedToClipboard(val text: String) : BibleUiEvent
    data class ShareText(val text: String) : BibleUiEvent
    data class ShowMessage(val text: String) : BibleUiEvent
    data object ScrollToCurrentVerse : BibleUiEvent
}

@HiltViewModel
class BibleViewModel @Inject constructor(
    private val repository: BibleRepository,
    private val autoDownload: BibleAutoDownloadUseCase,
    workManager: WorkManager,
    connectivityObserver: NetworkConnectivityObserver,
) : ViewModel() {

    private val _selectedVerses = MutableStateFlow<Set<VerseRef>>(emptySet())

    val isOnWifi: StateFlow<Boolean> = connectivityObserver.isOnWifi
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val downloadStatus: StateFlow<BibleDownloadStatus> = workManager
        .getWorkInfosForUniqueWorkFlow(BibleDownloadWorker.WORK_NAME)
        .map { infos ->
            val info = infos.firstOrNull()
            when (info?.state) {
                WorkInfo.State.RUNNING -> {
                    val done = info.progress.getInt(BibleDownloadWorker.KEY_DOWNLOADED, 0)
                    val total = info.progress.getInt(BibleDownloadWorker.KEY_TOTAL, 0)
                    BibleDownloadStatus(isDownloading = true, downloaded = done, total = total, isResolved = true)
                }
                WorkInfo.State.ENQUEUED -> BibleDownloadStatus(isPending = true, isResolved = true)
                WorkInfo.State.FAILED -> {
                    val msg = info.outputData.getString(BibleDownloadWorker.KEY_ERROR) ?: "Falha ao baixar Bíblia"
                    val code = info.outputData.getInt(BibleDownloadWorker.KEY_ERROR_CODE, 0)
                    BibleDownloadStatus(error = msg, errorCode = code.takeIf { it != 0 }, isResolved = true)
                }
                else -> BibleDownloadStatus(isResolved = true)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BibleDownloadStatus())

    val uiState: StateFlow<BibleUiState> = combine(
        combine(
            repository.booksFlow,
            repository.activeTranslationFlow,
            repository.cachedTranslationsFlow,
            repository.positionFlow,
        ) { books, active, cached, position ->
            BibleUiState(
                books = books.orEmpty(),
                isLoadingBooks = books == null,
                activeTranslation = active,
                cachedTranslations = cached,
                position = position,
            )
        },
        repository.fontSizeFlow,
        downloadStatus,
        _selectedVerses,
    ) { base, font, status, selected ->
        base.copy(fontSize = font, download = status, selectedVerses = selected)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BibleUiState())

    private val _events = Channel<BibleUiEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun setActiveTranslation(translation: BibleTranslation) {
        viewModelScope.launch { repository.setActiveTranslation(translation) }
    }

    fun navigateTo(bookAbbrev: String, chapter: Int, verse: Int = 1) {
        viewModelScope.launch {
            repository.savePosition(bookAbbrev, chapter, verse)
            _selectedVerses.value = emptySet()
            _events.trySend(BibleUiEvent.ScrollToCurrentVerse)
        }
    }

    fun nextChapter() {
        val state = uiState.value
        val book = state.currentBook ?: return
        val nextCh = state.position.chapter + 1
        if (nextCh <= book.chapterCount) {
            navigateTo(book.abbrev, nextCh, verse = 1)
        } else {
            // Próximo livro
            val books = state.books
            val idx = books.indexOf(book)
            val next = books.getOrNull(idx + 1) ?: return
            navigateTo(next.abbrev, 1, 1)
        }
    }

    fun previousChapter() {
        val state = uiState.value
        val book = state.currentBook ?: return
        val prevCh = state.position.chapter - 1
        if (prevCh >= 1) {
            navigateTo(book.abbrev, prevCh, verse = 1)
        } else {
            val books = state.books
            val idx = books.indexOf(book)
            val prev = books.getOrNull(idx - 1) ?: return
            navigateTo(prev.abbrev, prev.chapterCount, 1)
        }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch { repository.setFontSize(size) }
    }

    fun toggleVerseSelection(ref: VerseRef) {
        _selectedVerses.update { current ->
            if (ref in current) current - ref else current + ref
        }
    }

    fun clearSelection() {
        _selectedVerses.value = emptySet()
    }

    fun copySelected() {
        val text = formatSelected() ?: return
        _events.trySend(BibleUiEvent.CopiedToClipboard(text))
    }

    fun shareSelected() {
        val text = formatSelected() ?: return
        _events.trySend(BibleUiEvent.ShareText(text))
    }

    private fun formatSelected(): String? {
        val state = uiState.value
        val book = state.currentBook ?: return null
        val verses = state.currentChapterVerses
        val selected = _selectedVerses.value
            .filter { it.bookAbbrev.equals(book.abbrev, ignoreCase = true) && it.chapter == state.position.chapter }
            .map { it.verse }
            .sorted()
        if (selected.isEmpty()) return null

        val ref = "${book.name} ${state.position.chapter}:${selected.joinToString(",")}"
        val body = selected.mapNotNull { v ->
            val text = verses.getOrNull(v - 1) ?: return@mapNotNull null
            "$v $text"
        }.joinToString("\n")
        return "$ref (${state.activeTranslation.code})\n$body"
    }

    fun retryDownloadOnMobileData() {
        autoDownload.enqueueAnyNetwork()
    }

    fun retryDownloadWifi() {
        autoDownload.enqueueWifiOnly(replaceExisting = true)
    }
}
