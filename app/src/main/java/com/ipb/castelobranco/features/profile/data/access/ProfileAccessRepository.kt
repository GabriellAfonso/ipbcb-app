package com.ipb.castelobranco.features.profile.data.access

import com.ipb.castelobranco.core.domain.access.Access
import com.ipb.castelobranco.core.domain.access.AccessRepository
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.features.profile.data.snapshot.ProfileSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Access comes from the same `/me` snapshot as the rest of the profile, so it is on screen from
 * the disk preload at boot and follows every refresh.
 */
@Singleton
class ProfileAccessRepository @Inject constructor(
    private val profileSnapshot: ProfileSnapshotRepository,
) : AccessRepository {

    private val refreshing = Mutex()

    override val access: Flow<Access> = profileSnapshot.observe()
        .map { state -> (state as? SnapshotState.Data)?.value?.access ?: Access.NONE }
        .distinctUntilChanged()

    // Several refused requests in a row need a single profile read, not one each.
    override suspend fun refresh() {
        if (!refreshing.tryLock()) return
        try {
            profileSnapshot.refresh()
        } finally {
            refreshing.unlock()
        }
    }
}
