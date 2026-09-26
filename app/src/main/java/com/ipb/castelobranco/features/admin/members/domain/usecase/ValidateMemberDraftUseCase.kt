package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.core.domain.util.DateProvider
import com.ipb.castelobranco.features.admin.members.domain.model.MemberDraft
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import com.ipb.castelobranco.features.admin.members.domain.model.hasUnknownYear
import javax.inject.Inject

/**
 * The server's write rules, checked before sending so an impossible member never becomes a
 * request. Anything the server still refuses comes back as `field_errors` on the same fields.
 *
 * A birth date in year 0001 is the church's "year unknown": it is never "in the future" and the
 * baptism-before-birth rule cannot be judged against it.
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

        val birth = draft.birthDate
        val baptism = draft.baptismDate
        if (birth != null && !birth.hasUnknownYear() && birth.isAfter(today)) {
            errors[MemberField.BIRTH_DATE] = FUTURE_DATE
        }
        when {
            baptism != null && baptism.isAfter(today) -> errors[MemberField.BAPTISM_DATE] = FUTURE_DATE
            baptism != null && birth != null && !birth.hasUnknownYear() && baptism.isBefore(birth) ->
                errors[MemberField.BAPTISM_DATE] = BAPTISM_BEFORE_BIRTH
        }
        return errors
    }

    companion object {
        const val MAX_NAME_LENGTH = 255
        const val NAME_REQUIRED = "Informe o nome."
        const val TOO_LONG = "Use no máximo 255 caracteres."
        const val FUTURE_DATE = "A data não pode estar no futuro."
        const val BAPTISM_BEFORE_BIRTH = "O batismo não pode ser antes do nascimento."
    }
}
