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
import com.ipb.castelobranco.core.domain.push.RecipientGender
import com.ipb.castelobranco.core.domain.push.ScheduleSlot
import com.ipb.castelobranco.core.presentation.CoreActivity
import com.ipb.castelobranco.core.presentation.navigation.NotificationTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

/** The three rota reminders (`specs/018-schedule-reminders/contracts/ui.md`). */
interface ScheduleNotifier {
    fun showWeek(slots: List<ScheduleSlot>)
    fun showEve(slot: ScheduleSlot, gender: RecipientGender)
    fun showPublished(month: YearMonth, slots: List<ScheduleSlot>, gender: RecipientGender)
}

/**
 * Its own "Escala" channel, so the reminders can be muted apart from the setlist. One fixed id per kind: a newer
 * one replaces the older (the server sends at most one eve reminder a day). A tap opens Escala on the first slot's
 * date.
 */
@Singleton
class ScheduleNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : ScheduleNotifier {

    override fun showWeek(slots: List<ScheduleSlot>) {
        notify(
            id = WEEK_ID,
            title = ScheduleNotificationText.WEEK_TITLE,
            text = ScheduleNotificationText.weekText(slots),
            lines = if (slots.size > 1) slots.map(ScheduleNotificationText::line) else emptyList(),
            slots = slots,
        )
    }

    override fun showEve(slot: ScheduleSlot, gender: RecipientGender) {
        notify(
            id = EVE_ID,
            title = ScheduleNotificationText.eveTitle(slot),
            text = ScheduleNotificationText.eveText(slot, gender),
            lines = emptyList(),
            slots = listOf(slot),
        )
    }

    override fun showPublished(month: YearMonth, slots: List<ScheduleSlot>, gender: RecipientGender) {
        notify(
            id = PUBLISHED_ID,
            title = ScheduleNotificationText.publishedTitle(month),
            text = ScheduleNotificationText.publishedText(slots.size, gender),
            lines = slots.map(ScheduleNotificationText::line),
            slots = slots,
        )
    }

    /** With [lines], the expanded view lists them; without, it shows [text] in full. */
    private fun notify(id: Int, title: String, text: String, lines: List<String>, slots: List<ScheduleSlot>) {
        if (!hasPermission()) return
        ensureChannel()
        val style = if (lines.isEmpty()) {
            NotificationCompat.BigTextStyle().bigText(text)
        } else {
            NotificationCompat.InboxStyle().setSummaryText(text).also { inbox -> lines.forEach(inbox::addLine) }
        }
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openIntent(id, slots.first()))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private fun openIntent(requestCode: Int, slot: ScheduleSlot): PendingIntent {
        val intent = Intent(context, CoreActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationTarget.EXTRA_TARGET, NotificationTarget.TARGET_SCHEDULE)
            putExtra(NotificationTarget.EXTRA_DATE, slot.date.toString())
        }
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
            description = "Lembretes de quando você está na escala"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "schedule"
        const val CHANNEL_NAME = "Escala"
        const val WEEK_ID = 3201
        const val EVE_ID = 3202
        const val PUBLISHED_ID = 3203
    }
}
