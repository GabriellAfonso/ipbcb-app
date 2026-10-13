package com.ipb.castelobranco

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.ipb.castelobranco.core.data.image.ImageMemoryBudget
import com.ipb.castelobranco.core.data.local.ThemePreferences
import com.ipb.castelobranco.core.data.worker.BirthdayNotificationSchedule
import com.ipb.castelobranco.core.data.worker.BirthdayNotificationWorker
import com.ipb.castelobranco.features.hymnal.domain.sync.HymnViewSyncScheduler
import com.ipb.castelobranco.features.settings.domain.model.ThemeMode
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class MyApp : Application(), Configuration.Provider, ImageLoaderFactory {

    @Inject lateinit var themePreferences: ThemePreferences
    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var hymnViewSyncScheduler: HymnViewSyncScheduler

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /** Coil's default loader, used by every `AsyncImage` that does not pass its own. */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .memoryCache { ImageMemoryBudget.memoryCache(this, ImageMemoryBudget.DEFAULT) }
        .build()

    override fun onCreate() {
        super.onCreate()

        appScope.launch {
            val mode = runCatching { themePreferences.themeModeFlow.first() }
                .getOrDefault(ThemeMode.FOLLOW_SYSTEM)
            val nightMode = when (mode) {
                ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
                ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
                ThemeMode.FOLLOW_SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            }
            AppCompatDelegate.setDefaultNightMode(nightMode)
        }

        scheduleBirthdayNotifications()

        // Picks up anything the unique-work KEEP policy could not schedule while a previous run
        // was finishing, so a queued view is never stranded longer than one app launch.
        hymnViewSyncScheduler.scheduleSync()
    }

    /**
     * KEEP on both: WorkManager starts this process to run the worker, so `onCreate` runs again inside that run,
     * and a replacing policy would cancel the very run that started it.
     */
    private fun scheduleBirthdayNotifications() {
        val workManager = WorkManager.getInstance(this)
        workManager.cancelUniqueWork(BirthdayNotificationWorker.LEGACY_WORK_NAME)
        scheduleBirthdayRun(workManager, BirthdayNotificationWorker.NOON_WORK_NAME, BirthdayNotificationSchedule.NOON)
        scheduleBirthdayRun(
            workManager,
            BirthdayNotificationWorker.EVENING_WORK_NAME,
            BirthdayNotificationSchedule.EVENING,
        )
    }

    private fun scheduleBirthdayRun(workManager: WorkManager, name: String, target: LocalTime) {
        val initialDelay = BirthdayNotificationSchedule.initialDelay(LocalDateTime.now(), target)
        val request = PeriodicWorkRequestBuilder<BirthdayNotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay.toMinutes(), TimeUnit.MINUTES)
            .build()
        workManager.enqueueUniquePeriodicWork(name, ExistingPeriodicWorkPolicy.KEEP, request)
    }
}