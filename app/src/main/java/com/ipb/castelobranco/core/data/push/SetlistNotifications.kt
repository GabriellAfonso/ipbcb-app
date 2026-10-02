package com.ipb.castelobranco.core.data.push

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.CoreActivity
import com.ipb.castelobranco.core.presentation.navigation.NotificationTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/** The two Sunday setlist notifications. */
interface SetlistNotifier {
    fun showSetlistSaved(date: LocalDate)
    fun showConfirmPlays(date: LocalDate)
}

/**
 * One "Repertório" channel for both, so the user can mute them without muting birthdays or uploads.
 * Each kind has a fixed id: a newer message replaces the older one instead of stacking.
 */
@Singleton
class SetlistNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SetlistNotifier {

    override fun showSetlistSaved(date: LocalDate) {
        notify(
            id = SETLIST_SAVED_NOTIFICATION_ID,
            title = "Repertório de domingo ${date.format(DAY_MONTH)} disponível",
            text = "Veja as músicas em Letras e Cifras.",
            intent = openIntent(NotificationTarget.TARGET_SUNDAY_SETLIST, null),
        )
    }

    override fun showConfirmPlays(date: LocalDate) {
        notify(
            id = CONFIRM_PLAYS_NOTIFICATION_ID,
            title = "Confirmar músicas de domingo",
            text = "Registre as músicas tocadas em ${date.format(DAY_MONTH)}.",
            intent = openIntent(NotificationTarget.TARGET_CONFIRM_PLAYS, date),
        )
    }

    private fun notify(id: Int, title: String, text: String, intent: PendingIntent) {
        if (!hasPermission()) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private fun openIntent(target: String, date: LocalDate?): PendingIntent {
        val intent = Intent(context, CoreActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationTarget.EXTRA_TARGET, target)
            date?.let { putExtra(NotificationTarget.EXTRA_DATE, it.toString()) }
        }
        val requestCode = if (date == null) SETLIST_SAVED_NOTIFICATION_ID else CONFIRM_PLAYS_NOTIFICATION_ID
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun hasPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel() {
        val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Repertório de domingo e lembrete de confirmar as músicas tocadas"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "sunday_setlist"
        const val CHANNEL_NAME = "Repertório"
        const val SETLIST_SAVED_NOTIFICATION_ID = 3001
        const val CONFIRM_PLAYS_NOTIFICATION_ID = 3002
        val DAY_MONTH: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM")
    }
}
