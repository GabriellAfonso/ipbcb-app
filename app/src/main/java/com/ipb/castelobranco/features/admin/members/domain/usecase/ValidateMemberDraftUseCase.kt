package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.features.admin.members.domain.model.BirthDate
import com.ipb.castelobranco.features.admin.members.domain.model.MemberDraft
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import java.time.LocalDate
import java.time.MonthDay
import javax.inject.Inject

/**
 * The server's write rules, checked before sending so an impossible member never becomes a
 * request. Anything the server still refuses comes back as `field_errors` on the same fields.
 *
 * The birth date is checked part by part, as the server does: a day and month only are never
 * "in the future" and cannot be compared with the baptism; a year only is compared by year.
 */
class ValidateMemberDraftUseCase @Inject constructor(
    private val dateProvider: DateProvider,
) {

    operator fun invoke(draft: MemberDraft): Map<MemberField, String> {
        val errors = linkedMapOf<MemberField, String>()
        val today = dateProvider.today()

        when {
            draft.name.isBlank() -> errors[MemberField.NAME] = NAME_REQUIRED
            draft.name.trim().length > MAX_NAME_LENGTH -> errors[MemberField.NAME] = TOO_LONG
        }
        if (draft.firstName.trim().length > MAX_NAME_LENGTH) errors[MemberField.FIRST_NAME] = TOO_LONG
        if (draft.lastName.trim().length > MAX_NAME_LENGTH) errors[MemberField.LAST_NAME] = TOO_LONG

        errors += birthErrors(draft.birth, today)
        val baptism = draft.baptismDate
        when {
            baptism != null && baptism.isAfter(today) -> errors[MemberField.BAPTISM_DATE] = FUTURE_DATE
            baptism != null && isBeforeBirth(baptism, draft.birth) ->
                errors[MemberField.BAPTISM_DATE] = BAPTISM_BEFORE_BIRTH
        }
        return errors
    }

    private fun birthErrors(birth: BirthDate, today: LocalDate): Map<MemberField, String> {
        val (day, month, year) = birth
        return when {
            (day == null) != (month == null) -> mapOf(MemberField.BIRTH_DAY to BIRTHDAY_INCOMPLETE)
            year != null && year < 1 -> mapOf(MemberField.BIRTH_YEAR to INVALID_YEAR)
            year != null && year > today.year -> mapOf(MemberField.BIRTH_YEAR to FUTURE_DATE)
            day != null && month != null && !isRealDate(day, month, year) ->
                mapOf(MemberField.BIRTH_DAY to NO_SUCH_DAY)
            birth.fullDate()?.isAfter(today) == true -> mapOf(MemberField.BIRTH_YEAR to FUTURE_DATE)
            else -> emptyMap()
        }
    }

    /** 29/02 exists with no year (it falls in some leap year) or in a leap year. */
    private fun isRealDate(day: Int, month: Int, year: Int?): Boolean = runCatching {
        if (year == null) MonthDay.of(month, day) else LocalDate.of(year, month, day)
    }.isSuccess

    private fun isBeforeBirth(baptism: LocalDate, birth: BirthDate): Boolean {
        val full = birth.fullDate()
        val year = birth.year
        return when {
            full != null -> baptism.isBefore(full)
            !birth.hasBirthday && year != null -> baptism.year < year
            else -> false
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 255
        const val NAME_REQUIRED = "Informe o nome."
        const val TOO_LONG = "Use no máximo 255 caracteres."
        const val FUTURE_DATE = "A data não pode estar no futuro."
        const val BAPTISM_BEFORE_BIRTH = "O batismo não pode ser antes do nascimento."
        const val BIRTHDAY_INCOMPLETE = "Informe o dia e o mês."
        const val INVALID_YEAR = "Ano de nascimento inválido."
        const val NO_SUCH_DAY = "Esse dia não existe nesse mês e ano."
    }
}
