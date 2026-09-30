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
import java.util.concurrent.TimeUnit

class WorkManagerGalleryUploadSchedulerTest {

    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerGalleryUploadScheduler

    @Before
    fun setup() {
        workManager = mockk(relaxed = true)
        every {
            workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>())
        } returns mockk(relaxed = true)
        scheduler = WorkManagerGalleryUploadScheduler(workManager)
    }

    @Test
    fun `enqueue appends to the queue work on any network with exponential backoff`() {
        val request = slot<OneTimeWorkRequest>()

        scheduler.enqueue()

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                GalleryUploadWorker.WORK_NAME,
                ExistingWorkPolicy.APPEND_OR_REPLACE,
                capture(request),
            )
        }
        val spec = request.captured.workSpec
        assertEquals(NetworkType.CONNECTED, spec.constraints.requiredNetworkType)
        assertEquals(BackoffPolicy.EXPONENTIAL, spec.backoffPolicy)
        assertEquals(TimeUnit.SECONDS.toMillis(30), spec.backoffDelayDuration)
    }

    @Test
    fun `cancel cancels the upload work`() {
        scheduler.cancel()

        verify(exactly = 1) { workManager.cancelUniqueWork(GalleryUploadWorker.WORK_NAME) }
    }
}
