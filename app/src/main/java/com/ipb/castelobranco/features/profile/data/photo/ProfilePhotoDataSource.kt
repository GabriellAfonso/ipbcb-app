package com.ipb.castelobranco.features.profile.data.photo

import android.content.Context
import com.ipb.castelobranco.core.data.local.StorageDirConstants
import com.ipb.castelobranco.core.di.ApiBaseUrl
import com.ipb.castelobranco.core.di.IoDispatcher
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.profile.data.api.ProfileApi
import com.ipb.castelobranco.features.profile.data.local.ProfilePhotoBus
import com.ipb.castelobranco.features.profile.data.local.ProfilePhotoCacheStorage
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfilePhotoDataSource @Inject constructor(
    private val api: ProfileApi,
    @ApplicationContext private val context: Context,
    @ApiBaseUrl private val baseUrl: String,
    private val photoCache: ProfilePhotoCacheStorage,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun upload(bytes: ByteArray, fileName: String): Result<String?> =
        runCatching {
            val body = bytes.toRequestBody("image/*".toMediaType())
            val part = MultipartBody.Part.createFormData(
                name = "photo",
                filename = fileName,
                body = body
            )

            val response = api.uploadProfilePhoto(part)
            if (!response.isSuccessful) throw response.toAppError()

            response.body()?.photoUrl
        }.mapError()

    suspend fun delete(): Result<Unit> =
        runCatching {
            val response = api.deleteProfilePhoto()
            if (!response.isSuccessful) throw response.toAppError()
            Unit
        }.mapError().also { result ->
            if (result.isSuccess) {
                clearLocal().getOrNull()
                ProfilePhotoBus.bump()
            }
        }

    /**
     * Downloads the profile photo from protected media (JWT required) and keeps the local copy in
     * step with what the server allows:
     * - 200: replaced; 304, 429 or no network: the last local photo stays;
     * - 404 or 403: cleared, placeholder shown — neither is an error for the user;
     * - anything else: failure, local photo untouched.
     */
    suspend fun downloadAndPersist(photoUrl: String): Result<File?> =
        withContext(ioDispatcher) {
            runCatching {
                val absoluteUrl = toAbsoluteUrl(photoUrl)

                val lastUrl = photoCache.loadLastUrlOrNull()
                if (lastUrl != null && lastUrl != absoluteUrl) {
                    photoCache.clearETag()
                }
                photoCache.saveLastUrl(absoluteUrl)

                val lastETag = photoCache.loadETagOrNull()

                try {
                    val response = api.downloadFile(
                        absoluteUrl = absoluteUrl,
                        ifNoneMatch = lastETag
                    )
                    applyResponse(response)
                } catch (e: IOException) {
                    Timber.w(e, "Profile photo download failed: network")
                    findLastLocalPhotoOrNull()
                }
            }.mapError()
        }

    private suspend fun applyResponse(response: Response<ResponseBody>): File? =
        when (response.code()) {
            HTTP_NOT_FOUND, HTTP_FORBIDDEN -> {
                clearLocal().getOrNull()
                photoCache.clearAll()
                ProfilePhotoBus.bump()
                null
            }

            HTTP_NOT_MODIFIED, HTTP_TOO_MANY_REQUESTS -> findLastLocalPhotoOrNull()

            else -> {
                if (!response.isSuccessful) throw response.toAppError()
                persist(response)
            }
        }

    private suspend fun persist(response: Response<ResponseBody>): File {
        val body = response.body()
            ?: throw AppError.Server(code = response.code(), message = "Corpo de resposta vazio")

        val contentType = body.contentType()?.toString().orEmpty()
        val ext = when {
            contentType.contains("png", ignoreCase = true) -> "png"
            contentType.contains("webp", ignoreCase = true) -> "webp"
            contentType.contains("jpeg", ignoreCase = true) ||
                    contentType.contains("jpg", ignoreCase = true) -> "jpg"
            else -> "jpg"
        }

        val dir = File(context.filesDir, StorageDirConstants.PROFILE).apply { mkdirs() }
        val outFile = File(dir, "profile_photo.$ext")

        // Escreve num temporário e só troca quando o corpo chegou inteiro: uma conexão cortada no meio
        // não pode estragar a foto que já estava na tela.
        val temp = File(dir, TEMP_FILE_NAME)
        try {
            body.byteStream().use { input ->
                FileOutputStream(temp).use { output ->
                    input.copyTo(output)
                }
            }
            if (!temp.renameTo(outFile)) temp.copyTo(outFile, overwrite = true)
        } finally {
            temp.delete()
        }

        val newETag = response.headers()["ETag"]?.trim()
        if (!newETag.isNullOrBlank()) photoCache.saveETag(newETag)

        ProfilePhotoBus.bump()
        return outFile
    }

    suspend fun clearLocal(): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val dir = File(context.filesDir, StorageDirConstants.PROFILE)
                if (dir.exists()) {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name.startsWith("profile_photo")) {
                            file.delete()
                        }
                    }
                }

                photoCache.clearAll()
                Unit
            }.mapError().also { result ->
                if (result.isSuccess) ProfilePhotoBus.bump()
            }
        }

    fun findLastLocalPhotoOrNull(): File? {
        val dir = File(context.filesDir, StorageDirConstants.PROFILE)
        return dir.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.name.startsWith("profile_photo.") && it.length() > 0L }
            ?.maxByOrNull { it.lastModified() }
    }

    private fun toAbsoluteUrl(url: String): String {
        val trimmed = url.trim()
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
        return "${baseUrl.trimEnd('/')}/${trimmed.trimStart('/')}"
    }

    private companion object {
        const val HTTP_NOT_MODIFIED = 304
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_TOO_MANY_REQUESTS = 429

        /** Fora do prefixo `profile_photo` para nunca ser lido como a foto. */
        const val TEMP_FILE_NAME = "download.part"
    }
}
