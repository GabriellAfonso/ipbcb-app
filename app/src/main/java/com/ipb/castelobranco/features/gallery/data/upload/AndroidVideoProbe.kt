package com.ipb.castelobranco.features.gallery.data.upload

import android.graphics.Bitmap
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import com.ipb.castelobranco.features.gallery.domain.upload.VideoContainer
import com.ipb.castelobranco.features.gallery.domain.upload.VideoProbeResult
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The container from the file's signature (`ftyp` brand `qt  ` = MOV, any other brand = MP4), the
 * tracks from [MediaExtractor], and the duration, recording date and a poster (a frame near the start,
 * longest side at most [POSTER_MAX_SIDE], JPEG) from [MediaMetadataRetriever].
 */
class AndroidVideoProbe @Inject constructor() : VideoProbe {

    override fun probe(video: File, poster: File): VideoProbeResult? = try {
        val (videoMime, audioMimes) = tracks(video)
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(video.absolutePath)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            VideoProbeResult(
                container = container(video),
                byteSize = video.length(),
                videoMime = videoMime,
                audioMimes = audioMimes,
                durationMs = duration,
                dateTaken = parseDate(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)),
                hasPoster = writePoster(retriever, poster),
            )
        } finally {
            retriever.release()
        }
    } catch (e: Exception) {
        Timber.w(e, "Picked video could not be read")
        null
    }

    private fun tracks(video: File): Pair<String?, List<String>> {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(video.absolutePath)
            val mimes = (0 until extractor.trackCount).mapNotNull {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)
            }
            return mimes.firstOrNull { it.startsWith(VIDEO_PREFIX) } to mimes.filter { it.startsWith(AUDIO_PREFIX) }
        } finally {
            extractor.release()
        }
    }

    /** ISO base media: bytes 4..7 are `ftyp`, 8..11 the major brand. */
    private fun container(video: File): VideoContainer {
        val header = ByteArray(HEADER_BYTES)
        val read = RandomAccessFile(video, READ_MODE).use { it.read(header) }
        if (read < HEADER_BYTES) return VideoContainer.OTHER
        val box = String(header, BOX_START, BOX_LENGTH, Charsets.US_ASCII)
        val brand = String(header, BRAND_START, BOX_LENGTH, Charsets.US_ASCII)
        return when {
            box != FTYP -> VideoContainer.OTHER
            brand == QUICKTIME_BRAND -> VideoContainer.MOV
            else -> VideoContainer.MP4
        }
    }

    private fun writePoster(retriever: MediaMetadataRetriever, poster: File): Boolean {
        val frame = retriever.getFrameAtTime(POSTER_TIME_US, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.frameAtTime
            ?: return false
        val scaled = scale(frame)
        return try {
            poster.parentFile?.mkdirs()
            FileOutputStream(poster).use { scaled.compress(Bitmap.CompressFormat.JPEG, POSTER_QUALITY, it) }
        } finally {
            if (scaled !== frame) scaled.recycle()
            frame.recycle()
        }
    }

    private fun scale(frame: Bitmap): Bitmap {
        val longSide = max(frame.width, frame.height)
        if (longSide <= POSTER_MAX_SIDE) return frame
        val ratio = POSTER_MAX_SIDE.toDouble() / longSide
        return Bitmap.createScaledBitmap(
            frame,
            (frame.width * ratio).roundToInt().coerceAtLeast(1),
            (frame.height * ratio).roundToInt().coerceAtLeast(1),
            true,
        )
    }

    /** `20260314T101500.000Z` → `2026-03-14`; anything else (or the empty 1904 date) → `null`. */
    private fun parseDate(raw: String?): String? {
        val digits = raw?.take(DATE_DIGITS)?.takeIf { it.length == DATE_DIGITS && it.all(Char::isDigit) }
        if (digits == null || digits.startsWith(EMPTY_YEAR)) return null
        return "${digits.substring(0, 4)}-${digits.substring(4, 6)}-${digits.substring(6, 8)}"
    }

    private companion object {
        const val VIDEO_PREFIX = "video/"
        const val AUDIO_PREFIX = "audio/"
        const val HEADER_BYTES = 12
        const val BOX_START = 4
        const val BRAND_START = 8
        const val BOX_LENGTH = 4
        const val FTYP = "ftyp"
        const val QUICKTIME_BRAND = "qt  "
        const val READ_MODE = "r"
        const val POSTER_TIME_US = 500_000L
        const val POSTER_MAX_SIDE = 1280
        const val POSTER_QUALITY = 85
        const val DATE_DIGITS = 8
        const val EMPTY_YEAR = "1904"
    }
}
