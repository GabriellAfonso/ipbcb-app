package com.ipb.castelobranco.core.presentation.format

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/**
 * The dates of a church event, as the carousel, the detail and the management list show them
 * (`specs/events/spec.md` §2). Month and weekday names are spelled here instead of coming from the
 * device locale, so the labels read the same on every phone and in tests.
 */
object EventDateLabel {

    private val SHORT_MONTHS = listOf(
        "jan", "fev", "mar", "abr", "mai", "jun", "jul", "ago", "set", "out", "nov", "dez",
    )
    private val FULL_MONTHS = listOf(
        "janeiro", "fevereiro", "março", "abril", "maio", "junho",
        "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
    )
    private val WEEKDAYS = mapOf(
        DayOfWeek.MONDAY to "Segunda-feira",
        DayOfWeek.TUESDAY to "Terça-feira",
        DayOfWeek.WEDNESDAY to "Quarta-feira",
        DayOfWeek.THURSDAY to "Quinta-feira",
        DayOfWeek.FRIDAY to "Sexta-feira",
        DayOfWeek.SATURDAY to "Sábado",
        DayOfWeek.SUNDAY to "Domingo",
    )
    private const val TIME_SEPARATOR = " · "

    /** "14 nov", "14 nov · 19:30", "14 a 16 nov", "30 nov a 2 dez", "30 dez 2026 a 2 jan 2027". */
    fun dateLabel(start: LocalDate, end: LocalDate?, time: LocalTime?): String {
        val dates = if (isOneDay(start, end)) short(start) else range(start, end!!, ::short)
        return if (time == null) dates else dates + TIME_SEPARATOR + timeLabel(time)
    }

    /** "Sábado, 14 de novembro", "14 a 16 de novembro", "30 de novembro a 2 de dezembro". */
    fun fullDateLabel(start: LocalDate, end: LocalDate?): String =
        if (isOneDay(start, end)) {
            "${WEEKDAYS.getValue(start.dayOfWeek)}, ${full(start)}"
        } else {
            range(start, end!!, ::full)
        }

    fun timeLabel(time: LocalTime): String = "%02d:%02d".format(time.hour, time.minute)

    private fun isOneDay(start: LocalDate, end: LocalDate?) = end == null || end == start

    private fun short(date: LocalDate) = "${date.dayOfMonth} ${SHORT_MONTHS[date.monthValue - 1]}"

    private fun full(date: LocalDate) = "${date.dayOfMonth} de ${FULL_MONTHS[date.monthValue - 1]}"

    /** Same month: the month is said once. Across years: both ends carry their year. */
    private fun range(start: LocalDate, end: LocalDate, format: (LocalDate) -> String): String = when {
        start.year != end.year -> "${format(start)} ${start.year} a ${format(end)} ${end.year}"
        start.month != end.month -> "${format(start)} a ${format(end)}"
        else -> "${start.dayOfMonth} a ${format(end)}"
    }
}
