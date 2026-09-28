package com.ipb.castelobranco.features.admin.members.presentation.util

import com.ipb.castelobranco.core.domain.util.normalize
import com.ipb.castelobranco.features.admin.members.domain.model.BirthDate
import com.ipb.castelobranco.features.admin.members.domain.model.NamedRef
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

const val NOT_INFORMED = "Não informado"
const val NO_STATUS = "Sem situação"
const val NO_ROLE = "Sem cargo"
const val NO_MINISTRY = "Nenhum ministério"
const val AGE_UNKNOWN = "Desconhecida"

private val DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val WHITESPACE = Regex("[ \t\n]+")
private val DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

/** "AS" for "Ana Souza"; one letter for a single name. */
fun initialsOf(name: String): String {
    val parts = name.trim().split(WHITESPACE).filter { it.isNotBlank() }
    if (parts.isEmpty()) return ""
    val first = parts.first().take(1)
    val last = if (parts.size > 1) parts.last().take(1) else ""
    return (first + last).uppercase()
}

/** Accent-, case- and punctuation-insensitive form used by the name search. */
fun normalizeForSearch(text: String): String = text.normalize().lowercase().trim()

fun formatDate(date: LocalDate): String = date.format(DATE)

/** "12/03/1990", "12/03" (year unknown), "1990" (year only), or null when nothing is known. */
fun formatBirth(birth: BirthDate): String? {
    val (day, month, year) = birth
    val dayMonth = if (day != null && month != null) "%02d/%02d".format(day, month) else null
    return when {
        dayMonth != null && year != null -> "$dayMonth/$year"
        dayMonth != null -> dayMonth
        year != null -> year.toString()
        else -> null
    }
}

fun formatDateTime(instant: Instant, zone: ZoneId = ZoneId.systemDefault()): String =
    instant.atZone(zone).format(DATE_TIME)

fun statusLabel(status: NamedRef?): String = status?.name ?: NO_STATUS
