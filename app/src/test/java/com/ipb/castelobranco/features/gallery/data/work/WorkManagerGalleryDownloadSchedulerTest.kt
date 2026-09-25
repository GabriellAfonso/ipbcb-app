package com.ipb.castelobranco.features.gallery.data.work

import androidx.work.BackoffPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WorkManagerGalleryDownloadSchedulerTest {

    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerGalleryDownloadScheduler

    @Before
    fun setup() {
        workManager = mockk(relaxed = true)
        every {
            workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>())
        } returns mockk(relaxed = true)
        scheduler = WorkManagerGalleryDownloadScheduler(workManager)
    }

    @Test
    fun `enqueueWifiOnly uses KEEP policy and unmetered network by default`() {
        val request = slot<OneTimeWorkRequest>()

        scheduler.enqueueWifiOnly()

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                GalleryDownloadWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                capture(request),
            )
        }
        assertEquals(NetworkType.UNMETERED, request.captured.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `enqueueWifiOnly with replaceExisting uses REPLACE policy`() {
        scheduler.enqueueWifiOnly(replaceExisting = true)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                GalleryDownloadWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                any<OneTimeWorkRequest>(),
            )
        }
    }

    @Test
    fun `enqueueAnyNetwork uses REPLACE policy and any connected network`() {
        val request = slot<OneTimeWorkRequest>()

        scheduler.enqueueAnyNetwork()

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                GalleryDownloadWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                capture(request),
            )
        }
        assertEquals(NetworkType.CONNECTED, request.captured.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `cancel cancels the unique work`() {
        scheduler.cancel()

        verify(exactly = 1) { workManager.cancelUniqueWork(GalleryDownloadWorker.WORK_NAME) }
    }

    @Test
    fun `both requests back off exponentially from 60 seconds`() {
        val requests = mutableListOf<OneTimeWorkRequest>()
        every {
            workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), capture(requests))
        } returns mockk(relaxed = true)

        scheduler.enqueueWifiOnly()
        scheduler.enqueueAnyNetwork()

        assertEquals(2, requests.size)
        requests.forEach { request ->
            assertEquals(BackoffPolicy.EXPONENTIAL, request.workSpec.backoffPolicy)
            assertEquals(60_000L, request.workSpec.backoffDelayDuration)
        }
    }
}
