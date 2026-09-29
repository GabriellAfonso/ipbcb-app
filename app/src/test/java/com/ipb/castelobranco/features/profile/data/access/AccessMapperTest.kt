package com.ipb.castelobranco.features.profile.data.access

import com.ipb.castelobranco.core.domain.access.Access
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto
import com.ipb.castelobranco.features.profile.data.dto.RoleDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessMapperTest {

    @Test
    fun `admin is owner of every scope`() {
        val access = profile(
            roles = listOf("admin"),
            permissions = ALL_SCOPES.associateWith { "owner" },
        ).toAccess()

        assertTrue(access.holds(Role.ADMIN))
        Scope.entries.forEach { scope -> assertEquals(AccessLevel.OWNER, access.levels[scope]) }
    }

    @Test
    fun `leader never gets owner`() {
        val access = profile(roles = listOf("leader"), permissions = LEADER).toAccess()

        assertEquals(AccessLevel.MANAGE, access.levels[Scope.MEMBERS])
        assertEquals(AccessLevel.VIEW, access.levels[Scope.HYMNAL_HISTORY_REPORT])
        assertFalse(access.levels.containsValue(AccessLevel.OWNER))
        assertTrue(access.holds(Role.LEADER))
    }

    @Test
    fun `media has no level on members`() {
        val access = profile(roles = listOf("media"), permissions = MEDIA).toAccess()

        assertFalse(access.levels.containsKey(Scope.MEMBERS))
        assertEquals(AccessLevel.MANAGE, access.levels[Scope.GALLERY])
    }

    @Test
    fun `no role and all null is the same as NONE`() {
        val access = profile(roles = emptyList(), permissions = ALL_SCOPES.associateWith { null }).toAccess()

        assertEquals(Access.NONE, access)
    }

    @Test
    fun `an unknown role still counts as having a role`() {
        val access = profile(roles = listOf("treasurer"), permissions = emptyMap()).toAccess()

        assertTrue(access.hasAnyRole)
        assertTrue(access.roles.isEmpty())
    }

    @Test
    fun `unknown levels and unknown keys are dropped`() {
        val access = profile(
            roles = listOf("leader"),
            permissions = mapOf("songs" to "superowner", "reports.attendance" to "view", "members" to "view"),
        ).toAccess()

        assertEquals(mapOf(Scope.MEMBERS to AccessLevel.VIEW), access.levels)
    }

    private fun profile(roles: List<String>, permissions: Map<String, String?>) = MeProfileDto(
        name = "Ana",
        isMember = true,
        roles = roles.map { RoleDto(id = it, name = it) },
        permissions = permissions,
    )

    private companion object {
        val ALL_SCOPES = listOf(
            "members", "schedule", "songs", "gallery", "events", "notices", "reports.hymnal_history",
        )
        val LEADER = mapOf(
            "members" to "manage", "schedule" to "manage", "songs" to "manage", "gallery" to "manage",
            "events" to "manage", "notices" to "manage", "reports.hymnal_history" to "view",
        )
        val MEDIA = mapOf(
            "members" to null, "schedule" to null, "songs" to null, "gallery" to "manage",
            "events" to "manage", "notices" to "manage", "reports.hymnal_history" to "view",
        )
    }
}
