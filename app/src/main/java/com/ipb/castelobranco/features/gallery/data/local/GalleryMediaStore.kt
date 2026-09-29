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
 *
 * Every write goes to a temp file in `cacheDir` and is moved into place only once the body arrived
 * whole: a truncated file with the final name would count as downloaded forever.
 */
class GalleryMediaStore(private val context: Context) {

    private val root: File get() = File(context.filesDir, StorageDirConstants.GALLERY)
    private val originalsDir: File get() = File(root, ORIGINALS_DIR)
    private val coversDir: File get() = File(root, COVERS_DIR)

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

    fun saveCover(url: String, input: InputStream): File = writeAtomically(coverFile(url), input)

    /** Where the cover of [url] lives; it may not exist yet. */
    fun coverFile(url: String): File = File(coversDir, coverName(url))

    fun coverName(url: String): String = "${sha1(url)}.$COVER_EXTENSION"

    fun coverFiles(): List<File> = coversDir.listFiles()?.filter { it.isFile }.orEmpty()

    fun delete(file: File) {
        file.delete()
    }

    fun clearAll() {
        root.deleteRecursively()
    }

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
        private const val COVER_EXTENSION = "jpg"
        private const val TEMP_PREFIX = "gallery-"
        private const val TEMP_SUFFIX = ".part"
        private const val SHA_1 = "SHA-1"
    }
}
