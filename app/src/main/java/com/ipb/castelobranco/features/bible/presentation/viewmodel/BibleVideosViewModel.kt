package com.ipb.castelobranco.features.bible.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.ObserveAccessUseCase
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.bible.domain.model.BiblePlace
import com.ipb.castelobranco.features.bible.domain.model.BibleProgress
import com.ipb.castelobranco.features.bible.domain.model.BibleVideo
import com.ipb.castelobranco.features.bible.domain.model.BibleVideoSections
import com.ipb.castelobranco.features.bible.domain.model.ProgressTarget
import com.ipb.castelobranco.features.bible.domain.model.inPlace
import com.ipb.castelobranco.features.bible.domain.model.resolve
import com.ipb.castelobranco.features.bible.domain.model.sectionsFor
import com.ipb.castelobranco.features.bible.domain.repository.AddVideoResult
import com.ipb.castelobranco.features.bible.domain.repository.BibleProgressRepository
import com.ipb.castelobranco.features.bible.domain.repository.BibleRepository
import com.ipb.castelobranco.features.bible.domain.repository.BibleVideoRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Drag mode of one section of the sheet: [videos] is the order on screen while dragging. */
data class BibleVideoReorder(
    val place: BiblePlace,
    val videos: List<BibleVideo>,
)

data class BibleVideosUiState(
    /** Book and chapter being read; null while the Bible index is not in memory. */
    val chapterPlace: BiblePlace? = null,
    val bookName: String = "",
    val sections: BibleVideoSections = BibleVideoSections.EMPTY,
    val canManage: Boolean = false,
    val canDelete: Boolean = false,
    /** Null when signed out: no watched check. */
    val progress: BibleProgress? = null,
    val isAdding: Boolean = false,
    val addError: String? = null,
    val reorder: BibleVideoReorder? = null,
    val isBusy: Boolean = false,
) {
    val chapterTitle: String get() = "$bookName ${chapterPlace?.chapter ?: ""}".trim()

    /** Members see the play icon only when there is something to watch; managers always (the entry to add). */
    val showPlayIcon: Boolean get() = chapterPlace != null && (canManage || !sections.isEmpty)
}

sealed interface BibleVideosEvent {
    /** Opens "Locais do vídeo"; [preset] is a place added to the draft but not saved yet. */
    data class OpenPlaces(val videoId: Long, val preset: BiblePlace? = null) : BibleVideosEvent
    data class ShowMessage(val text: String) : BibleVideosEvent
}

/** The videos sheet of the reader: what to show for the current chapter, management and the watched check. */
@HiltViewModel
class BibleVideosViewModel @Inject constructor(
    private val videoRepository: BibleVideoRepository,
    private val progressRepository: BibleProgressRepository,
    bibleRepository: BibleRepository,
    observeAccess: ObserveAccessUseCase,
) : ViewModel() {

    private val _isSignedIn = MutableStateFlow(false)
    private val _local = MutableStateFlow(LocalState())

    private data class LocalState(
        val isAdding: Boolean = false,
        val addError: String? = null,
        val reorder: BibleVideoReorder? = null,
        val isBusy: Boolean = false,
    )

    private val chapterInfo = combine(bibleRepository.booksFlow, bibleRepository.positionFlow) { books, position ->
        val book = books.orEmpty().resolve(position.bookAbbrev) ?: return@combine null
        BiblePlace.of(book.abbrev, position.chapter) to book.name
    }

    private val progress = combine(progressRepository.progress, _isSignedIn) { progress, signedIn ->
        progress.takeIf { signedIn }
    }

    val uiState: StateFlow<BibleVideosUiState> = combine(
        videoRepository.videos,
        chapterInfo,
        observeAccess(),
        progress,
        _local,
    ) { videos, chapter, access, progress, local ->
        val place = chapter?.first
        BibleVideosUiState(
            chapterPlace = place,
            bookName = chapter?.second.orEmpty(),
            sections = place?.chapter?.let { videos.sectionsFor(place.book, it) } ?: BibleVideoSections.EMPTY,
            canManage = access.allows(Scope.BIBLE_VIDEOS, AccessLevel.MANAGE),
            canDelete = access.allows(Scope.BIBLE_VIDEOS, AccessLevel.OWNER),
            progress = progress,
            isAdding = local.isAdding,
            addError = local.addError,
            reorder = local.reorder,
            isBusy = local.isBusy,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, BibleVideosUiState())

    private val _events = Channel<BibleVideosEvent>(capacity = Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            videoRepository.preload()
            videoRepository.refresh()
        }
    }

    fun setSignedIn(signedIn: Boolean) {
        _isSignedIn.value = signedIn
    }

    fun clearAddError() = _local.update { it.copy(addError = null) }

    /** Adds [url] on the current chapter; on success (or when it already exists) opens its places. */
    fun addVideo(url: String) {
        val place = uiState.value.chapterPlace ?: return
        if (url.isBlank() || _local.value.isAdding) return
        _local.update { it.copy(isAdding = true, addError = null) }
        viewModelScope.launch {
            when (val result = videoRepository.addVideo(url, place)) {
                is AddVideoResult.Added -> {
                    _local.update { it.copy(isAdding = false) }
                    _events.send(BibleVideosEvent.OpenPlaces(result.video.id))
                }
                is AddVideoResult.AlreadyExists -> {
                    _local.update { it.copy(isAdding = false) }
                    _events.send(BibleVideosEvent.ShowMessage(MESSAGE_ALREADY_EXISTS))
                    val existing = result.video ?: return@launch
                    val preset = place.takeUnless { existing.isIn(it) }
                    _events.send(BibleVideosEvent.OpenPlaces(existing.id, preset))
                }
                is AddVideoResult.Failed ->
                    _local.update { it.copy(isAdding = false, addError = result.error.toUserMessage()) }
            }
        }
    }

    fun removeFromPlace(video: BibleVideo, place: BiblePlace) = runWrite {
        videoRepository.removePlace(video.id, place)
    }

    fun deleteVideo(video: BibleVideo) = runWrite { videoRepository.deleteVideo(video.id) }

    fun startReorder(place: BiblePlace) {
        val videos = videoRepository.videos.value.inPlace(place)
        if (videos.size < 2) return
        _local.update { it.copy(reorder = BibleVideoReorder(place, videos)) }
    }

    /** Moves a card on screen while dragging; saved by [saveOrder] when the drag stops. */
    fun moveInReorder(from: Int, to: Int) = _local.update { local ->
        val reorder = local.reorder ?: return@update local
        if (from !in reorder.videos.indices || to !in reorder.videos.indices) return@update local
        val moved = reorder.videos.toMutableList().apply { add(to, removeAt(from)) }
        local.copy(reorder = reorder.copy(videos = moved))
    }

    fun saveOrder() {
        val reorder = _local.value.reorder ?: return
        val current = videoRepository.videos.value.inPlace(reorder.place).map { it.id }
        val wanted = reorder.videos.map { it.id }
        if (current == wanted) return
        runWrite(keepReorder = true) { videoRepository.reorder(reorder.place, wanted) }
    }

    fun stopReorder() = _local.update { it.copy(reorder = null) }

    fun toggleWatched(video: BibleVideo) {
        val progress = uiState.value.progress ?: return
        viewModelScope.launch {
            progressRepository.mark(listOf(ProgressTarget.Video(video.id)), done = !progress.isWatched(video.id))
        }
    }

    /** A write needs the network; a failure shows a message and the list stays as the server has it. */
    private fun runWrite(keepReorder: Boolean = false, write: suspend () -> Result<Unit>) {
        _local.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            val outcome = write()
            _local.update { local ->
                val reorder = local.reorder?.takeIf { keepReorder }?.let { current ->
                    // On failure the drag order goes back to the server's.
                    if (outcome.isFailure) current.copy(videos = videoRepository.videos.value.inPlace(current.place))
                    else current
                }
                local.copy(isBusy = false, reorder = reorder)
            }
            outcome.exceptionOrNull()?.let { error ->
                val message = (error as? AppError)?.toUserMessage()
                    ?: MESSAGE_FAILED
                _events.send(BibleVideosEvent.ShowMessage(message))
            }
        }
    }

    private companion object {
        const val MESSAGE_ALREADY_EXISTS = "Esse vídeo já está cadastrado"
        const val MESSAGE_FAILED = "Não foi possível salvar. Tente de novo."
    }
}
