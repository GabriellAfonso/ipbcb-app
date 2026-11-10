package com.ipb.castelobranco.features.gallery.domain.upload

import javax.inject.Inject

/** A finished cut, under `gallery/uploads/`. */
data class TrimmedFile(val fileName: String, val sizeBytes: Long)

interface VideoTrimmer {
    /**
     * Cuts [source] (a picked URI as a string) to the range, keeping [videoMime]. Suspends until done and
     * answers `null` on failure, with the partial output deleted. Cancelling the coroutine stops the
     * export and deletes the partial output too.
     */
    suspend fun trim(
        source: String,
        startMs: Long,
        endMs: Long,
        videoMime: String,
        onProgress: (Int) -> Unit,
    ): TrimmedFile?

    suspend fun delete(fileName: String)
}

sealed interface TrimOutcome {
    data class Cut(val entry: UploadEntry.Cut, val sizeBytes: Long) : TrimOutcome

    /** The estimate is only an estimate: the real output was over the limit and was deleted. */
    data class TooLarge(val sizeBytes: Long) : TrimOutcome
    data object Failed : TrimOutcome
}

class TrimVideoUseCase @Inject constructor(
    private val trimmer: VideoTrimmer,
) {
    suspend operator fun invoke(
        source: String,
        video: TrimmableVideo,
        startMs: Long,
        endMs: Long,
        onProgress: (Int) -> Unit,
    ): TrimOutcome {
        val file = trimmer.trim(source, startMs, endMs, video.videoMime, onProgress) ?: return TrimOutcome.Failed
        if (file.sizeBytes > VideoUploadCheck.MAX_BYTES) {
            trimmer.delete(file.fileName)
            return TrimOutcome.TooLarge(file.sizeBytes)
        }
        val entry = UploadEntry.Cut(file.fileName, mp4Name(video.displayName), video.dateTaken)
        return TrimOutcome.Cut(entry, file.sizeBytes)
    }

    suspend fun discard(fileNames: List<String>) = fileNames.forEach { trimmer.delete(it) }

    /** The output is always MP4, also for a MOV source, and the upload's MIME follows the name. */
    private fun mp4Name(displayName: String) = "${displayName.substringBeforeLast('.')}$MP4_EXTENSION"

    private companion object {
        const val MP4_EXTENSION = ".mp4"
    }
}
