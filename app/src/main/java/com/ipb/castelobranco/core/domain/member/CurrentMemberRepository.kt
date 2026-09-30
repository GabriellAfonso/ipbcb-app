package com.ipb.castelobranco.core.domain.member

import kotlinx.coroutines.flow.Flow

/**
 * The member record an admin linked to the signed-in user. Lives in core so the gallery reads it
 * without importing the profile feature, which implements it. Not a permission: kept apart from
 * `Access`.
 */
interface CurrentMemberRepository {

    /** The linked member's id; `null` when not linked or there is no profile data. Emits only on change. */
    val memberId: Flow<Long?>
}
