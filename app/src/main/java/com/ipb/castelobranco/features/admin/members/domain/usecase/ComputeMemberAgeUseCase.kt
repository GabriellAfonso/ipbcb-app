package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.features.admin.members.domain.model.hasUnknownYear
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

/**
 * "36 anos" and "há 21 anos", counted on the church's current day. A missing or future date gives
 * nothing, and so does a birth date in year 0001 — the church's "year unknown".
 */
class ComputeMemberAgeUseCase @Inject constructor(
    private val dateProvider: DateProvider,
) {

    fun ageLabel(birthDate: LocalDate?): String? {
        if (birthDate == null || birthDate.hasUnknownYear()) return null
        val years = yearsUntilToday(birthDate) ?: return null
        return if (years == 1) "1 ano" else "$years anos"
    }

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
