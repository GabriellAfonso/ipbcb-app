package com.ipb.castelobranco.core.domain.access

/** A feature area of the management panel, as the backend names it in the profile's `permissions`. */
enum class Scope(val wireKey: String) {
    MEMBERS("members"),
    SCHEDULE("schedule"),
    SONGS("songs"),
    GALLERY("gallery"),
    EVENTS("events"),
    NOTICES("notices"),
    HYMNAL_HISTORY_REPORT("reports.hymnal_history");

    companion object {
        /** An unknown key is dropped: the app never grants access to something it does not understand. */
        fun fromWire(key: String): Scope? = entries.firstOrNull { it.wireKey == key }
    }
}
