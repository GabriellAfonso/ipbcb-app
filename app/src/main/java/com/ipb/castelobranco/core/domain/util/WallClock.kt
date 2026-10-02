package com.ipb.castelobranco.core.domain.util

/**
 * Wall-clock time, in milliseconds since the epoch.
 *
 * For durations that must survive the app and the device restarting (e.g. "changed less than an hour
 * ago"), where [MonotonicClock] cannot help: its origin resets on reboot. Injected so `domain/` code
 * can be tested against a fixed instant.
 */
fun interface WallClock {
    fun nowMillis(): Long
}
