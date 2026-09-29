package com.ipb.castelobranco.features.gallery.domain.sync

/** Schedules the periodic background sync. An interface so `domain/` stays free of WorkManager. */
interface GallerySyncScheduler {
    /** Idempotent: keeps the schedule when it already exists. */
    fun schedulePeriodic()
    fun cancel()
}
