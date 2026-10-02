package com.ipb.castelobranco.features.profile.data.access

import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.domain.worship.WorshipAccess
import com.ipb.castelobranco.core.domain.worship.WorshipAccessRepository
import com.ipb.castelobranco.features.profile.data.snapshot.ProfileSnapshotRepository
import com.ipb.castelobranco.features.profile.domain.model.MeProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** The worship flags come from the same `/me` snapshot as the access, so they follow every profile read. */
@Singleton
class ProfileWorshipAccessRepository @Inject constructor(
    private val profileSnapshot: ProfileSnapshotRepository,
) : WorshipAccessRepository {

    override val worshipAccess: Flow<WorshipAccess> = profileSnapshot.observe()
        .map { it.toWorshipAccess() }
        .distinctUntilChanged()

    override suspend fun current(): WorshipAccess {
        if (profileSnapshot.getCurrentState() !is SnapshotState.Data) profileSnapshot.preload()
        return profileSnapshot.getCurrentState().toWorshipAccess()
    }

    private fun SnapshotState<MeProfile>.toWorshipAccess(): WorshipAccess {
        val profile = (this as? SnapshotState.Data)?.value ?: return WorshipAccess.NONE
        return WorshipAccess(isWorshipMember = profile.isWorshipMember, canSaveSetlist = profile.canSaveSetlist)
    }
}
