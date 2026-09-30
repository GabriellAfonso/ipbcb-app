package com.ipb.castelobranco.core.domain.member

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveOwnMemberIdUseCase @Inject constructor(
    private val repository: CurrentMemberRepository,
) {
    operator fun invoke(): Flow<Long?> = repository.memberId
}
