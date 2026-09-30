package com.ipb.castelobranco.features.gallery.data.work

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.ForegroundInfo
import com.ipb.castelobranco.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * The upload queue's notifications: "Enviando X de N" while it runs (the foreground notification),
 * and one summary when photos failed. When notifications are off the queue still runs; the album
 * screen shows the same progress.
 */
class GalleryUploadNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun foregroundInfo(current: Int, total: Int): ForegroundInfo {
        val notification = progress(current, total)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(PROGRESS_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(PROGRESS_ID, notification)
        }
    }

    fun showProgress(current: Int, total: Int) = notify(PROGRESS_ID, progress(current, total))

    /** A plain (non-foreground) run leaves its ongoing notification behind; the run ends it. */
    fun clearProgress() = NotificationManagerCompat.from(context).cancel(PROGRESS_ID)

    fun showFailures(count: Int) {
        val text = if (count == 1) "1 foto não foi enviada" else "$count fotos não foram enviadas"
        val notification = builder()
            .setContentTitle(TITLE)
            .setContentText(text)
            .setContentIntent(openApp())
            .setAutoCancel(true)
            .build()
        notify(FAILURES_ID, notification)
    }

    private fun progress(current: Int, total: Int): Notification = builder()
        .setContentTitle(TITLE)
        .setContentText(if (total > 0) "Enviando $current de $total" else PREPARING_TEXT)
        .setProgress(total, (current - 1).coerceAtLeast(0), total == 0)
        .setOngoing(true)
        .setOnlyAlertOnce(true)
        .build()

    private fun builder(): NotificationCompat.Builder {
        ensureChannel()
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_LOW)
    }

    @SuppressLint("MissingPermission") // Checked by areNotificationsEnabled(); denied = nothing shown.
    private fun notify(id: Int, notification: Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (manager.areNotificationsEnabled()) manager.notify(id, notification)
    }

    private fun openApp(): PendingIntent? {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName) ?: return null
        return PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "gallery_upload"
        private const val CHANNEL_NAME = "Envio de fotos"
        private const val TITLE = "Galeria"
        private const val PREPARING_TEXT = "Preparando fotos…"
        private const val PROGRESS_ID = 3100
        private const val FAILURES_ID = 3101
    }
}
