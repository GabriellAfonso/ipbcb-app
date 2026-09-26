package com.ipb.castelobranco.features.admin.members.domain.model

import java.time.Instant
import java.time.LocalDate

/** The full member record. Empty [firstName]/[lastName] mean "not informed". */
data class MemberRecord(
    val id: Int,
    val name: String,
    val firstName: String,
    val lastName: String,
    val birthDate: LocalDate?,
    val gender: Gender?,
    val status: NamedRef?,
    val role: NamedRef?,
    val ministries: List<NamedRef>,
    val baptismDate: LocalDate?,
    val isValid: Boolean,
    val photoUrl: String?,
    val createdAt: Instant,
)

fun MemberRecord.toSummary(): MemberSummary = MemberSummary(
    id = id,
    name = name,
    photoUrl = photoUrl,
    status = status,
    isValid = isValid,
)

fun MemberRecord.toDraft(): MemberDraft = MemberDraft(
    id = id,
    name = name,
    firstName = firstName,
    lastName = lastName,
    birthDate = birthDate,
    gender = gender,
    statusId = status?.id,
    roleId = role?.id,
    ministryIds = ministries.map { it.id }.toSet(),
    baptismDate = baptismDate,
    isValid = isValid,
)
