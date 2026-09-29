package com.ipb.castelobranco.features.admin.members.presentation.state

/** One-shot events shared by the members screens. */
sealed interface MembersEvent {
    data class ShowMessage(val message: String) : MembersEvent

    /** The server refused a read (403): leave the members area. A refused write is a [ShowMessage]. */
    data class LeaveArea(val message: String) : MembersEvent

    /** The member no longer exists (404): back to the list. */
    data class MemberGone(val message: String) : MembersEvent

    data class Saved(val memberId: Int) : MembersEvent

    data object Deleted : MembersEvent
}
