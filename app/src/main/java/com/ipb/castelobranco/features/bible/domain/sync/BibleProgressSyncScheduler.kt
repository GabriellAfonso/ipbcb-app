package com.ipb.castelobranco.features.bible.domain.sync

/** Schedules the background send of queued progress changes once there is network. */
fun interface BibleProgressSyncScheduler {
    fun scheduleSync()
}
