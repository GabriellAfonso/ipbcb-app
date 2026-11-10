package com.ipb.castelobranco.features.gallery.data.upload

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.transformer.Composition
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import com.ipb.castelobranco.features.gallery.data.local.GalleryMediaStore
import com.ipb.castelobranco.features.gallery.domain.upload.TrimmedFile
import com.ipb.castelobranco.features.gallery.domain.upload.VideoTrimmer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Cuts with Media3: with trim optimization only the part up to the first keyframe is re-encoded and the
 * rest is copied; when that does not apply, the selected part goes through a normal export.
 *
 * Runs on the main thread because every [Transformer] call must come from the thread that built it.
 */
@OptIn(UnstableApi::class)
class Media3VideoTrimmer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val mediaStore: GalleryMediaStore,
) : VideoTrimmer {

    override suspend fun trim(
        source: String,
        startMs: Long,
        endMs: Long,
        videoMime: String,
        onProgress: (Int) -> Unit,
    ): TrimmedFile? = withContext(Dispatchers.Main.immediate) {
        val fileName = mediaStore.newCutFileName()
        val output = mediaStore.uploadFile(fileName)
        output.parentFile?.mkdirs()
        val finished = CompletableDeferred<ExportResult?>()
        try {
            val transformer = newTransformer(videoMime, finished)
            try {
                transformer.start(clippedItem(source, startMs, endMs), output.absolutePath)
                val result = awaitExport(transformer, finished, onProgress)
                result?.let { TrimmedFile(fileName, output.length()) } ?: discard(output)
            } catch (e: CancellationException) {
                transformer.cancel()
                output.delete()
                throw e
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.tag(LOG_TAG).w(e, "Export could not start")
            discard(output)
        }
    }

    override suspend fun delete(fileName: String) {
        mediaStore.delete(mediaStore.uploadFile(fileName))
    }

    private fun newTransformer(videoMime: String, finished: CompletableDeferred<ExportResult?>): Transformer =
        Transformer.Builder(context)
            .experimentalSetTrimOptimizationEnabled(true)
            .setVideoMimeType(videoMime)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    Timber.tag(LOG_TAG).i("Export done, optimizationResult=%d", exportResult.optimizationResult)
                    finished.complete(exportResult)
                }

                override fun onError(
                    composition: Composition,
                    exportResult: ExportResult,
                    exportException: ExportException,
                ) {
                    Timber.tag(LOG_TAG).w(exportException, "Export failed")
                    finished.complete(null)
                }
            })
            .build()

    private fun clippedItem(source: String, startMs: Long, endMs: Long): MediaItem =
        MediaItem.Builder()
            .setUri(source)
            .setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder()
                    .setStartPositionMs(startMs)
                    .setEndPositionMs(endMs)
                    .build(),
            )
            .build()

    /** The progress may be unavailable for a while; the last value stays on screen until it comes back. */
    private suspend fun awaitExport(
        transformer: Transformer,
        finished: CompletableDeferred<ExportResult?>,
        onProgress: (Int) -> Unit,
    ): ExportResult? = coroutineScope {
        val polling = launch {
            val holder = ProgressHolder()
            while (isActive) {
                if (transformer.getProgress(holder) == Transformer.PROGRESS_STATE_AVAILABLE) onProgress(holder.progress)
                delay(PROGRESS_INTERVAL_MS)
            }
        }
        try {
            finished.await()
        } finally {
            polling.cancel()
        }
    }

    private fun discard(output: File): TrimmedFile? {
        output.delete()
        return null
    }

    private companion object {
        const val LOG_TAG = "VideoTrim"
        const val PROGRESS_INTERVAL_MS = 200L
    }
}
