package com.ipb.castelobranco.features.admin.members.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class MemberChangesTest {

    private val existing = MemberDraft(
        id = 12,
        name = "Ana Souza",
        firstName = "Ana",
        lastName = "Souza",
        birth = BirthDate(day = 2, month = 4, year = 1990),
        gender = Gender.FEMALE,
        statusId = 1,
        roleId = 3,
        ministryIds = setOf(2, 5),
        baptismDate = LocalDate.of(2005, 6, 12),
        isValid = true,
    )

    @Test
    fun `create sends only filled fields and never a default validity`() {
        val draft = MemberDraft(name = " Ana ", statusId = 1, ministryIds = setOf(2))

        val changes = draft.changesFrom(null)

        assertEquals(
            mapOf(
                MemberField.NAME to "Ana",
                MemberField.STATUS to 1,
                MemberField.MINISTRIES to setOf(2),
            ),
            changes.values,
        )
    }

    @Test
    fun `create sends validity only when the member starts invalid`() {
        val changes = MemberDraft(name = "Ana", isValid = false).changesFrom(null)

        assertEquals(false, changes.values[MemberField.IS_VALID])
    }

    @Test
    fun `edit sends only the fields that changed`() {
        val changes = existing.copy(statusId = 2).changesFrom(existing)

        assertEquals(mapOf(MemberField.STATUS to 2), changes.values)
    }

    @Test
    fun `clearing a field sends it as null`() {
        val changes = existing.copy(roleId = null).changesFrom(existing)

        assertTrue(changes.values.containsKey(MemberField.ROLE))
        assertEquals(mapOf(MemberField.ROLE to null), changes.values)
    }

    @Test
    fun `clearing the birth year sends only the year, as null`() {
        val changes = existing.copy(birth = existing.birth.copy(year = null)).changesFrom(existing)

        assertEquals(mapOf(MemberField.BIRTH_YEAR to null), changes.values)
    }

    @Test
    fun `changing the birthday sends only the parts that changed`() {
        val changes = existing.copy(birth = BirthDate(day = 15, month = 4, year = 1990)).changesFrom(existing)

        assertEquals(mapOf(MemberField.BIRTH_DAY to 15), changes.values)
    }

    @Test
    fun `clearing the birthday keeps the year out of the body`() {
        val changes = existing.copy(birth = BirthDate(year = 1990)).changesFrom(existing)

        assertEquals(mapOf(MemberField.BIRTH_DAY to null, MemberField.BIRTH_MONTH to null), changes.values)
    }

    @Test
    fun `a new member sends only the known birth parts`() {
        val changes = MemberDraft(name = "Ana", birth = BirthDate(year = 1990)).changesFrom(null)

        assertEquals(mapOf(MemberField.NAME to "Ana", MemberField.BIRTH_YEAR to 1990), changes.values)
    }

    @Test
    fun `whitespace around a name is not a change`() {
        val changes = existing.copy(name = "  Ana Souza ").changesFrom(existing)

        assertTrue(changes.isEmpty)
    }

    @Test
    fun `identical drafts produce no changes`() {
        assertTrue(existing.copy().changesFrom(existing).isEmpty)
    }
}
