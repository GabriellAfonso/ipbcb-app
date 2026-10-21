package com.ipb.castelobranco.features.admin.events.data.mapper

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.features.admin.events.data.dto.EventRecordDto
import com.ipb.castelobranco.features.admin.events.domain.model.EventRecord
import com.ipb.castelobranco.features.admin.events.domain.model.EventVisibility
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

private const val BAD_DATE = "Evento com data inválida"

/** A record without a readable start date is a broken answer, not something the form can show. */
fun EventRecordDto.toDomain(): EventRecord = EventRecord(
    id = id,
    title = title,
    startDate = runCatching { LocalDate.parse(startDate) }.getOrElse { throw AppError.Unknown(message = BAD_DATE) },
    endDate = endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
    startTime = startTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() },
    location = location?.takeIf { it.isNotBlank() },
    description = description?.takeIf { it.isNotBlank() },
    imageUrl = imageUrl,
    visibility = EventVisibility.fromWire(visibility),
    createdByName = createdBy?.name?.takeIf { it.isNotBlank() },
    wasEdited = updatedBy != null,
    updatedByName = updatedBy?.name?.takeIf { it.isNotBlank() },
    createdAt = createdAt?.let(::parseInstant),
    updatedAt = updatedAt?.let(::parseInstant),
)

private fun parseInstant(value: String): Instant? =
    runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
