package com.ipb.castelobranco.features.admin.panel.domain

import com.ipb.castelobranco.core.domain.access.Access
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope

/** What the user needs for a panel card to appear. */
sealed interface CardRequirement {

    fun isMetBy(access: Access): Boolean

    data class AtLeast(val scope: Scope, val level: AccessLevel) : CardRequirement {
        override fun isMetBy(access: Access) = access.allows(scope, level)
    }

    /** For cards whose area has no scope in the backend. */
    data class HasRole(val role: Role) : CardRequirement {
        override fun isMetBy(access: Access) = access.holds(role)
    }
}
