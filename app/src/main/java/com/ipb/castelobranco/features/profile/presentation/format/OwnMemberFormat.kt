package com.ipb.castelobranco.features.profile.presentation.format

import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val PT_BR: Locale = Locale.forLanguageTag("pt-BR")
private val BAPTISM_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/** "14 de março"; `null` when the day or month is unknown or impossible. */
fun formatBirthday(day: Int?, month: Int?): String? {
    if (day == null || month !in 1..12 || day !in 1..31) return null
    val monthName = Month.of(month!!).getDisplayName(TextStyle.FULL, PT_BR).lowercase(Locale.ROOT)
    return "$day de $monthName"
}

fun formatBaptism(date: LocalDate): String = BAPTISM_FORMATTER.format(date)

fun formatGender(code: String?): String? = when (code) {
    "F" -> "Feminino"
    "M" -> "Masculino"
    else -> null
}
