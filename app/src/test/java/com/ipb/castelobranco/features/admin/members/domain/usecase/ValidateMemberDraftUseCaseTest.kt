package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.features.admin.members.domain.model.MemberDraft
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberDraftUseCase.Companion.BAPTISM_BEFORE_BIRTH
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberDraftUseCase.Companion.FUTURE_DATE
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberDraftUseCase.Companion.NAME_REQUIRED
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberDraftUseCase.Companion.TOO_LONG
import com.ipb.castelobranco.features.admin.members.fixedDateProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ValidateMemberDraftUseCaseTest {

    private val validate = ValidateMemberDraftUseCase(fixedDateProvider) // today = 26/09/2026
    private val valid = MemberDraft(
        name = "Ana Souza",
        birthDate = LocalDate.of(1990, 4, 2),
        baptismDate = LocalDate.of(2005, 6, 12),
    )

    @Test
    fun `a valid draft has no errors`() {
        assertTrue(validate(valid).isEmpty())
    }

    @Test
    fun `blank name is required`() {
        assertEquals(mapOf(MemberField.NAME to NAME_REQUIRED), validate(valid.copy(name = "   ")))
    }

    @Test
    fun `names over 255 characters are refused`() {
        val long = "a".repeat(256)

        val errors = validate(valid.copy(name = long, firstName = long, lastName = long))

        assertEquals(TOO_LONG, errors[MemberField.NAME])
        assertEquals(TOO_LONG, errors[MemberField.FIRST_NAME])
        assertEquals(TOO_LONG, errors[MemberField.LAST_NAME])
    }

    @Test
    fun `future dates are refused`() {
        val errors = validate(
            valid.copy(birthDate = LocalDate.of(2026, 9, 27), baptismDate = LocalDate.of(2030, 1, 1))
        )

        assertEquals(FUTURE_DATE, errors[MemberField.BIRTH_DATE])
        assertEquals(FUTURE_DATE, errors[MemberField.BAPTISM_DATE])
    }

    @Test
    fun `baptism before birth is refused`() {
        val errors = validate(valid.copy(baptismDate = LocalDate.of(1989, 1, 1)))

        assertEquals(mapOf(MemberField.BAPTISM_DATE to BAPTISM_BEFORE_BIRTH), errors)
    }

    @Test
    fun `birth year 0001 is year unknown - never future and never compared with baptism`() {
        val errors = validate(
            valid.copy(birthDate = LocalDate.of(1, 12, 31), baptismDate = LocalDate.of(1990, 1, 1))
        )

        assertTrue(errors.isEmpty())
    }
}
