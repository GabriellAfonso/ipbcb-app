package com.ipb.castelobranco.core.testing

import com.ipb.castelobranco.core.domain.worship.WorshipAccess
import com.ipb.castelobranco.core.domain.worship.WorshipAccessRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeWorshipAccessRepository(initial: WorshipAccess = WorshipAccess.NONE) : WorshipAccessRepository {

    val state = MutableStateFlow(initial)

    override val worshipAccess = state

    override suspend fun current(): WorshipAccess = state.value
}

val WORSHIP_MEMBER = WorshipAccess(isWorshipMember = true, canSaveSetlist = false)
val WORSHIP_LEADER = WorshipAccess(isWorshipMember = true, canSaveSetlist = true)
