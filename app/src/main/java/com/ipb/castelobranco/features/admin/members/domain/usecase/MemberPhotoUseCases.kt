package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.members.domain.repository.MembersAdminRepository
import javax.inject.Inject

/**
 * The server's photo rule, checked before uploading: a decodable JPEG, PNG, WEBP or GIF up to
 * 10 MB. The format is read from the file's first bytes, not from a name or a declared type.
 *
 * @return the MIME type to upload with
 */
class ValidateMemberPhotoUseCase @Inject constructor() {

    operator fun invoke(bytes: ByteArray): Result<String> {
        if (bytes.isEmpty()) return failure(INVALID_FORMAT)
        if (bytes.size > MAX_BYTES) return failure(TOO_LARGE)
        val mime = detectMime(bytes) ?: return failure(INVALID_FORMAT)
        return Result.success(mime)
    }

    private fun detectMime(bytes: ByteArray): String? = when {
        bytes.startsWith(0xFF, 0xD8, 0xFF) -> MIME_JPEG
        bytes.startsWith(0x89, 0x50, 0x4E, 0x47) -> MIME_PNG
        bytes.startsWith(0x47, 0x49, 0x46, 0x38) -> MIME_GIF
        bytes.startsWith(0x52, 0x49, 0x46, 0x46) && bytes.hasAt(WEBP_OFFSET, 0x57, 0x45, 0x42, 0x50) -> MIME_WEBP
        else -> null
    }

    private fun ByteArray.startsWith(vararg signature: Int): Boolean = hasAt(0, *signature)

    private fun ByteArray.hasAt(offset: Int, vararg signature: Int): Boolean =
        size >= offset + signature.size &&
            signature.indices.all { (this[offset + it].toInt() and BYTE_MASK) == signature[it] }

    private fun failure(message: String): Result<String> =
        Result.failure(AppError.Unknown(message = message, userMessage = message))

    companion object {
        const val MAX_BYTES = 10 * 1024 * 1024
        const val TOO_LARGE = "A foto deve ter no máximo 10 MB."
        const val INVALID_FORMAT = "Use uma imagem JPEG, PNG, WEBP ou GIF."
        const val MIME_JPEG = "image/jpeg"
        const val MIME_PNG = "image/png"
        const val MIME_GIF = "image/gif"
        const val MIME_WEBP = "image/webp"
        private const val WEBP_OFFSET = 8
        private const val BYTE_MASK = 0xFF
    }
}

/** Validates, then uploads. @return the new photo URL */
class UploadMemberPhotoUseCase @Inject constructor(
    private val validate: ValidateMemberPhotoUseCase,
    private val repository: MembersAdminRepository,
) {
    suspend operator fun invoke(id: Int, bytes: ByteArray): Result<String> {
        val mime = validate(bytes).getOrElse { return Result.failure(it) }
        return repository.uploadPhoto(id, bytes, mime)
    }
}

class RemoveMemberPhotoUseCase @Inject constructor(
    private val repository: MembersAdminRepository,
) {
    suspend operator fun invoke(id: Int): Result<Unit> = repository.removePhoto(id)
}
