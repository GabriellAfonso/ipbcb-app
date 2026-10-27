package com.ipb.castelobranco.features.gallery.data.work

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

class WorkManagerGalleryVideoDownloadSchedulerTest {

    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerGalleryVideoDownloadScheduler

    @Before
    fun setup() {
        workManager = mockk(relaxed = true)
        every {
            workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>())
        } returns mockk(relaxed = true)
        scheduler = WorkManagerGalleryVideoDownloadScheduler(workManager)
    }

    @Test
    fun `videos always wait for an unmetered network`() {
        val request = slot<OneTimeWorkRequest>()

        scheduler.enqueue()
        scheduler.enqueue(replaceExisting = true)

        verify {
            workManager.enqueueUniqueWork(
                GalleryVideoDownloadWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                any<OneTimeWorkRequest>(),
            )
        }
        verify {
            workManager.enqueueUniqueWork(
                GalleryVideoDownloadWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                capture(request),
            )
        }
        assertEquals(NetworkType.UNMETERED, request.captured.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `the WiFi upload run waits for an unmetered network and keeps a waiting one`() {
        val upload = WorkManagerGalleryUploadScheduler(workManager)
        val request = slot<OneTimeWorkRequest>()

        upload.enqueueWifiOnly()

        verify {
            workManager.enqueueUniqueWork(GalleryUploadWorker.WIFI_WORK_NAME, ExistingWorkPolicy.KEEP, capture(request))
        }
        assertEquals(NetworkType.UNMETERED, request.captured.workSpec.constraints.requiredNetworkType)
    }
}
