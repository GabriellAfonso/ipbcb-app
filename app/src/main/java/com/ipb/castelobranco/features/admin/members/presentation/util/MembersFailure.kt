package com.ipb.castelobranco.features.admin.members.presentation.util

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent

private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404

/**
 * The reaction every members screen shares: a refused leader leaves the area, a vanished member
 * goes back to the list, anything else is a message. The text is always `toUserMessage()`.
 */
fun Throwable.toMembersEvent(): MembersEvent {
    val error = toAppError()
    val message = error.toUserMessage()
    return when {
        error is AppError.Auth && error.code == HTTP_FORBIDDEN -> MembersEvent.LeaveArea(message)
        error is AppError.Server && error.code == HTTP_NOT_FOUND -> MembersEvent.MemberGone(message)
        else -> MembersEvent.ShowMessage(message)
    }
}

/** True when the failure is one the screen hands to navigation instead of showing in place. */
fun MembersEvent.isNavigation(): Boolean =
    this is MembersEvent.LeaveArea || this is MembersEvent.MemberGone
