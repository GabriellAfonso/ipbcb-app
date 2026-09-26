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
        birthDate = LocalDate.of(1990, 4, 2),
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
        val changes = existing.copy(roleId = null, birthDate = null).changesFrom(existing)

        assertTrue(changes.values.containsKey(MemberField.ROLE))
        assertEquals(null, changes.values[MemberField.ROLE])
        assertEquals(null, changes.values[MemberField.BIRTH_DATE])
        assertEquals(2, changes.values.size)
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
