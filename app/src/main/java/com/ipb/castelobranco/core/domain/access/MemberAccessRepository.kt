package com.ipb.castelobranco.core.domain.access

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Member access and access requests. Lives in core so the members-only screens of several features read it; the
 * profile feature implements it from the `/me` snapshot.
 */
interface MemberAccessRepository {

    /** [MemberAccess.NONE] whenever there is no profile data. Emits only on change. */
    val memberAccess: Flow<MemberAccess>

    /**
     * Sends a request and then reads the profile again, also after a refusal: a refusal almost always means the
     * state changed (blocked, already pending, already granted).
     */
    suspend fun request(type: AccessRequestType, message: String?): Result<Unit>
}

class ObserveMemberAccessUseCase @Inject constructor(
    private val repository: MemberAccessRepository,
) {
    operator fun invoke(): Flow<MemberAccess> = repository.memberAccess
}

class RequestAccessUseCase @Inject constructor(
    private val repository: MemberAccessRepository,
) {
    /** A blank message is sent as absent; the server trims it too. */
    suspend operator fun invoke(type: AccessRequestType, message: String?): Result<Unit> =
        repository.request(type, message?.trim()?.takeIf { it.isNotEmpty() })
}
