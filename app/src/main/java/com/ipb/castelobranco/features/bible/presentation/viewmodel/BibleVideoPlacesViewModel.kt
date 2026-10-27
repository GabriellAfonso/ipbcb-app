package com.ipb.castelobranco.features.bible.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.bible.domain.model.BibleBook
import com.ipb.castelobranco.features.bible.domain.model.BiblePlace
import com.ipb.castelobranco.features.bible.domain.model.BibleVideo
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import com.ipb.castelobranco.features.bible.domain.repository.BibleVideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BibleVideoPlacesUiState(
    /** Null while the list is not in memory, or when the video no longer exists. */
    val video: BibleVideo? = null,
    val books: List<BibleBook> = emptyList(),
    /** The places on screen, in the order shown (the draft that "Salvar" sends). */
    val places: List<BiblePlace> = emptyList(),
    val hasChanges: Boolean = false,
    val isSaving: Boolean = false,
)

sealed interface BibleVideoPlacesEvent {
    data object Saved : BibleVideoPlacesEvent
    data class ShowMessage(val text: String) : BibleVideoPlacesEvent
}

/** "Locais do vídeo": edits the whole set of places of one video and saves it at once. */
@HiltViewModel
class BibleVideoPlacesViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val videoRepository: BibleVideoRepository,
    bibleRepository: BibleRepository,
) : ViewModel() {

    private val videoId: Long = savedStateHandle.get<Long>(ARG_VIDEO_ID) ?: 0L
    private val preset: BiblePlace? = savedStateHandle.get<String>(ARG_PRESET_BOOK)
        ?.takeIf { it.isNotBlank() }
        ?.let { book -> BiblePlace.of(book, savedStateHandle.get<Int>(ARG_PRESET_CHAPTER)?.takeIf { it > 0 }) }

    /** Null until the video is first found: then it starts as the saved places plus the preset. */
    private val draft = MutableStateFlow<List<BiblePlace>?>(null)
    private val isSaving = MutableStateFlow(false)

    val uiState: StateFlow<BibleVideoPlacesUiState> = combine(
        videoRepository.videos,
        bibleRepository.booksFlow,
        draft,
        isSaving,
    ) { videos, books, draft, saving ->
        val video = videos.firstOrNull { it.id == videoId }
        val saved = video?.places.orEmpty().map { it.place }
        val places = draft ?: saved
        BibleVideoPlacesUiState(
            video = video,
            books = books.orEmpty(),
            places = places.sortedWith(placeOrder(books.orEmpty())),
            hasChanges = video != null && places.toSet() != saved.toSet(),
            isSaving = saving,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BibleVideoPlacesUiState())

    private val _events = Channel<BibleVideoPlacesEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            videoRepository.preload()
            val video = videoRepository.videos.first { videos -> videos.any { it.id == videoId } }
                .first { it.id == videoId }
            draft.value = (video.places.map { it.place } + listOfNotNull(preset)).distinct()
        }
    }

    fun remove(place: BiblePlace) = draft.update { current -> current?.minus(place) }

    fun add(places: List<BiblePlace>) = draft.update { current -> current?.let { (it + places).distinct() } }

    /** Sends the whole list; an empty one deletes the video (the screen confirms first). */
    fun save() {
        val places = draft.value ?: return
        if (isSaving.value) return
        isSaving.value = true
        viewModelScope.launch {
            val outcome = videoRepository.setPlaces(videoId, places)
            isSaving.value = false
            outcome.onSuccess { _events.send(BibleVideoPlacesEvent.Saved) }
            outcome.onFailure { error ->
                val text = (error as? AppError)?.toUserMessage() ?: MESSAGE_FAILED
                _events.send(BibleVideoPlacesEvent.ShowMessage(text))
            }
        }
    }

    /** Canonical book order, then whole book before its chapters, then chapter number. */
    private fun placeOrder(books: List<BibleBook>): Comparator<BiblePlace> {
        val index = books.mapIndexed { i, book -> book.abbrev.lowercase() to i }.toMap()
        return compareBy<BiblePlace>({ index[it.book] ?: Int.MAX_VALUE }, { it.chapter ?: 0 })
    }

    companion object {
        const val ARG_VIDEO_ID = "videoId"
        const val ARG_PRESET_BOOK = "presetBook"
        const val ARG_PRESET_CHAPTER = "presetChapter"
        private const val MESSAGE_FAILED = "Não foi possível salvar. Tente de novo."
    }
}
