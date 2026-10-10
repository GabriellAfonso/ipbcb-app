package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoExporter
import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoRevisions
import com.ipb.castelobranco.features.admin.members.domain.repository.MembersAdminRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
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

/** Bumps per URL whenever the device copy of a photo changes, so screens reload it. */
class ObserveMemberPhotoRevisionsUseCase @Inject constructor(
    private val revisions: MemberPhotoRevisions,
) {
    operator fun invoke(): Flow<Map<String, Int>> = revisions.revisions
}

/**
 * The name a downloaded photo gets in the gallery: the member's name and the day, e.g.
 * "Maria Souza 2026-10-09". Characters a file name cannot hold are dropped; a name left empty
 * falls back to "Membro <id>".
 */
object MemberPhotoFileName {
    private const val MAX_NAME_CHARS = 100
    private const val FALLBACK_PREFIX = "Membro "
    private val FORBIDDEN = Regex("""[\/:*?"<>|\p{Cntrl}]""")
    private val SPACES = Regex("""\s+""")

    fun build(name: String, memberId: Int, date: LocalDate): String {
        val clean = name.replace(FORBIDDEN, "").replace(SPACES, " ").trim().take(MAX_NAME_CHARS).trim()
        val base = clean.trimStart('.').ifEmpty { FALLBACK_PREFIX + memberId }
        return "$base $date"
    }
}

/**
 * Saves the member's photo, unencrypted, to the device gallery. The copy is the leader's from then
 * on: the app neither tracks nor removes it (specs/012-encrypted-session-photo-cache, FR-021).
 */
class DownloadMemberPhotoUseCase @Inject constructor(
    private val exporter: MemberPhotoExporter,
    private val dateProvider: DateProvider,
) {
    suspend operator fun invoke(id: Int, name: String, url: String): Result<Unit> =
        exporter.save(MemberPhotoFileName.build(name, id, dateProvider.today()), url)
}
