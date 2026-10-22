package com.ipb.castelobranco.core.domain.access

/** Where the account stands on member access, as the profile's `access_status` says. */
enum class AccessStatus {
    GRANTED,
    PENDING,
    NONE,

    /** Not sent (older server or a profile cached before it): no request is offered. */
    UNKNOWN;

    companion object {
        fun fromWire(value: String?): AccessStatus = when (value) {
            "granted" -> GRANTED
            "pending" -> PENDING
            "none" -> NONE
            else -> UNKNOWN
        }
    }
}

enum class AccessRequestType(val wireKey: String) {
    ACCESS("access"),
    DATA_CORRECTION("data_correction");

    companion object {
        fun fromWire(value: String?): AccessRequestType? = entries.firstOrNull { it.wireKey == value }
    }
}

/**
 * Member content access from the profile. [hasAccess] is the effective access (the flag or a leadership role);
 * the rest decides what a locked screen offers.
 */
data class MemberAccess(
    val hasAccess: Boolean,
    val status: AccessStatus,
    val requestsBlocked: Boolean,
    val correctionPending: Boolean,
) {
    companion object {
        val NONE = MemberAccess(
            hasAccess = false,
            status = AccessStatus.UNKNOWN,
            requestsBlocked = false,
            correctionPending = false,
        )
    }
}

/** What a members-only screen shows under its message. */
enum class AccessPrompt { NONE, CAN_REQUEST, PENDING, BLOCKED }

fun accessPromptFor(access: MemberAccess): AccessPrompt = when {
    access.requestsBlocked -> AccessPrompt.BLOCKED
    access.status == AccessStatus.PENDING -> AccessPrompt.PENDING
    access.status == AccessStatus.NONE -> AccessPrompt.CAN_REQUEST
    else -> AccessPrompt.NONE
}

/** The server refuses a longer request message. */
const val ACCESS_REQUEST_MESSAGE_MAX_LENGTH = 1000
