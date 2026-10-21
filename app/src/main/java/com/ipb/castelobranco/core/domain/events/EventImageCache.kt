package com.ipb.castelobranco.core.domain.events

/**
 * Empties the event images kept on the device. A members-only event's flyer is member content, so
 * signing out must not leave it behind (`specs/events/spec.md` §2.3).
 */
fun interface EventImageCache {
    suspend fun clear()
}
