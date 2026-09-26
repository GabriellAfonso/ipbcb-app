package com.ipb.castelobranco.features.admin.members.data.mapper

import com.ipb.castelobranco.features.admin.members.data.dto.HistoryEntryDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberOptionsDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberRecordDto
import com.ipb.castelobranco.features.admin.members.data.dto.MemberSummaryDto
import com.ipb.castelobranco.features.admin.members.data.dto.NamedRefDto
import com.ipb.castelobranco.features.admin.members.domain.model.Gender
import com.ipb.castelobranco.features.admin.members.domain.model.HistoryEditor
import com.ipb.castelobranco.features.admin.members.domain.model.HistoryEntry
import com.ipb.castelobranco.features.admin.members.domain.model.MemberChanges
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import com.ipb.castelobranco.features.admin.members.domain.model.MemberOptions
import com.ipb.castelobranco.features.admin.members.domain.model.MemberRecord
import com.ipb.castelobranco.features.admin.members.domain.model.MemberSummary
import com.ipb.castelobranco.features.admin.members.domain.model.NamedRef
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.LocalDate

fun NamedRefDto.toDomain(): NamedRef = NamedRef(id = id, name = name)

fun MemberSummaryDto.toDomain(): MemberSummary = MemberSummary(
    id = id,
    name = name,
    photoUrl = photoUrl?.ifBlank { null },
    status = status?.toDomain(),
    isValid = isActive,
)

fun MemberRecordDto.toDomain(): MemberRecord = MemberRecord(
    id = id,
    name = name,
    firstName = firstName,
    lastName = lastName,
    birthDate = birthDate.toLocalDateOrNull(),
    gender = Gender.fromApiCode(gender),
    status = status?.toDomain(),
    role = role?.toDomain(),
    ministries = ministries.map { it.toDomain() },
    baptismDate = baptismDate.toLocalDateOrNull(),
    isValid = isActive,
    photoUrl = photoUrl?.ifBlank { null },
    createdAt = Instant.parse(createdAt),
)

fun MemberOptionsDto.toDomain(): MemberOptions = MemberOptions(
    statuses = statuses.map { it.toDomain() },
    roles = roles.map { it.toDomain() },
    ministries = ministries.map { it.toDomain() },
)

fun HistoryEntryDto.toDomain(): HistoryEntry = HistoryEntry(
    id = id,
    editor = editor?.let { HistoryEditor(id = it.id, name = it.name) },
    field = field,
    oldValue = oldValue,
    newValue = newValue,
    changedAt = Instant.parse(changedAt),
)

/**
 * The write body. Built by hand instead of from a DTO because the shared `Json` drops null
 * properties, and a cleared field has to reach the server as `null`.
 */
fun MemberChanges.toJsonObject(): JsonObject = JsonObject(
    values.entries.associate { (field, value) -> field.apiKey to field.encode(value) }
)

private fun MemberField.encode(value: Any?): JsonElement {
    if (value == null) return JsonNull
    return when (this) {
        MemberField.NAME, MemberField.FIRST_NAME, MemberField.LAST_NAME -> JsonPrimitive(value as String)
        MemberField.BIRTH_DATE, MemberField.BAPTISM_DATE -> JsonPrimitive((value as LocalDate).toApiDate())
        MemberField.GENDER -> JsonPrimitive((value as Gender).apiCode)
        MemberField.STATUS, MemberField.ROLE -> JsonPrimitive(value as Int)
        MemberField.MINISTRIES -> JsonArray((value as Set<*>).map { it as Int }.sorted().map(::JsonPrimitive))
        MemberField.IS_VALID -> JsonPrimitive(value as Boolean)
    }
}

/** ISO `YYYY-MM-DD`; `LocalDate` pads the year to four digits, so 0001 stays `0001`. */
private fun LocalDate.toApiDate(): String = toString()

private fun String?.toLocalDateOrNull(): LocalDate? =
    this?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
