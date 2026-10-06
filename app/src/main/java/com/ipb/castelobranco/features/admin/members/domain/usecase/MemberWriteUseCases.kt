package com.ipb.castelobranco.features.admin.members.domain.usecase

import com.ipb.castelobranco.features.admin.members.domain.model.MemberDraft
import com.ipb.castelobranco.features.admin.members.domain.model.MemberRecord
import com.ipb.castelobranco.features.admin.members.domain.model.changesFrom
import com.ipb.castelobranco.features.admin.members.domain.repository.MembersAdminRepository
import javax.inject.Inject

/**
 * Creates when there is no [original], edits otherwise — sending only what changed. An edit with
 * nothing changed sends nothing and answers the record as it was.
 */
class SaveMemberUseCase @Inject constructor(
    private val repository: MembersAdminRepository,
) {
    suspend operator fun invoke(
        draft: MemberDraft,
        original: MemberRecord?,
        originalDraft: MemberDraft?,
    ): Result<MemberRecord> {
        val changes = draft.changesFrom(originalDraft)
        if (original == null) return repository.createMember(changes)
        if (changes.isEmpty) return Result.success(original)
        return repository.updateMember(original.id, changes)
    }
}

/** The server deletes at once, with the history and the photo; confirmation is the app's job. */
class DeleteMemberUseCase @Inject constructor(
    private val repository: MembersAdminRepository,
) {
    suspend operator fun invoke(id: Int): Result<Unit> = repository.deleteMember(id)
}
