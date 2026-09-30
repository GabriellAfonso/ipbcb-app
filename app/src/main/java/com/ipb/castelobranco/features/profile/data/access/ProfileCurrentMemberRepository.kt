package com.ipb.castelobranco.features.profile.data.access

import com.ipb.castelobranco.core.domain.member.CurrentMemberRepository
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.profile.data.snapshot.ProfileSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The member id comes from the same `/me` snapshot as the access, so it follows every profile read. */
@Singleton
class ProfileCurrentMemberRepository @Inject constructor(
    profileSnapshot: ProfileSnapshotRepository,
) : CurrentMemberRepository {

    override val memberId: Flow<Long?> = profileSnapshot.observe()
        .map { state -> (state as? SnapshotState.Data)?.value?.memberId }
        .distinctUntilChanged()
}
