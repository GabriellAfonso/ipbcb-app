package com.ipb.castelobranco.features.worshiphub.tables.domain.usecase

/** The draft lasts one hour from its last change; every change renews it. */
object DraftExpiry {

    const val DRAFT_TTL_MS = 3_600_000L

    /** A clock moved back (negative age) never expires the draft. */
    fun isExpired(updatedAtMillis: Long, nowMillis: Long): Boolean =
        nowMillis - updatedAtMillis >= DRAFT_TTL_MS
}
