package com.ipb.castelobranco.core.presentation.screens

import com.ipb.castelobranco.features.schedule.presentation.components.ScheduleBandUi
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val HEADER_LOCALE = Locale.forLanguageTag("pt-BR")
private const val WEEKDAY_SUFFIX = "-feira"
private const val MORNING_START_HOUR = 5
private const val AFTERNOON_START_HOUR = 12
private const val EVENING_START_HOUR = 18
private const val GOOD_MORNING = "Bom dia"
private const val GOOD_AFTERNOON = "Boa tarde"
private const val GOOD_EVENING = "Boa noite"
internal const val VISITOR_GREETING = "Bem-vindo à IPB Castelo Branco"

/**
 * The home's green band (`specs/core/spec.md` §6.3), already formatted.
 *
 * @param schedule the schedule card; null hides it. Ignored for a visitor, who sees the login button instead.
 */
data class HomeHeaderUi(
    val dateLabel: String,
    val greeting: String,
    val isLoggedIn: Boolean,
    val schedule: ScheduleBandUi?,
)

fun buildHomeHeader(
    today: LocalDate,
    hour: Int,
    isLoggedIn: Boolean,
    firstName: String?,
    schedule: ScheduleBandUi?,
): HomeHeaderUi = HomeHeaderUi(
    dateLabel = homeDateLabel(today),
    greeting = when {
        !isLoggedIn -> VISITOR_GREETING
        firstName.isNullOrBlank() -> greetingFor(hour)
        else -> "${greetingFor(hour)}, $firstName"
    },
    isLoggedIn = isLoggedIn,
    schedule = schedule.takeIf { isLoggedIn },
)

internal fun greetingFor(hour: Int): String = when (hour) {
    in MORNING_START_HOUR until AFTERNOON_START_HOUR -> GOOD_MORNING
    in AFTERNOON_START_HOUR until EVENING_START_HOUR -> GOOD_AFTERNOON
    else -> GOOD_EVENING
}

/** "Quarta, 28 de outubro". */
internal fun homeDateLabel(date: LocalDate): String {
    val weekday = date.dayOfWeek.getDisplayName(TextStyle.FULL, HEADER_LOCALE).removeSuffix(WEEKDAY_SUFFIX)
        .replaceFirstChar { it.titlecase(HEADER_LOCALE) }
    val month = date.month.getDisplayName(TextStyle.FULL, HEADER_LOCALE)
    return "$weekday, ${date.dayOfMonth} de $month"
}
