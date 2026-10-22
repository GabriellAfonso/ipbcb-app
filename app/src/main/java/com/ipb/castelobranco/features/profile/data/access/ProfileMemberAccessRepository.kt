package com.ipb.castelobranco.features.profile.data.access

import com.ipb.castelobranco.core.domain.access.AccessRepository
import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.domain.access.MemberAccess
import com.ipb.castelobranco.core.domain.access.MemberAccessRepository
import com.ipb.castelobranco.core.domain.error.mapError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.network.error.toAppError
import com.ipb.castelobranco.features.profile.data.api.ProfileApi
import com.ipb.castelobranco.features.profile.data.dto.CreateAccessRequestBody
import com.ipb.castelobranco.features.profile.data.snapshot.ProfileSnapshotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Member access from the `/me` snapshot; a request is followed by one profile read through [AccessRepository]. */
@Singleton
class ProfileMemberAccessRepository @Inject constructor(
    profileSnapshot: ProfileSnapshotRepository,
    private val api: ProfileApi,
    private val accessRepository: AccessRepository,
) : MemberAccessRepository {

    override val memberAccess: Flow<MemberAccess> = profileSnapshot.observe()
        .map { state -> (state as? SnapshotState.Data)?.value?.memberAccessInfo ?: MemberAccess.NONE }
        .distinctUntilChanged()

    override suspend fun request(type: AccessRequestType, message: String?): Result<Unit> {
        val result = runCatching {
            val response = api.createAccessRequest(CreateAccessRequestBody(type = type.wireKey, message = message))
            if (!response.isSuccessful) throw response.toAppError()
            Unit
        }.mapError()
        accessRepository.refresh()
        return result
    }
}
