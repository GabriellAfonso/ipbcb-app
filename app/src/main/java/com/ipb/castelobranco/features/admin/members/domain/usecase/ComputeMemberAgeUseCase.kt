package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.features.admin.members.domain.model.BirthDate
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

/**
 * "36 anos" and "há 21 anos", counted on the church's current day. A missing or future date gives
 * nothing. With only the birth year the age is the current year minus it (the profile already
 * shows the date has no day); with only day and month there is no age.
 */
class ComputeMemberAgeUseCase @Inject constructor(
    private val dateProvider: DateProvider,
) {

    fun ageLabel(birth: BirthDate): String? {
        val full = birth.fullDate()
        if (full != null) return yearsUntilToday(full)?.let(::yearsText)
        if (birth.hasBirthday || birth.year == null) return null
        val years = dateProvider.today().year - birth.year
        return if (years < 0) null else yearsText(years)
    }

    private fun yearsText(years: Int): String = if (years == 1) "1 ano" else "$years anos"

    fun sinceLabel(date: LocalDate?): String? {
        val years = date?.let(::yearsUntilToday) ?: return null
        return when (years) {
            0 -> "este ano"
            1 -> "há 1 ano"
            else -> "há $years anos"
        }
    }

    private fun yearsUntilToday(date: LocalDate): Int? {
        val today = dateProvider.today()
        if (date.isAfter(today)) return null
        return Period.between(date, today).years
    }
}
