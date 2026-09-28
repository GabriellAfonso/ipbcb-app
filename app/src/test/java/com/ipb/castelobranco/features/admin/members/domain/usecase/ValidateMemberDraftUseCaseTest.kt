package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.features.admin.members.domain.model.BirthDate
import com.ipb.castelobranco.features.admin.members.domain.model.MemberDraft
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberDraftUseCase.Companion.BAPTISM_BEFORE_BIRTH
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberDraftUseCase.Companion.FUTURE_DATE
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberDraftUseCase.Companion.INVALID_YEAR
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberDraftUseCase.Companion.NAME_REQUIRED
import com.ipb.castelobranco.features.admin.members.domain.usecase.ValidateMemberDraftUseCase.Companion.NO_SUCH_DAY
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
        birth = BirthDate(day = 2, month = 4, year = 1990),
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
            valid.copy(birth = BirthDate(27, 9, 2026), baptismDate = LocalDate.of(2030, 1, 1))
        )

        assertEquals(FUTURE_DATE, errors[MemberField.BIRTH_YEAR])
        assertEquals(FUTURE_DATE, errors[MemberField.BAPTISM_DATE])
    }

    @Test
    fun `a year after the current one or before 1 is refused`() {
        assertEquals(FUTURE_DATE, validate(valid.copy(birth = BirthDate(year = 2027)))[MemberField.BIRTH_YEAR])
        assertEquals(INVALID_YEAR, validate(valid.copy(birth = BirthDate(year = 0)))[MemberField.BIRTH_YEAR])
    }

    @Test
    fun `29 February needs no year or a leap year`() {
        assertTrue(validate(valid.copy(birth = BirthDate(29, 2, null))).isEmpty())
        assertTrue(validate(valid.copy(birth = BirthDate(29, 2, 1988))).isEmpty())
        assertEquals(NO_SUCH_DAY, validate(valid.copy(birth = BirthDate(29, 2, 1990)))[MemberField.BIRTH_DAY])
    }

    @Test
    fun `baptism before a full birth date is refused`() {
        val errors = validate(valid.copy(baptismDate = LocalDate.of(1989, 1, 1)))

        assertEquals(mapOf(MemberField.BAPTISM_DATE to BAPTISM_BEFORE_BIRTH), errors)
    }

    @Test
    fun `with the year only, baptism is compared by year`() {
        val yearOnly = valid.copy(birth = BirthDate(year = 1990))

        assertTrue(validate(yearOnly.copy(baptismDate = LocalDate.of(1990, 1, 1))).isEmpty())
        assertEquals(
            BAPTISM_BEFORE_BIRTH,
            validate(yearOnly.copy(baptismDate = LocalDate.of(1989, 12, 31)))[MemberField.BAPTISM_DATE],
        )
    }

    @Test
    fun `day and month only are never future and never compared with baptism`() {
        val errors = validate(
            valid.copy(birth = BirthDate(31, 12, null), baptismDate = LocalDate.of(1990, 1, 1))
        )

        assertTrue(errors.isEmpty())
    }
}
