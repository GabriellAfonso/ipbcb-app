package com.ipb.castelobranco.core.testing

import com.ipb.castelobranco.core.domain.member.CurrentMemberRepository
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCurrentMemberRepository(initial: Long? = null) : CurrentMemberRepository {

    val state = MutableStateFlow(initial)

    override val memberId = state
}
