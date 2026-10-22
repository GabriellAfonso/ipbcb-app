package com.ipb.castelobranco.core.testing

import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.domain.access.MemberAccess
import com.ipb.castelobranco.core.domain.access.MemberAccessRepository
import kotlinx.coroutines.flow.MutableStateFlow

/** Member access you set by hand; every request is recorded and answered with [requestResult]. */
class FakeMemberAccessRepository(initial: MemberAccess = MemberAccess.NONE) : MemberAccessRepository {

    override val memberAccess = MutableStateFlow(initial)

    var requestResult: Result<Unit> = Result.success(Unit)
    val requests = mutableListOf<Pair<AccessRequestType, String?>>()

    override suspend fun request(type: AccessRequestType, message: String?): Result<Unit> {
        requests += type to message
        return requestResult
    }
}
