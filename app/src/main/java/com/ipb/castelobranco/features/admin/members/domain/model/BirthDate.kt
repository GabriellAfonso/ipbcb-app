package com.ipb.castelobranco.features.admin.members.domain.model

import java.time.LocalDate

/**
 * The church records "birthday known, year unknown" as a birth date in the year 0001. This file is
 * the only place that knows the convention; age, display, validation and the form all ask
 * [hasUnknownYear]. 29 February cannot be recorded this way — 0001 is not a leap year.
 */
const val UNKNOWN_BIRTH_YEAR = 1

fun LocalDate.hasUnknownYear(): Boolean = year == UNKNOWN_BIRTH_YEAR

fun birthDateWithUnknownYear(month: Int, day: Int): LocalDate =
    LocalDate.of(UNKNOWN_BIRTH_YEAR, month, day)
