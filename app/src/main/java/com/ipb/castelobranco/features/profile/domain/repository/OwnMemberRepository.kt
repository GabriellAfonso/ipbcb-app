package com.ipb.castelobranco.features.profile.domain.repository

import com.ipb.castelobranco.features.profile.domain.model.OwnMember

interface OwnMemberRepository {
    /** `null` when the account is not linked to a roll member. Nothing is cached. */
    suspend fun get(): Result<OwnMember?>
}
