package com.ipb.castelobranco.features.profile.presentation.format

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val JOINED_SINCE_FORMATTER: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMMM 'de' yyyy", Locale.forLanguageTag("pt-BR"))

/** "março de 2025": the month in full, lowercase, as Portuguese writes it. */
fun formatJoinedSince(date: LocalDate): String = JOINED_SINCE_FORMATTER.format(date).lowercase(Locale.ROOT)
