package com.ipb.castelobranco.features.gallery.presentation.state

import com.ipb.castelobranco.core.presentation.components.formatDiskUsage
import com.ipb.castelobranco.features.gallery.domain.model.VideoDuration
import com.ipb.castelobranco.features.gallery.domain.upload.VideoUploadCheck
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.VideoTexts

enum class TrimHandle { START, END }

/** The field opened by tapping a time: what was typed and why it was refused. */
data class TrimTimeEditor(val handle: TrimHandle, val text: String, val error: String? = null) {
    val title: String get() = if (handle == TrimHandle.START) VideoTexts.START_TIME_TITLE else VideoTexts.END_TIME_TITLE
}

/** The video on the editor. [previewMs] is where the player should stand: the handle moved last. */
data class VideoTrimUiState(
    val position: Int,
    val total: Int,
    val undecided: Int,
    val source: String,
    val durationMs: Long,
    val sizeBytes: Long?,
    val startMs: Long = 0L,
    val endMs: Long = durationMs,
    val previewMs: Long = 0L,
    val isCutting: Boolean = false,
    val progress: Int = 0,
    val error: String? = null,
    val askingDiscard: Boolean = false,
    val timeEditor: TrimTimeEditor? = null,
) {
    val selectedMs: Long get() = endMs - startMs
    val isWhole: Boolean get() = startMs == 0L && endMs == durationMs
    val canMoveHandles: Boolean get() = !isCutting && durationMs > MIN_SELECTION_MS
    val estimatedBytes: Long? get() = sizeBytes?.let { it * selectedMs / durationMs }
    val isOverLimit: Boolean get() = (sizeBytes ?: 0L) > VideoUploadCheck.MAX_BYTES
    val canUseWhole: Boolean get() = !isCutting && !isOverLimit
    val canCut: Boolean get() = !isCutting && !isWhole && (estimatedBytes ?: 0L) <= VideoUploadCheck.MAX_BYTES
    val showSendAllWhole: Boolean get() = undecided >= 2
    val positionLabel: String? get() = VideoTexts.position(position, total).takeIf { total > 1 }
    val primaryLabel: String get() = if (isWhole) VideoTexts.USE_WHOLE else VideoTexts.CUT_AND_CONTINUE
    val primaryEnabled: Boolean get() = if (isWhole) canUseWhole else canCut
    val tooBigMessage: String? get() = VideoTexts.TOO_BIG_TO_SEND.takeIf { isOverLimit }
    val startLabel: String get() = VideoDuration.format(startMs)
    val endLabel: String get() = VideoDuration.format(endMs)

    /** `0:42 · 18 MB`, without the size when the picker did not say it. */
    val selectionLabel: String
        get() = listOfNotNull(VideoDuration.format(selectedMs), estimatedBytes?.let(::formatDiskUsage))
            .joinToString(LABEL_SEPARATOR)

    companion object {
        const val MIN_SELECTION_MS = 1_000L
        private const val LABEL_SEPARATOR = " · "
    }
}
