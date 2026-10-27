package com.ipb.castelobranco.features.studies.data.repository

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.studies.data.api.SundaySchoolAdminApi
import com.ipb.castelobranco.features.studies.data.api.SundaySchoolEndpoints
import com.ipb.castelobranco.features.studies.data.dto.ContentErrorDto
import com.ipb.castelobranco.features.studies.data.mapper.LessonContentCodec
import com.ipb.castelobranco.features.studies.data.mapper.toDomain
import com.ipb.castelobranco.features.studies.data.mapper.toSummary
import com.ipb.castelobranco.features.studies.domain.model.ContentError
import com.ipb.castelobranco.features.studies.domain.model.Lesson
import com.ipb.castelobranco.features.studies.domain.model.LessonDraft
import com.ipb.castelobranco.features.studies.domain.model.LessonSaveResult
import com.ipb.castelobranco.features.studies.domain.model.LessonSummary
import com.ipb.castelobranco.features.studies.domain.model.ReorderResult
import com.ipb.castelobranco.features.studies.domain.model.SundaySchoolYearAdmin
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolAdminRepository
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolAdminRepository.Companion.LESSON_DELETED
import com.ipb.castelobranco.features.studies.domain.repository.SundaySchoolRepository
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import retrofit2.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SundaySchoolAdminRepositoryImpl @Inject constructor(
    private val api: SundaySchoolAdminApi,
    private val publicRepository: SundaySchoolRepository,
    private val json: Json,
) : SundaySchoolAdminRepository {

    override suspend fun years(): Result<List<SundaySchoolYearAdmin>> = runCatching {
        api.getYears().bodyOrThrow().map { it.toDomain() }
    }.mapError()

    override suspend fun lessons(year: Int): Result<List<LessonSummary>> = runCatching {
        api.getLessons(year).bodyOrThrow().map { it.toSummary() }
    }.mapError()

    override suspend fun lesson(id: Int): Result<Lesson> = runCatching {
        api.getLesson(id).bodyOrThrow().toDomain()
    }.mapError()

    override suspend fun save(id: Int?, draft: LessonDraft): LessonSaveResult = try {
        val body = draft.toBody()
        val response = if (id == null) api.createLesson(body) else api.updateLesson(id, body)
        val lesson = response.bodyOrThrow().toDomain()
        Timber.i("Sunday school lesson %d saved", lesson.id)
        refreshPublic(lesson.year.year, lesson.id)
        LessonSaveResult.Saved(lesson)
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        val error = t.toAppError()
        if (error is AppError.Server && error.code == HTTP_BAD_REQUEST) {
            LessonSaveResult.Invalid(
                contentErrors = error.contentErrors(),
                fieldErrors = error.fieldErrors.orEmpty(),
                message = error.userMessage,
            )
        } else {
            LessonSaveResult.Failed(error)
        }
    }

    override suspend fun delete(id: Int): Result<Unit> = runCatching {
        api.deleteLesson(id).throwIfFailed()
        Timber.i("Sunday school lesson %d deleted", id)
        publicRepository.refreshIndex(null)
        Unit
    }.mapError()

    override suspend fun reorder(year: Int, lessonIds: List<Int>): ReorderResult = try {
        val body = buildJsonObject {
            put(SundaySchoolEndpoints.FIELD_YEAR, year)
            put(SundaySchoolEndpoints.FIELD_LESSON_IDS, JsonArray(lessonIds.map { JsonPrimitive(it) }))
        }
        api.reorder(body).throwIfFailed()
        refreshPublic(year, lessonId = null)
        ReorderResult.Saved
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        val error = t.toAppError()
        if (error is AppError.Server && error.errorCode == SundaySchoolEndpoints.ERROR_ORDER_MISMATCH) {
            ReorderResult.Stale
        } else {
            ReorderResult.Failed(error)
        }
    }

    override suspend fun setTheme(year: Int, theme: String): Result<SundaySchoolYearAdmin> = runCatching {
        val body = buildJsonObject { put(SundaySchoolEndpoints.FIELD_THEME, theme.trim()) }
        val updated = api.updateYear(year, body).bodyOrThrow().toDomain()
        refreshPublic(year, lessonId = null)
        updated
    }.mapError()

    private fun LessonDraft.toBody(): JsonObject = buildJsonObject {
        put(SundaySchoolEndpoints.FIELD_YEAR, year)
        put(SundaySchoolEndpoints.FIELD_TITLE, title.trim())
        put(SundaySchoolEndpoints.FIELD_DATE, date?.toString()?.let(::JsonPrimitive) ?: JsonNull)
        put(SundaySchoolEndpoints.FIELD_TEACHER, teacher.trim())
        put(SundaySchoolEndpoints.FIELD_STATUS, status.wireName)
        put(SundaySchoolEndpoints.FIELD_CONTENT, LessonContentCodec.encode(content))
    }

    /** The reader on this device sees the change at once (research R-09); a failure here is not the save's. */
    private suspend fun refreshPublic(year: Int, lessonId: Int?) {
        publicRepository.refreshIndex(null)
        publicRepository.refreshIndex(year)
        if (lessonId != null) publicRepository.refreshLesson(lessonId)
    }

    private fun AppError.Server.contentErrors(): List<ContentError> =
        extras?.get(SundaySchoolEndpoints.EXTRA_ERRORS)?.let { raw ->
            runCatching { json.decodeFromString(ListSerializer(ContentErrorDto.serializer()), raw) }.getOrNull()
        }.orEmpty().map { ContentError(path = it.path, message = it.message) }

    private fun <T> Response<T>.bodyOrThrow(): T {
        throwIfFailed()
        return body() ?: throw AppError.Unknown(message = EMPTY_BODY)
    }

    /** A 404 on a lesson means someone else deleted it: the screen gets app copy. */
    private fun Response<*>.throwIfFailed() {
        if (isSuccessful) return
        val error = toAppError()
        if (error is AppError.Server && error.errorCode == SundaySchoolEndpoints.ERROR_LESSON_NOT_FOUND) {
            throw AppError.Server(
                code = error.code,
                message = error.message,
                cause = error,
                errorCode = error.errorCode,
                userMessage = LESSON_DELETED,
            )
        }
        throw error
    }

    private companion object {
        const val HTTP_BAD_REQUEST = 400
        const val EMPTY_BODY = "Resposta vazia do servidor"
    }
}
