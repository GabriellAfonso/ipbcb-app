package com.ipb.castelobranco.core.testing

import com.ipb.castelobranco.core.domain.access.Access
import com.ipb.castelobranco.core.domain.access.AccessLevel
import com.ipb.castelobranco.core.domain.access.AccessRepository
import com.ipb.castelobranco.core.domain.access.Role
import com.ipb.castelobranco.core.domain.access.Scope
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAccessRepository(initial: Access = Access.NONE) : AccessRepository {

    val state = MutableStateFlow(initial)
    var refreshCalls = 0
        private set

    override val access = state

    override suspend fun refresh() {
        refreshCalls++
    }
}

/** Access with a single role and the given levels, for tests. */
fun accessOf(role: Role?, vararg levels: Pair<Scope, AccessLevel>) = Access(
    roles = setOfNotNull(role),
    hasAnyRole = role != null,
    levels = levels.toMap(),
)
