package com.ipb.castelobranco.features.gallery.data.upload

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.ipb.castelobranco.features.gallery.domain.upload.PickedVideoInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject

/** A source can be much larger than the upload limit, so it is read in place, never copied. */
class AndroidPickedVideoReader @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : PickedVideoReader {

    override fun read(source: String): PickedVideoInfo? = try {
        val uri = Uri.parse(source)
        val extractor = MediaExtractor()
        val (videoMime, audioMimes) = try {
            extractor.setDataSource(context, uri, null)
            extractor.trackMimes()
        } finally {
            extractor.release()
        }
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            PickedVideoInfo(
                videoMime = videoMime,
                audioMimes = audioMimes,
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull(),
                dateTaken = parseRecordingDate(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)),
            )
        } finally {
            retriever.release()
        }
    } catch (e: Exception) {
        Timber.w(e, "Picked video could not be read from its URI")
        null
    }
}
