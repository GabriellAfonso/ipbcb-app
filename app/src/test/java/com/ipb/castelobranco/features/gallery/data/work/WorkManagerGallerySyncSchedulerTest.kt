package com.ipb.castelobranco.features.gallery.data.work

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class WorkManagerGallerySyncSchedulerTest {

    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerGallerySyncScheduler

    @Before
    fun setup() {
        workManager = mockk(relaxed = true)
        every {
            workManager.enqueueUniquePeriodicWork(
                any<String>(),
                any<ExistingPeriodicWorkPolicy>(),
                any<PeriodicWorkRequest>(),
            )
        } returns mockk(relaxed = true)
        scheduler = WorkManagerGallerySyncScheduler(workManager)
    }

    @Test
    fun `schedulePeriodic keeps an existing schedule of 6 hours on any network`() {
        val request = slot<PeriodicWorkRequest>()

        scheduler.schedulePeriodic()

        verify(exactly = 1) {
            workManager.enqueueUniquePeriodicWork(
                GallerySyncWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                capture(request),
            )
        }
        assertEquals(TimeUnit.HOURS.toMillis(6), request.captured.workSpec.intervalDuration)
        assertEquals(NetworkType.CONNECTED, request.captured.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `cancel cancels the periodic sync`() {
        scheduler.cancel()

        verify(exactly = 1) { workManager.cancelUniqueWork(GallerySyncWorker.WORK_NAME) }
    }
}
