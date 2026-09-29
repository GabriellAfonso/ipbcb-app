package com.ipb.castelobranco.core.domain.access

/**
 * What the signed-in user may do in the management areas, as the profile last reported it. A UI
 * filter only: the backend authorizes every request.
 *
 * @param roles the known roles; unknown ids are dropped.
 * @param hasAnyRole the profile listed at least one role, known or not — the panel entry follows it.
 * @param levels the level per known scope; a scope absent from the map has no access.
 */
data class Access(
    val roles: Set<Role>,
    val hasAnyRole: Boolean,
    val levels: Map<Scope, AccessLevel>,
) {
    fun allows(scope: Scope, minimum: AccessLevel): Boolean =
        levels[scope]?.let { it >= minimum } == true

    fun holds(role: Role): Boolean = role in roles

    companion object {
        /** No profile yet, a failed load, or a signed-out user. */
        val NONE = Access(roles = emptySet(), hasAnyRole = false, levels = emptyMap())
    }
}
