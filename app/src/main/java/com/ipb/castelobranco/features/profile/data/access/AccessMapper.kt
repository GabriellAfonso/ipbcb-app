package com.ipb.castelobranco.features.profile.data.access

import com.ipb.castelobranco.core.domain.access.Access
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import com.ipb.castelobranco.features.profile.data.dto.MeProfileDto

/**
 * Turns the profile's raw `roles` and `permissions` into [Access], denying whatever it does not
 * know: an unknown role, scope or level is dropped. `hasAnyRole` still counts unknown roles, so the
 * panel entry appears for them — with no card to show.
 */
fun MeProfileDto.toAccess(): Access = Access(
    roles = roles.mapNotNull { Role.fromWire(it.id) }.toSet(),
    hasAnyRole = roles.isNotEmpty(),
    levels = permissions.mapNotNull { (key, value) ->
        val scope = Scope.fromWire(key) ?: return@mapNotNull null
        val level = AccessLevel.fromWire(value) ?: return@mapNotNull null
        scope to level
    }.toMap(),
)
