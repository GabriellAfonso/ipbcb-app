package com.ipb.castelobranco.features.bible.domain.download

/**
 * Schedules the Bible download job.
 *
 * An interface so `domain/` stays free of WorkManager. [replaceExisting] stands in for the
 * scheduling policy: `false` keeps a job that is already queued, `true` replaces it.
 */
interface BibleDownloadScheduler {
    fun enqueueWifiOnly(replaceExisting: Boolean = false, force: Boolean = false)
    fun enqueueAnyNetwork(force: Boolean = false)

    /** WiFi-only re-download of old-layout translations; keeps a job already queued and shows no banner. */
    fun enqueueFormatUpgrade()
    fun cancel()
}
