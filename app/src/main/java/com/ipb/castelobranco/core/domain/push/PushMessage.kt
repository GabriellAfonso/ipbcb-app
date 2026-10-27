package com.ipb.castelobranco.core.domain.push

import com.ipb.castelobranco.core.domain.access.AccessRequestType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeParseException

/** One rota entry as a reminder carries it: no member name, no id of a person. */
data class ScheduleSlot(val date: LocalDate, val service: String, val time: LocalTime)

/** Who a rota reminder speaks to, for "o/a responsável", "escalado/a". No gender recorded reads as [MALE]. */
enum class RecipientGender {
    MALE,
    FEMALE,
    ;

    companion object {
        private const val WIRE_FEMALE = "F"

        fun fromWire(value: String?): RecipientGender = if (value == WIRE_FEMALE) FEMALE else MALE
    }
}

/**
 * A data message the app acts on (`backend/specs/017-sunday-setlist-push/contracts/push-messages.md`,
 * `backend/specs/022-access-requests/contracts/access-api.md`, `backend/specs/024-schedule-reminders/spec.md`).
 * The payload carries a type and ids, dates or church catalogue data, never a person's name.
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

    /** Monday morning: the user's rota entries of this week, in date order. */
    data class ScheduleWeek(
        val slots: List<ScheduleSlot>,
        val gender: RecipientGender = RecipientGender.MALE,
    ) : PushMessage

    /** The evening before [slot]. */
    data class ScheduleEve(
        val slot: ScheduleSlot,
        val gender: RecipientGender = RecipientGender.MALE,
    ) : PushMessage

    /** The rota of [month] became final; [slots] are the user's entries from today on. */
    data class SchedulePublished(
        val month: YearMonth,
        val slots: List<ScheduleSlot>,
        val gender: RecipientGender = RecipientGender.MALE,
    ) : PushMessage

    companion object {
        const val KEY_TYPE = "type"
        const val KEY_DATE = "date"
        const val KEY_REQUEST_TYPE = "request_type"
        const val KEY_REASON = "reason"
        const val KEY_SLOTS = "slots"
        const val KEY_MONTH = "month"
        const val KEY_GENDER = "gender"
        const val SLOT_DATE = "date"
        const val SLOT_SERVICE = "service"
        const val SLOT_TIME = "time"
        const val TYPE_SETLIST_SAVED = "setlist_saved"
        const val TYPE_CONFIRM_PLAYS = "confirm_plays"
        const val TYPE_ACCESS_REQUEST_CREATED = "access_request_created"
        const val TYPE_ACCESS_REQUEST_APPROVED = "access_request_approved"
        const val TYPE_ACCESS_REQUEST_DENIED = "access_request_denied"
        const val TYPE_SCHEDULE_WEEK = "schedule_week"
        const val TYPE_SCHEDULE_EVE = "schedule_eve"
        const val TYPE_SCHEDULE_PUBLISHED = "schedule_published"

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
            TYPE_SCHEDULE_WEEK -> data.slots()?.let { ScheduleWeek(it, data.gender()) }
            TYPE_SCHEDULE_EVE -> data.slots()?.singleOrNull()?.let { ScheduleEve(it, data.gender()) }
            TYPE_SCHEDULE_PUBLISHED -> {
                val month = data[KEY_MONTH]?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                val slots = data.slots()
                if (month != null && slots != null) SchedulePublished(month, slots, data.gender()) else null
            }
            else -> null
        }

        private fun Map<String, String>.gender(): RecipientGender = RecipientGender.fromWire(this[KEY_GENDER])

        /** `slots` is a JSON-encoded array; one bad slot (or none at all) makes the whole message invalid. */
        private fun Map<String, String>.slots(): List<ScheduleSlot>? {
            val raw = this[KEY_SLOTS] ?: return null
            val items = runCatching { Json.parseToJsonElement(raw).jsonArray }.getOrNull() ?: return null
            val slots = items.map { item -> (item as? JsonObject)?.toSlot() ?: return null }
            return slots.ifEmpty { null }
        }

        private fun JsonObject.toSlot(): ScheduleSlot? {
            val date = string(SLOT_DATE)?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: return null
            val service = string(SLOT_SERVICE)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
            val time = string(SLOT_TIME)?.takeIf { TIME_PATTERN.matches(it) }
                ?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: return null
            return ScheduleSlot(date, service, time)
        }

        private fun JsonObject.string(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

        private val TIME_PATTERN = Regex("""\d{2}:\d{2}""")

        private fun Map<String, String>.date(): LocalDate? = this[KEY_DATE]?.let { raw ->
            try {
                LocalDate.parse(raw)
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }
}
