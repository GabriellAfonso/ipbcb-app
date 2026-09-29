package com.ipb.castelobranco.core.domain.access

/**
 * How much a user may do in a [Scope]. Declared in hierarchy order — each level includes the ones
 * before it — so comparing levels is comparing enum order.
 */
enum class AccessLevel(val wireValue: String) {
    VIEW("view"),
    MANAGE("manage"),
    OWNER("owner");

    companion object {
        /** Anything other than the three known values — `null` included — means no access. */
        fun fromWire(value: String?): AccessLevel? = entries.firstOrNull { it.wireValue == value }
    }
}
