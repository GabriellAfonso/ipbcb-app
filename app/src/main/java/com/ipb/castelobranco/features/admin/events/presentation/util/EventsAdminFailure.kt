package com.ipb.castelobranco.features.admin.events.presentation.util

import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminEvent

private const val HTTP_FORBIDDEN = 403
private const val HTTP_NOT_FOUND = 404

/** Whether the refused request was reading events or changing them — only the caller knows. */
enum class FailureKind { READ, WRITE }

/**
 * The reaction every events management screen shares (`specs/events/spec.md` §4.4): a refused read
 * leaves the area, a refused write only says why, a vanished event goes back to the list, anything
 * else is a message. The text is always `toUserMessage()`.
 */
fun Throwable.toEventsAdminEvent(kind: FailureKind): EventsAdminEvent {
    val error = toAppError()
    val message = error.toUserMessage()
    return when {
        error is AppError.Auth && error.code == HTTP_FORBIDDEN && kind == FailureKind.READ ->
            EventsAdminEvent.LeaveArea(message)
        error is AppError.Server && error.code == HTTP_NOT_FOUND -> EventsAdminEvent.EventGone(message)
        else -> EventsAdminEvent.ShowMessage(message)
    }
}
