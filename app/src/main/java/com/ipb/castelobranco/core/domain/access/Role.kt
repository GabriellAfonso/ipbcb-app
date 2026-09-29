package com.ipb.castelobranco.core.domain.access

/**
 * A role the backend assigns in the Django admin. The app never shows it; it only decides the
 * panel cards that have no scope of their own. Always referred to through this enum — `media` alone
 * would read as the `features/media` area.
 */
enum class Role(val wireId: String) {
    ADMIN("admin"),
    LEADER("leader"),
    MEDIA("media");

    companion object {
        fun fromWire(id: String): Role? = entries.firstOrNull { it.wireId == id }
    }
}
