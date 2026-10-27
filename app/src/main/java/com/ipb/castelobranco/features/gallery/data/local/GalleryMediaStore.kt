package com.ipb.castelobranco.features.gallery.data.local

import android.content.Context
import com.ipb.castelobranco.core.data.local.StorageDirConstants
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest

/**
 * The gallery's files on disk.
 *
 * - Originals, flat by photo id: `gallery/photos/{photoId}.{ext}`. Moving a photo between albums
 *   never touches its file.
 * - Covers, by a hash of their URL: `gallery/covers/{sha1(url)}.jpg`. A replaced cover gets a new URL,
 *   hence a new file; two albums inheriting the same cover share one.
 * - Videos, flat by item id: `gallery/videos/{id}.{mp4|mov}`. A download in progress is
 *   `{id}.{ext}.part` next to it — kept between runs so it resumes, never listed as a video.
 * - Upload queue files, by upload id: `gallery/uploads/{uploadId}.{ext}`. Inside the gallery root so
 *   sign-out removes them with the rest; the reconcile never looks at them.
 *
 * Every write goes to a temp file in `cacheDir` and is moved into place only once the body arrived
 * whole: a truncated file with the final name would count as downloaded forever.
 */
class GalleryMediaStore(private val context: Context) {

    private val root: File get() = File(context.filesDir, StorageDirConstants.GALLERY)
    private val originalsDir: File get() = File(root, ORIGINALS_DIR)
    private val coversDir: File get() = File(root, COVERS_DIR)
    private val uploadsDir: File get() = File(root, UPLOADS_DIR)
    private val videosDir: File get() = File(root, VIDEOS_DIR)

    fun saveOriginal(photoId: Long, ext: String, input: InputStream): File =
        writeAtomically(File(originalsDir, "$photoId.$ext"), input)

    fun hasOriginal(photoId: Long, ext: String): Boolean = File(originalsDir, "$photoId.$ext").exists()

    /** Photo id → original. Files whose name is not a photo id are ignored. */
    fun originalFiles(): Map<Long, File> =
        originalsDir.listFiles()
            ?.filter { it.isFile }
            ?.mapNotNull { file -> file.nameWithoutExtension.toLongOrNull()?.let { it to file } }
            ?.toMap()
            .orEmpty()

    /** Item id → complete video. Partials and files whose name is not `{id}.{mp4|mov}` are ignored. */
    fun videoFiles(): Map<Long, File> =
        videosDir.listFiles()
            ?.filter { it.isFile && it.extension.lowercase() in VIDEO_EXTENSIONS }
            ?.mapNotNull { file -> file.nameWithoutExtension.toLongOrNull()?.let { it to file } }
            ?.toMap()
            .orEmpty()

    fun videoFile(id: Long, ext: String): File = File(videosDir, "$id.$ext")

    /** Where the download of a video accumulates until it is whole. */
    fun partialVideo(id: Long, ext: String): File = File(videosDir, "$id.$ext$PARTIAL_SUFFIX").also {
        it.parentFile?.mkdirs()
    }

    /** Item ids of every partial video on disk, with its file. */
    fun partialVideos(): Map<Long, File> =
        videosDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(PARTIAL_SUFFIX) }
            ?.mapNotNull { file -> file.name.substringBefore('.').toLongOrNull()?.let { it to file } }
            ?.toMap()
            .orEmpty()

    /** The partial is whole: give it the final name, which makes it a video. */
    fun completeVideo(id: Long, ext: String): File {
        val partial = partialVideo(id, ext)
        val target = videoFile(id, ext)
        if (!partial.renameTo(target)) {
            partial.copyTo(target, overwrite = true)
            partial.delete()
        }
        return target
    }

    /** An uploaded video becomes the device's copy: the server stores it as sent. */
    fun adoptVideo(id: Long, ext: String, source: File): File = move(source, videoFile(id, ext))

    fun saveCover(url: String, input: InputStream): File = writeAtomically(coverFile(url), input)

    /** Where the cover of [url] lives; it may not exist yet. */
    fun coverFile(url: String): File = File(coversDir, coverName(url))

    fun coverName(url: String): String = "${sha1(url)}.$COVER_EXTENSION"

    fun coverFiles(): List<File> = coversDir.listFiles()?.filter { it.isFile }.orEmpty()

    fun saveUpload(fileName: String, input: InputStream): File = writeAtomically(uploadFile(fileName), input)

    /** Where the queue file [fileName] lives; it may not exist. */
    fun uploadFile(fileName: String): File = File(uploadsDir, fileName)

    /**
     * Makes a prepared upload the original of the photo the server created from it — the server
     * stores the file as sent, so there is nothing to download back.
     */
    fun adoptOriginal(photoId: Long, ext: String, source: File): File =
        move(source, File(originalsDir, "$photoId.$ext"))

    private fun move(source: File, target: File): File {
        target.parentFile?.mkdirs()
        if (!source.renameTo(target)) {
            source.copyTo(target, overwrite = true)
            source.delete()
        }
        return target
    }

    /** A fresh file in `cacheDir` for a one-off request (a cover being sent); the caller deletes it. */
    fun newTempFile(suffix: String): File = File.createTempFile(TEMP_PREFIX, suffix, context.cacheDir)

    fun delete(file: File) {
        file.delete()
    }

    fun clearAll() {
        root.deleteRecursively()
    }

    /** "Apagar galeria" with only Fotos: the photo originals go; covers, videos and the queue stay. */
    fun clearPhotos() {
        originalsDir.deleteRecursively()
    }

    /** "Apagar galeria" with only Vídeos: complete and partial videos. */
    fun clearVideos() {
        videosDir.deleteRecursively()
    }

    /** Bytes of every file under the gallery root — what [clearAll] frees. */
    fun totalBytes(): Long = bytesOf(root)

    /** Bytes of the videos, partials included. Everything else under the root counts as photos. */
    fun videoBytes(): Long = bytesOf(videosDir)

    private fun bytesOf(dir: File): Long = dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    private fun writeAtomically(target: File, input: InputStream): File {
        target.parentFile?.mkdirs()
        // Fora da árvore da galeria: um .part com o nome final seria contado como baixado.
        val temp = File.createTempFile(TEMP_PREFIX, TEMP_SUFFIX, context.cacheDir)
        try {
            FileOutputStream(temp).use { output -> input.copyTo(output) }
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
            }
        } finally {
            temp.delete()
        }
        return target
    }

    private fun sha1(value: String): String =
        MessageDigest.getInstance(SHA_1)
            .digest(value.toByteArray())
            .joinToString("") { "%02x".format(it) }

    companion object {
        const val ORIGINALS_DIR = "photos"
        const val COVERS_DIR = "covers"
        const val UPLOADS_DIR = "uploads"
        const val VIDEOS_DIR = "videos"
        private const val PARTIAL_SUFFIX = ".part"
        private val VIDEO_EXTENSIONS = setOf("mp4", "mov")
        private const val COVER_EXTENSION = "jpg"
        private const val TEMP_PREFIX = "gallery-"
        private const val TEMP_SUFFIX = ".part"
        private const val SHA_1 = "SHA-1"
    }
}
