package com.ipb.castelobranco.core.domain.push

import com.ipb.castelobranco.core.domain.access.AccessRequestType
import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * A data message the app acts on (`backend/specs/017-sunday-setlist-push/contracts/push-messages.md`,
 * `backend/specs/022-access-requests/contracts/access-api.md`). The payload carries a type and ids or a date,
 * never a name; the app decides what to show.
 */
sealed interface PushMessage {

    /** A setlist was saved for [date]: re-read the current setlist. */
    data class SetlistSaved(val date: LocalDate) : PushMessage

    /** The played songs of [date] are still not registered. */
    data class ConfirmPlays(val date: LocalDate) : PushMessage

    /** Someone asked for access or a correction (sent to leaders). */
    data class AccessRequestCreated(val requestType: AccessRequestType) : PushMessage

    /** The person's own request was approved (a correction: resolved). */
    data class AccessRequestApproved(val requestType: AccessRequestType) : PushMessage

    /** The person's own request was denied; [reason] is what the leader wrote, when they did. */
    data class AccessRequestDenied(val requestType: AccessRequestType, val reason: String?) : PushMessage

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_DATE = "date"
        const val KEY_REQUEST_TYPE = "request_type"
        const val KEY_REASON = "reason"
        const val TYPE_SETLIST_SAVED = "setlist_saved"
        const val TYPE_CONFIRM_PLAYS = "confirm_plays"
        const val TYPE_ACCESS_REQUEST_CREATED = "access_request_created"
        const val TYPE_ACCESS_REQUEST_APPROVED = "access_request_approved"
        const val TYPE_ACCESS_REQUEST_DENIED = "access_request_denied"

        /** `null` for an unknown type or a missing or malformed field — such messages are ignored. */
        fun parse(data: Map<String, String>): PushMessage? = when (val type = data[KEY_TYPE]) {
            TYPE_SETLIST_SAVED -> data.date()?.let(::SetlistSaved)
            TYPE_CONFIRM_PLAYS -> data.date()?.let(::ConfirmPlays)
            TYPE_ACCESS_REQUEST_CREATED, TYPE_ACCESS_REQUEST_APPROVED, TYPE_ACCESS_REQUEST_DENIED -> {
                AccessRequestType.fromWire(data[KEY_REQUEST_TYPE])?.let { requestType ->
                    when (type) {
                        TYPE_ACCESS_REQUEST_CREATED -> AccessRequestCreated(requestType)
                        TYPE_ACCESS_REQUEST_APPROVED -> AccessRequestApproved(requestType)
                        else -> AccessRequestDenied(requestType, data[KEY_REASON]?.trim()?.takeIf { it.isNotEmpty() })
                    }
                }
            }
            else -> null
        }

        private fun Map<String, String>.date(): LocalDate? = this[KEY_DATE]?.let { raw ->
            try {
                LocalDate.parse(raw)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
