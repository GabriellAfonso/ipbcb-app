package com.ipb.castelobranco.core.domain.push

import java.time.LocalDate
import java.time.format.DateTimeParseException

/**
 * A data message the app acts on (`backend/specs/017-sunday-setlist-push/contracts/push-messages.md`).
 * The payload carries only a type and a date; the app decides what to show.
 */
sealed interface PushMessage {
    val date: LocalDate

    /** A setlist was saved for [date]: re-read the current setlist. */
    data class SetlistSaved(override val date: LocalDate) : PushMessage

    /** The played songs of [date] are still not registered. */
    data class ConfirmPlays(override val date: LocalDate) : PushMessage

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_DATE = "date"
        const val TYPE_SETLIST_SAVED = "setlist_saved"
        const val TYPE_CONFIRM_PLAYS = "confirm_plays"

        /** `null` for an unknown type or a missing or malformed date — such messages are ignored. */
        fun parse(data: Map<String, String>): PushMessage? {
            val date = data[KEY_DATE]?.let { raw ->
                try {
                    LocalDate.parse(raw)
                } catch (_: DateTimeParseException) {
                    null
                }
            } ?: return null
            return when (data[KEY_TYPE]) {
                TYPE_SETLIST_SAVED -> SetlistSaved(date)
                TYPE_CONFIRM_PLAYS -> ConfirmPlays(date)
                else -> null
            }
        }
    }
}
