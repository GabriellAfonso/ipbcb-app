package com.ipb.castelobranco.features.bible.data.work

import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.WorkManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class WorkManagerBibleDownloadSchedulerTest {

    private lateinit var workManager: WorkManager
    private lateinit var scheduler: WorkManagerBibleDownloadScheduler

    @Before
    fun setup() {
        workManager = mockk(relaxed = true)
        every {
            workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), any<OneTimeWorkRequest>())
        } returns mockk(relaxed = true)
        scheduler = WorkManagerBibleDownloadScheduler(workManager)
    }

    @Test
    fun `enqueueWifiOnly uses KEEP policy and unmetered network by default`() {
        val request = slot<OneTimeWorkRequest>()

        scheduler.enqueueWifiOnly()

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                BibleDownloadWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                capture(request),
            )
        }
        assertEquals(NetworkType.UNMETERED, request.captured.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `enqueueWifiOnly with replaceExisting uses REPLACE policy`() {
        val request = slot<OneTimeWorkRequest>()

        scheduler.enqueueWifiOnly(replaceExisting = true)

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                BibleDownloadWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                capture(request),
            )
        }
        assertEquals(NetworkType.UNMETERED, request.captured.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `enqueueAnyNetwork uses REPLACE policy and any connected network`() {
        val request = slot<OneTimeWorkRequest>()

        scheduler.enqueueAnyNetwork()

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                BibleDownloadWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                capture(request),
            )
        }
        assertEquals(NetworkType.CONNECTED, request.captured.workSpec.constraints.requiredNetworkType)
    }

    @Test
    fun `enqueueFormatUpgrade keeps a queued job, waits for WiFi and carries the silent tag`() {
        val request = slot<OneTimeWorkRequest>()

        scheduler.enqueueFormatUpgrade()

        verify(exactly = 1) {
            workManager.enqueueUniqueWork(
                BibleDownloadWorker.WORK_NAME,
                ExistingWorkPolicy.KEEP,
                capture(request),
            )
        }
        assertEquals(NetworkType.UNMETERED, request.captured.workSpec.constraints.requiredNetworkType)
        assertTrue(BibleDownloadWorker.TAG_FORMAT_UPGRADE in request.captured.tags)
    }

    @Test
    fun `force flag is passed to the worker input data`() {
        val request = slot<OneTimeWorkRequest>()

        scheduler.enqueueAnyNetwork(force = true)

        verify { workManager.enqueueUniqueWork(any<String>(), any<ExistingWorkPolicy>(), capture(request)) }
        assertTrue(request.captured.workSpec.input.getBoolean(BibleDownloadWorker.INPUT_FORCE, false))
    }
}
