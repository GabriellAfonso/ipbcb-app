package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.features.admin.members.domain.model.MemberOptions
import com.ipb.castelobranco.features.admin.members.domain.model.MemberRecord
import com.ipb.castelobranco.features.admin.members.domain.model.MemberSummary
import com.ipb.castelobranco.features.admin.members.domain.repository.MembersAdminRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** The roll held in memory; null until loaded in this session. */
class ObserveMembersUseCase @Inject constructor(
    private val repository: MembersAdminRepository,
) {
    operator fun invoke(): Flow<List<MemberSummary>?> = repository.observeMembers()
}

class RefreshMembersUseCase @Inject constructor(
    private val repository: MembersAdminRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshMembers()
}

class GetMemberUseCase @Inject constructor(
    private val repository: MembersAdminRepository,
) {
    suspend operator fun invoke(id: Int): Result<MemberRecord> = repository.getMember(id)
}

/** Statuses, roles and ministries for the form pickers — always fetched fresh. */
class GetMemberOptionsUseCase @Inject constructor(
    private val repository: MembersAdminRepository,
) {
    suspend operator fun invoke(): Result<MemberOptions> = repository.getOptions()
}
