package com.ipb.castelobranco.features.admin.events.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.admin.events.data.api.EventsAdminApi
import com.ipb.castelobranco.features.admin.events.data.api.EventsAdminEndpoints
import com.ipb.castelobranco.features.admin.events.data.mapper.toDomain
import com.ipb.castelobranco.features.admin.events.domain.model.EventChanges
import com.ipb.castelobranco.features.admin.events.domain.model.EventRecord
import com.ipb.castelobranco.features.admin.events.domain.model.EventStatusFilter
import com.ipb.castelobranco.features.admin.events.domain.repository.EventsAdminRepository
import com.ipb.castelobranco.features.admin.events.domain.repository.EventsAdminRepository.Companion.EVENT_GONE
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EventsAdminRepositoryImpl @Inject constructor(
    private val api: EventsAdminApi,
) : EventsAdminRepository {

    override suspend fun list(status: EventStatusFilter): Result<List<EventRecord>> = runCatching {
        val response = api.getEvents(status.wireValue)
        if (!response.isSuccessful) throw response.toAppError()
        response.bodyOrThrow().events.map { it.toDomain() }
    }.mapError()

    override suspend fun get(id: Int): Result<EventRecord> = runCatching {
        val response = api.getEvent(id)
        response.throwIfFailed()
        response.bodyOrThrow().toDomain()
    }.mapError()

    override suspend fun create(changes: EventChanges, image: ByteArray): Result<EventRecord> = runCatching {
        val response = api.createEvent(fields = changes.toParts(), image = image.toImagePart())
        if (!response.isSuccessful) throw response.toAppError()
        response.bodyOrThrow().toDomain().also { Timber.i("Event %d created", it.id) }
    }.mapError()

    override suspend fun update(id: Int, changes: EventChanges, image: ByteArray?): Result<EventRecord> =
        runCatching {
            val response = api.updateEvent(id = id, fields = changes.toParts(), image = image?.toImagePart())
            response.throwIfFailed()
            response.bodyOrThrow().toDomain().also { Timber.i("Event %d updated", id) }
        }.mapError()

    override suspend fun delete(id: Int): Result<Unit> = runCatching {
        api.deleteEvent(id).throwIfFailed()
        Timber.i("Event %d deleted", id)
    }.mapError()

    private fun EventChanges.toParts(): Map<String, RequestBody> =
        values.entries.associate { (field, value) ->
            field.apiKey to value.toRequestBody(EventsAdminEndpoints.TEXT_MIME.toMediaType())
        }

    private fun ByteArray.toImagePart(): MultipartBody.Part = MultipartBody.Part.createFormData(
        EventsAdminEndpoints.IMAGE_PART,
        EventsAdminEndpoints.IMAGE_FILE_NAME,
        toRequestBody(EventsAdminEndpoints.IMAGE_MIME.toMediaType()),
    )

    /** A 404 means someone else deleted the event: the screen gets app copy, not the generic "not found". */
    private fun Response<*>.throwIfFailed() {
        if (isSuccessful) return
        val error = toAppError()
        if (error is AppError.Server && error.code == HTTP_NOT_FOUND) {
            throw AppError.Server(
                code = error.code,
                message = error.message,
                cause = error,
                errorCode = error.errorCode,
                userMessage = EVENT_GONE,
            )
        }
        throw error
    }

    private fun <T> Response<T>.bodyOrThrow(): T = body() ?: throw AppError.Unknown(message = EMPTY_BODY)

    private companion object {
        const val HTTP_NOT_FOUND = 404
        const val EMPTY_BODY = "Resposta vazia do servidor"
    }
}
