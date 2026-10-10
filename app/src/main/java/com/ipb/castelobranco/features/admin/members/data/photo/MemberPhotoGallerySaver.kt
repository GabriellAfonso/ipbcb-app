package com.ipb.castelobranco.features.admin.members.data.photo

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoExporter
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberPhotoUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Writes a member photo to `Pictures/IPB Castelo Branco`, plain, where the device gallery shows it.
 * On Android 10+ through MediaStore, hidden (`IS_PENDING`) until complete and deleted on failure;
 * on Android 7–9 to the public Pictures directory (the screen asked for the storage permission),
 * through a temp file renamed only once complete, then handed to the media scanner. Either way a
 * failure leaves nothing behind, and an existing file is never overwritten.
 */
class MemberPhotoGallerySaver @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val source: MemberPhotoSource,
) : MemberPhotoExporter {

    override suspend fun save(fileBaseName: String, url: String): Result<Unit> {
        val photo = source.bytesFor(url).getOrElse { return Result.failure(it) }
        val fileName = "$fileBaseName.${extensionFor(photo.mimeType)}"
        return withContext(Dispatchers.IO) {
            runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveWithMediaStore(fileName, photo)
                } else {
                    saveToPublicPictures(fileName, photo)
                }
                Timber.i("Member photo saved to the gallery")
            }.recoverCatching { throwable ->
                Timber.w("Member photo save failed (%s)", throwable.javaClass.simpleName)
                throw AppError.Unknown(message = throwable.message, cause = throwable, userMessage = SAVE_FAILED)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveWithMediaStore(fileName: String, photo: CachedPhoto) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, photo.mimeType)
            put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$FOLDER")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("MediaStore insert returned null")
        try {
            val output = resolver.openOutputStream(uri) ?: throw IOException("No output stream")
            output.use { it.write(photo.bytes) }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }

    @Suppress("DEPRECATION") // the public Pictures directory is the only way before scoped storage
    private fun saveToPublicPictures(fileName: String, photo: CachedPhoto) {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), FOLDER)
        if (!dir.isDirectory && !dir.mkdirs()) throw IOException("Cannot create the pictures folder")
        val target = freeFile(dir, fileName)
        val temp = File(dir, ".${target.name}$TEMP_SUFFIX")
        try {
            temp.writeBytes(photo.bytes)
            if (!temp.renameTo(target)) throw IOException("Rename failed")
        } catch (e: IOException) {
            temp.delete()
            throw e
        }
        MediaScannerConnection.scanFile(context, arrayOf(target.absolutePath), arrayOf(photo.mimeType), null)
    }

    /** "Name.jpg", else "Name (1).jpg", "Name (2).jpg"… — never an existing file. */
    private fun freeFile(dir: File, fileName: String): File {
        val base = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.')
        var candidate = File(dir, fileName)
        var index = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base ($index).$extension")
            index++
        }
        return candidate
    }

    private fun extensionFor(mimeType: String): String = when (mimeType) {
        ValidateMemberPhotoUseCase.MIME_PNG -> "png"
        ValidateMemberPhotoUseCase.MIME_WEBP -> "webp"
        ValidateMemberPhotoUseCase.MIME_GIF -> "gif"
        else -> "jpg"
    }

    companion object {
        const val FOLDER = "IPB Castelo Branco"
        const val SAVE_FAILED = "Não foi possível salvar a foto."
        private const val TEMP_SUFFIX = ".tmp"
    }
}
