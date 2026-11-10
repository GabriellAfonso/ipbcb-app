package com.ipb.castelobranco.features.gallery.presentation.viewmodel

import com.ipb.castelobranco.features.gallery.domain.model.VideoDuration
import com.ipb.castelobranco.features.gallery.domain.upload.TrimOutcome
import com.ipb.castelobranco.features.gallery.domain.upload.TrimVideoUseCase
import com.ipb.castelobranco.features.gallery.domain.upload.UploadBatch
import com.ipb.castelobranco.features.gallery.domain.upload.VideoDecision
import com.ipb.castelobranco.features.gallery.presentation.state.TrimHandle
import com.ipb.castelobranco.features.gallery.presentation.state.TrimTime
import com.ipb.castelobranco.features.gallery.presentation.state.TrimTimeEditor
import com.ipb.castelobranco.features.gallery.presentation.state.VideoTrimUiState
import com.ipb.castelobranco.features.gallery.presentation.state.VideoTrimUiState.Companion.MIN_SELECTION_MS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The editor's rules, one video at a time. Owned by the graph-scoped `GalleryViewModel` on [scope], so
 * the batch and a running cut survive a rotation. A `null` [state] means no batch: the screen leaves.
 * [onDecided] receives the batch once its last video is decided.
 */
class VideoTrimController(
    private val scope: CoroutineScope,
    private val trimVideo: TrimVideoUseCase,
    private val onDecided: (UploadBatch) -> Unit,
) {
    private var batch: UploadBatch? = null
    private var cut: Job? = null

    private val _state = MutableStateFlow<VideoTrimUiState?>(null)
    val state: StateFlow<VideoTrimUiState?> = _state.asStateFlow()

    fun open(batch: UploadBatch) {
        this.batch = batch
        showCurrent()
    }

    /** Only the handle that moved is applied, held at least [MIN_SELECTION_MS] from the other. */
    fun setRange(startMs: Long, endMs: Long) {
        val current = _state.value ?: return
        if (!current.canMoveHandles) return
        val startMoved = startMs != current.startMs
        _state.value = if (startMoved) {
            val start = startMs.coerceIn(0L, current.endMs - MIN_SELECTION_MS)
            current.copy(startMs = start, previewMs = start, error = null)
        } else {
            val end = endMs.coerceIn(current.startMs + MIN_SELECTION_MS, current.durationMs)
            current.copy(endMs = end, previewMs = end, error = null)
        }
    }

    /** Dragging inside the selection slides it whole, keeping its length, and stops at the video's ends. */
    fun moveSelection(startMs: Long) {
        val current = _state.value ?: return
        if (!current.canMoveHandles) return
        val length = current.selectedMs
        val start = startMs.coerceIn(0L, current.durationMs - length)
        if (start == current.startMs) return
        _state.value = current.copy(startMs = start, endMs = start + length, previewMs = start, error = null)
    }

    /** Tapping a time opens its field, starting with the handle's current value. */
    fun editTime(handle: TrimHandle) {
        val current = _state.value ?: return
        if (!current.canMoveHandles) return
        val ms = if (handle == TrimHandle.START) current.startMs else current.endMs
        _state.value = current.copy(timeEditor = TrimTimeEditor(handle, TrimTime.format(ms)))
    }

    fun typeTime(text: String) {
        _state.update { state -> state?.copy(timeEditor = state.timeEditor?.copy(text = text, error = null)) }
    }

    fun closeTimeEditor() {
        _state.update { it?.copy(timeEditor = null) }
    }

    /** A refused value keeps the field open with the reason; the handles stay as they were. */
    fun applyTime() {
        val current = _state.value ?: return
        val editor = current.timeEditor ?: return
        val ms = TrimTime.parse(editor.text)
        val error = when {
            ms == null -> VideoTexts.INVALID_TIME
            ms > current.durationMs -> VideoTexts.timeOutsideVideo(VideoDuration.format(current.durationMs))
            editor.handle == TrimHandle.START && ms > current.endMs - MIN_SELECTION_MS ->
                VideoTexts.startTooLate(TrimTime.format(current.endMs))
            editor.handle == TrimHandle.END && ms < current.startMs + MIN_SELECTION_MS ->
                VideoTexts.endTooEarly(TrimTime.format(current.startMs))
            else -> null
        }
        if (error != null || ms == null) {
            _state.value = current.copy(timeEditor = editor.copy(error = error))
            return
        }
        _state.value = if (editor.handle == TrimHandle.START) {
            current.copy(startMs = ms, previewMs = ms, error = null, timeEditor = null)
        } else {
            current.copy(endMs = ms, previewMs = ms, error = null, timeEditor = null)
        }
    }

    /** The primary button: whole while both handles are at the ends, otherwise the cut. */
    fun confirm() {
        val current = _state.value ?: return
        if (current.isWhole) useWhole() else cut()
    }

    fun cancelCut() {
        cut?.cancel()
        cut = null
        _state.update { it?.copy(isCutting = false, progress = 0) }
    }

    fun sendAllWhole() {
        val current = _state.value ?: return
        if (current.isCutting) return
        batch = batch?.allWhole()
        advance()
    }

    fun askDiscard() {
        _state.update { it?.copy(askingDiscard = true) }
    }

    /** A cut may have finished the batch while the question was open: it was waiting for this answer. */
    fun keepEditing() {
        _state.update { it?.copy(askingDiscard = false) }
        if (batch?.isComplete == true) advance()
    }

    fun discard() {
        cut?.cancel()
        cut = null
        val files = batch?.cutFileNames.orEmpty()
        batch = null
        _state.value = null
        scope.launch { trimVideo.discard(files) }
    }

    private fun useWhole() {
        val current = _state.value ?: return
        if (!current.canUseWhole) return
        decide(VideoDecision.Whole)
    }

    private fun cut() {
        val current = _state.value ?: return
        val item = batch?.current ?: return
        val video = item.media.trimmable ?: return
        if (!current.canCut) return
        _state.value = current.copy(isCutting = true, progress = 0, error = null)
        cut = scope.launch {
            val outcome = trimVideo(item.media.source, video, current.startMs, current.endMs) { progress ->
                _state.update { it?.copy(progress = progress) }
            }
            when (outcome) {
                is TrimOutcome.Cut -> decide(VideoDecision.Cut(outcome.entry, outcome.sizeBytes))
                is TrimOutcome.TooLarge -> stopCutting(VideoTexts.cutTooLarge(outcome.sizeBytes))
                TrimOutcome.Failed -> stopCutting(VideoTexts.CUT_FAILED)
            }
        }
    }

    private fun stopCutting(error: String) {
        _state.update { it?.copy(isCutting = false, progress = 0, error = error) }
    }

    private fun decide(decision: VideoDecision) {
        val current = batch ?: return
        batch = current.decided(current.currentIndex, decision)
        advance()
    }

    private fun advance() {
        val current = batch ?: return
        when {
            !current.isComplete -> showCurrent()
            _state.value?.askingDiscard == true -> _state.update { it?.copy(isCutting = false) }
            else -> {
                batch = null
                _state.value = null
                onDecided(current)
            }
        }
    }

    private fun showCurrent() {
        val current = batch ?: return
        val media = current.current?.media ?: return
        val video = media.trimmable ?: return
        _state.value = VideoTrimUiState(
            position = current.position,
            total = current.editableCount,
            undecided = current.undecidedCount,
            source = media.source,
            durationMs = video.durationMs,
            sizeBytes = media.sizeBytes,
            askingDiscard = _state.value?.askingDiscard == true,
        )
    }
}
