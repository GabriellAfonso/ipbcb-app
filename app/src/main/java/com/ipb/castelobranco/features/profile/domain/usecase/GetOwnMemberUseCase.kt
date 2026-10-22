package com.ipb.castelobranco.features.profile.domain.usecase

import com.ipb.castelobranco.features.profile.domain.model.OwnMember
import com.ipb.castelobranco.features.profile.domain.repository.OwnMemberRepository
import javax.inject.Inject

class GetOwnMemberUseCase @Inject constructor(
    private val repository: OwnMemberRepository,
) {
    suspend operator fun invoke(): Result<OwnMember?> = repository.get()
}
