package com.ipb.castelobranco.features.admin.members.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.domain.util.normalize
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.admin.members.data.api.MembersAdminApi
import com.ipb.castelobranco.features.admin.members.data.api.MembersAdminEndpoints
import com.ipb.castelobranco.features.admin.members.data.mapper.toDomain
import com.ipb.castelobranco.features.admin.members.data.mapper.toJsonObject
import com.ipb.castelobranco.features.admin.members.domain.model.HistoryEntry
import com.ipb.castelobranco.features.admin.members.domain.model.MemberChanges
import com.ipb.castelobranco.features.admin.members.domain.model.MemberOptions
import com.ipb.castelobranco.features.admin.members.domain.model.MemberRecord
import com.ipb.castelobranco.features.admin.members.domain.model.MemberSummary
import com.ipb.castelobranco.features.admin.members.domain.model.toSummary
import com.ipb.castelobranco.features.admin.members.domain.repository.MemberPhotoStore
import com.ipb.castelobranco.features.admin.members.domain.repository.MembersAdminRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds the roll for the current session, in memory only — never on disk (LGPD art. 11).
 *
 * The list and every record opened keep their `ETag`, so going back to a screen revalidates with
 * `If-None-Match` instead of downloading again. Each successful write patches the list right away,
 * which is how the list screen reflects a change made on the profile without any event between
 * the two. Signing out calls [clear] through the members `SessionScopedCache`.
 *
 * Member photos are the one thing allowed on disk, encrypted, in [photoStore]: a replaced, removed
 * or deleted photo leaves it at once, a fresh list drops photos removed by someone else, and a
 * refused read (401 after renewal, 403) erases it entirely.
 *
 * Logs carry the member id only.
 */
@Singleton
class MembersAdminRepositoryImpl @Inject constructor(
    private val api: MembersAdminApi,
    private val photoStore: MemberPhotoStore,
) : MembersAdminRepository {

    private data class CachedRecord(val etag: String?, val record: MemberRecord)

    private val members = MutableStateFlow<List<MemberSummary>?>(null)
    private val mutex = Mutex()
    private var listEtag: String? = null
    private val records = mutableMapOf<Int, CachedRecord>()

    override fun observeMembers(): Flow<List<MemberSummary>?> = members.asStateFlow()

    override suspend fun refreshMembers(): Result<Unit> = runCatching {
        val etag = mutex.withLock { listEtag.takeIf { members.value != null } }
        val response = api.getMembers(ifNoneMatch = etag)
        if (response.code() == HTTP_NOT_MODIFIED && etag != null) return@runCatching
        if (!response.isSuccessful) throw response.toAppError()
        val body = response.body() ?: throw AppError.Unknown(message = EMPTY_BODY)

        val list = body.members.map { it.toDomain() }
        mutex.withLock {
            listEtag = response.headers()[MembersAdminEndpoints.ETAG]
            members.value = list
        }
        photoStore.retainOnly(list.mapNotNullTo(HashSet()) { it.photoUrl })
    }.mapError().wipePhotosIfAccessLost()

    override suspend fun getMember(id: Int): Result<MemberRecord> = runCatching {
        val cached = mutex.withLock { records[id] }
        val response = api.getMember(id = id, ifNoneMatch = cached?.etag)
        if (response.code() == HTTP_NOT_MODIFIED && cached != null) return@runCatching cached.record
        response.throwIfFailed(id)
        val record = response.bodyOrThrow().toDomain()

        store(record, etag = response.headers()[MembersAdminEndpoints.ETAG])
        record
    }.mapError().wipePhotosIfAccessLost()

    override suspend fun getOptions(): Result<MemberOptions> = runCatching {
        val response = api.getOptions()
        if (!response.isSuccessful) throw response.toAppError()
        response.bodyOrThrow().toDomain()
    }.mapError()

    override suspend fun createMember(changes: MemberChanges): Result<MemberRecord> = runCatching {
        val response = api.createMember(changes.toJsonObject())
        if (!response.isSuccessful) throw response.toAppError()
        val record = response.bodyOrThrow().toDomain()

        store(record, etag = null)
        Timber.i("Member %d created", record.id)
        record
    }.mapError()

    override suspend fun updateMember(id: Int, changes: MemberChanges): Result<MemberRecord> = runCatching {
        val response = api.updateMember(id = id, body = changes.toJsonObject())
        response.throwIfFailed(id)
        val record = response.bodyOrThrow().toDomain()

        store(record, etag = null)
        Timber.i("Member %d updated", id)
        record
    }.mapError()

    override suspend fun deleteMember(id: Int): Result<Unit> = runCatching {
        val response = api.deleteMember(id)
        response.throwIfFailed(id)
        val photoUrl = photoUrlOf(id)
        forget(id)
        photoUrl?.let { photoStore.remove(it) }
        Timber.i("Member %d deleted", id)
    }.mapError()

    override suspend fun uploadPhoto(id: Int, bytes: ByteArray, mimeType: String): Result<String> =
        runCatching {
            val part = MultipartBody.Part.createFormData(
                MembersAdminEndpoints.PHOTO_PART,
                "$PHOTO_FILE_NAME.${extensionFor(mimeType)}",
                bytes.toRequestBody(mimeType.toMediaType()),
            )
            val response = api.uploadPhoto(id = id, photo = part)
            response.throwIfFailed(id)
            val url = response.bodyOrThrow().photoUrl

            val oldUrl = photoUrlOf(id)
            updatePhoto(id, url)
            if (oldUrl != null && oldUrl != url) photoStore.remove(oldUrl)
            Timber.i("Member %d photo changed", id)
            url
        }.mapError()

    override suspend fun removePhoto(id: Int): Result<Unit> = runCatching {
        val response = api.removePhoto(id)
        response.throwIfFailed(id)
        val oldUrl = photoUrlOf(id)
        updatePhoto(id, null)
        oldUrl?.let { photoStore.remove(it) }
        Timber.i("Member %d photo removed", id)
    }.mapError()

    override suspend fun getHistory(id: Int): Result<List<HistoryEntry>> = runCatching {
        val response = api.getHistory(id)
        response.throwIfFailed(id)
        response.bodyOrThrow().history.map { it.toDomain() }
    }.mapError()

    override suspend fun clear() {
        mutex.withLock {
            members.value = null
            listEtag = null
            records.clear()
        }
    }

    // region in-memory state

    private suspend fun store(record: MemberRecord, etag: String?) = mutex.withLock {
        records[record.id] = CachedRecord(etag, record)
        members.update { list ->
            list ?: return@update null
            val summary = record.toSummary()
            val others = list.filterNot { it.id == record.id }
            (others + summary).sortedBy { it.name.sortKey() }
        }
    }

    private suspend fun forget(id: Int) = mutex.withLock {
        records.remove(id)
        members.update { list -> list?.filterNot { it.id == id } }
    }

    private suspend fun photoUrlOf(id: Int): String? = mutex.withLock {
        records[id]?.record?.photoUrl ?: members.value?.firstOrNull { it.id == id }?.photoUrl
    }

    private suspend fun updatePhoto(id: Int, url: String?) = mutex.withLock {
        records[id]?.let { records[id] = CachedRecord(etag = null, record = it.record.copy(photoUrl = url)) }
        members.update { list -> list?.map { if (it.id == id) it.copy(photoUrl = url) else it } }
    }

    // endregion

    /**
     * A 404 on a member means someone else deleted it: the list forgets it too, and the screen
     * gets app copy instead of the server's generic "not found".
     */
    private suspend fun Response<*>.throwIfFailed(id: Int) {
        if (isSuccessful) return
        val error = toAppError()
        if (error is AppError.Server && error.code == HTTP_NOT_FOUND) {
            forget(id)
            throw AppError.Server(
                code = error.code,
                message = error.message,
                cause = error,
                errorCode = error.errorCode,
                userMessage = MEMBER_GONE,
            )
        }
        throw error
    }

    /** A refused read means the leader lost access: their device copy of the photos goes too. */
    private suspend fun <T> Result<T>.wipePhotosIfAccessLost(): Result<T> = onFailure {
        if (it is AppError.Auth) photoStore.wipe()
    }

    private fun <T> Response<T>.bodyOrThrow(): T = body() ?: throw AppError.Unknown(message = EMPTY_BODY)

    private fun String.sortKey(): String = normalize().lowercase()

    private fun extensionFor(mimeType: String): String = when (mimeType) {
        MIME_PNG -> "png"
        MIME_WEBP -> "webp"
        MIME_GIF -> "gif"
        else -> "jpg"
    }

    companion object {
        const val MEMBER_GONE = "Este membro não existe mais"
        private const val EMPTY_BODY = "Resposta vazia do servidor"
        private const val HTTP_NOT_MODIFIED = 304
        private const val HTTP_NOT_FOUND = 404
        private const val PHOTO_FILE_NAME = "member"
        private const val MIME_PNG = "image/png"
        private const val MIME_WEBP = "image/webp"
        private const val MIME_GIF = "image/gif"
    }
}
