package com.ipb.castelobranco.features.admin.members.domain.model

import java.time.LocalDate

/**
 * A birth date as far as it is known: the API's `birth_day`, `birth_month` and `birth_year`.
 * Day and month always go together; the year is independent. So a member has the full date,
 * the birthday only (year unknown), the year only, or nothing.
 */
data class BirthDate(
    val day: Int? = null,
    val month: Int? = null,
    val year: Int? = null,
) {
    val hasBirthday: Boolean get() = day != null && month != null

    val isEmpty: Boolean get() = day == null && month == null && year == null

    /** The calendar date when all three parts are known and form a real date. */
    fun fullDate(): LocalDate? {
        val (d, m, y) = this
        if (d == null || m == null || y == null) return null
        return runCatching { LocalDate.of(y, m, d) }.getOrNull()
    }

    companion object {
        val NONE = BirthDate()
    }
}
