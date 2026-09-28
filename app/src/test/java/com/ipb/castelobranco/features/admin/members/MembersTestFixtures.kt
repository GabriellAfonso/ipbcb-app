package com.ipb.castelobranco.features.admin.members

import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.features.admin.members.data.dto.HistoryEditorDto
import com.ipb.castelobranco.features.admin.members.data.dto.HistoryEntryDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberOptionsDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberRecordDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberSummaryDto
import com.ipb.castelobranco.features.admin.members.data.dto.NamedRefDto
import com.ipb.castelobranco.features.admin.members.domain.model.BirthDate
import com.ipb.castelobranco.features.admin.members.domain.model.Gender
import com.ipb.castelobranco.features.admin.members.domain.model.MemberRecord
import com.ipb.castelobranco.features.admin.members.domain.model.NamedRef
import okhttp3.Headers.Companion.headersOf
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.Response
import java.time.Instant
import java.time.LocalDate

/** 26/09/2026 — the day the feature was specified; every age in the tests is counted from it. */
val TODAY: LocalDate = LocalDate.of(2026, 9, 26)
val fixedDateProvider = DateProvider { TODAY }

val ATIVO = NamedRefDto(1, "Ativo")
val VISITANTE = NamedRefDto(3, "Visitante")

fun summaryDto(
    id: Int,
    name: String,
    status: NamedRefDto? = ATIVO,
    isActive: Boolean = true,
    photoUrl: String? = null,
) = MemberSummaryDto(id = id, name = name, photoUrl = photoUrl, status = status, isActive = isActive)

fun recordDto(
    id: Int = 12,
    name: String = "Ana Souza",
    birth: BirthDate = BirthDate(day = 2, month = 4, year = 1990),
    baptismDate: String? = "2005-06-12",
    role: NamedRefDto? = NamedRefDto(4, "Diaconisa"),
    ministries: List<NamedRefDto> = listOf(NamedRefDto(2, "Louvor"), NamedRefDto(5, "Recepção")),
    isActive: Boolean = true,
    photoUrl: String? = null,
) = MemberRecordDto(
    id = id,
    name = name,
    firstName = name.substringBefore(" "),
    lastName = name.substringAfter(" ", ""),
    birthDay = birth.day,
    birthMonth = birth.month,
    birthYear = birth.year,
    gender = "F",
    status = ATIVO,
    role = role,
    ministries = ministries,
    baptismDate = baptismDate,
    isActive = isActive,
    photoUrl = photoUrl,
    createdAt = "2026-03-14T10:12:00Z",
)

fun record(
    id: Int = 12,
    name: String = "Ana Souza",
    birth: BirthDate = BirthDate(day = 2, month = 4, year = 1990),
    baptismDate: LocalDate? = LocalDate.of(2005, 6, 12),
    role: NamedRef? = NamedRef(4, "Diaconisa"),
    ministries: List<NamedRef> = listOf(NamedRef(2, "Louvor"), NamedRef(5, "Recepção")),
    isValid: Boolean = true,
    photoUrl: String? = null,
) = MemberRecord(
    id = id,
    name = name,
    firstName = name.substringBefore(" "),
    lastName = name.substringAfter(" ", ""),
    birth = birth,
    gender = Gender.FEMALE,
    status = NamedRef(1, "Ativo"),
    role = role,
    ministries = ministries,
    baptismDate = baptismDate,
    isValid = isValid,
    photoUrl = photoUrl,
    createdAt = Instant.parse("2026-03-14T10:12:00Z"),
)

fun optionsDto() = MemberOptionsDto(
    statuses = listOf(ATIVO, NamedRefDto(2, "Inativo"), VISITANTE),
    roles = listOf(NamedRefDto(4, "Diaconisa")),
    ministries = listOf(NamedRefDto(2, "Louvor"), NamedRefDto(5, "Recepção")),
)

fun historyDto(
    id: Int,
    field: String,
    old: String? = null,
    new: String? = null,
    editor: String? = "Pr. João",
) = HistoryEntryDto(
    id = id,
    editor = editor?.let { HistoryEditorDto(id = "uuid-$id", name = it) },
    field = field,
    oldValue = old,
    newValue = new,
    changedAt = "2026-09-25T17:05:00Z",
)

fun <T> ok(body: T, etag: String? = null): Response<T> =
    if (etag == null) Response.success(body) else Response.success(body, headersOf("ETag", etag))

fun <T> notModified(): Response<T> = Response.error(
    "".toResponseBody(null),
    okhttp3.Response.Builder()
        .code(304)
        .message("Not Modified")
        .protocol(Protocol.HTTP_1_1)
        .request(Request.Builder().url("https://test/").build())
        .build(),
)

private val JSON = "application/json".toMediaType()

fun <T> apiError(code: Int, errorCode: String = "ERROR", detail: String = "Erro do servidor"): Response<T> =
    Response.error(code, """{"error_code":"$errorCode","detail":"$detail"}""".toResponseBody(JSON))

fun <T> fieldError(field: String, message: String): Response<T> = Response.error(
    400,
    ("""{"error_code":"VALIDATION_ERROR","detail":"Dados inválidos",""" +
        """"field_errors":{"$field":["$message"]}}""").toResponseBody(JSON),
)
