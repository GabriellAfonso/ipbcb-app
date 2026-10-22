package com.ipb.castelobranco.features.admin.profiles.presentation.util

import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.admin.profiles.domain.model.RequestStatus
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val MONTH_YEAR: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", PT_BR)
private val DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", PT_BR)
private val BRASILIA: ZoneId = ZoneId.of("America/Sao_Paulo")

fun requestTypeLabel(type: AccessRequestType): String = when (type) {
    AccessRequestType.ACCESS -> "Pedido de acesso"
    AccessRequestType.DATA_CORRECTION -> "Pedido de correção"
}

/** An approved correction was resolved, not "approved": nothing was granted. */
fun requestStatusLabel(type: AccessRequestType, status: RequestStatus): String = when (status) {
    RequestStatus.PENDING -> "Pendente"
    RequestStatus.APPROVED -> if (type == AccessRequestType.DATA_CORRECTION) "Resolvida" else "Aprovado"
    RequestStatus.DENIED -> "Negado"
}

/** "No app desde março de 2025". */
fun joinedSinceLabel(date: LocalDate): String =
    "No app desde ${MONTH_YEAR.format(date).lowercase(Locale.ROOT)}"

/** "21/10/2026 às 13:05", in Brasília time. */
fun formatDateTime(value: OffsetDateTime): String = DATE_TIME.format(value.atZoneSameInstant(BRASILIA))

private const val HTTP_FORBIDDEN = 403
private const val HTTP_CONFLICT = 409

/** A read refused for lack of the `profiles` scope leaves the area; anything else is a message. */
fun Throwable.isAccessLost(): Boolean = toAppError().let { it is AppError.Auth && it.code == HTTP_FORBIDDEN }

/** Someone else changed the account meanwhile (already decided, member already linked): reload. */
fun Throwable.isConflict(): Boolean = toAppError().let { it is AppError.Server && it.code == HTTP_CONFLICT }

fun Throwable.message(): String = toAppError().toUserMessage()
