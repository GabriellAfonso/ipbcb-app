package com.ipb.castelobranco.core.domain.access

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessTest {

    // region wire parsing

    @Test
    fun `AccessLevel fromWire maps the three known levels`() {
        assertEquals(AccessLevel.VIEW, AccessLevel.fromWire("view"))
        assertEquals(AccessLevel.MANAGE, AccessLevel.fromWire("manage"))
        assertEquals(AccessLevel.OWNER, AccessLevel.fromWire("owner"))
    }

    @Test
    fun `AccessLevel fromWire denies anything it does not know`() {
        listOf(null, "", "OWNER", "admin", "superowner").forEach { value ->
            assertNull("\"$value\" must not become a level", AccessLevel.fromWire(value))
        }
    }

    @Test
    fun `Scope fromWire maps every scope including the dotted report key`() {
        assertEquals(Scope.MEMBERS, Scope.fromWire("members"))
        assertEquals(Scope.SCHEDULE, Scope.fromWire("schedule"))
        assertEquals(Scope.SONGS, Scope.fromWire("songs"))
        assertEquals(Scope.GALLERY, Scope.fromWire("gallery"))
        assertEquals(Scope.EVENTS, Scope.fromWire("events"))
        assertEquals(Scope.NOTICES, Scope.fromWire("notices"))
        assertEquals(Scope.HYMNAL_HISTORY_REPORT, Scope.fromWire("reports.hymnal_history"))
        assertNull(Scope.fromWire("reports.attendance"))
    }

    @Test
    fun `Role fromWire maps the three roles and nothing else`() {
        assertEquals(Role.ADMIN, Role.fromWire("admin"))
        assertEquals(Role.LEADER, Role.fromWire("leader"))
        assertEquals(Role.MEDIA, Role.fromWire("media"))
        assertNull(Role.fromWire("member"))
    }

    // endregion

    // region decisions

    @Test
    fun `owner allows every level`() {
        val access = accessWith(Scope.MEMBERS to AccessLevel.OWNER)

        assertTrue(access.allows(Scope.MEMBERS, AccessLevel.VIEW))
        assertTrue(access.allows(Scope.MEMBERS, AccessLevel.MANAGE))
        assertTrue(access.allows(Scope.MEMBERS, AccessLevel.OWNER))
    }

    @Test
    fun `manage does not allow owner`() {
        val access = accessWith(Scope.MEMBERS to AccessLevel.MANAGE)

        assertTrue(access.allows(Scope.MEMBERS, AccessLevel.VIEW))
        assertTrue(access.allows(Scope.MEMBERS, AccessLevel.MANAGE))
        assertFalse(access.allows(Scope.MEMBERS, AccessLevel.OWNER))
    }

    @Test
    fun `a missing scope allows nothing`() {
        val access = accessWith(Scope.GALLERY to AccessLevel.MANAGE)

        assertFalse(access.allows(Scope.MEMBERS, AccessLevel.VIEW))
    }

    @Test
    fun `holds answers only for the roles present`() {
        val access = Access(roles = setOf(Role.ADMIN), hasAnyRole = true, levels = emptyMap())

        assertTrue(access.holds(Role.ADMIN))
        assertFalse(access.holds(Role.LEADER))
    }

    @Test
    fun `NONE has no role and allows nothing`() {
        assertFalse(Access.NONE.hasAnyRole)
        Scope.entries.forEach { scope ->
            assertFalse(Access.NONE.allows(scope, AccessLevel.VIEW))
        }
        Role.entries.forEach { role -> assertFalse(Access.NONE.holds(role)) }
    }

    // endregion

    private fun accessWith(vararg levels: Pair<Scope, AccessLevel>) =
        Access(roles = emptySet(), hasAnyRole = true, levels = levels.toMap())
}
