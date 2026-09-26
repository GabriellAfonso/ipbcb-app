package com.ipb.castelobranco.features.admin.members.domain.model

/**
 * The server deletes a member — record, history and photo — on request, with no recycle bin.
 * The only guard is here: the leader types the member's name. Letter case and spaces at the ends
 * do not count; anything else must match.
 */
object DeleteConfirmation {
    fun matches(typed: String, memberName: String): Boolean =
        typed.isNotBlank() && typed.trim().equals(memberName.trim(), ignoreCase = true)
}
