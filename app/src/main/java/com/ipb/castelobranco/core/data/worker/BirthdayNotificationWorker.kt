package com.ipb.castelobranco.core.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.data.local.ThemePreferences
import com.ipb.castelobranco.core.domain.model.Birthday
import com.ipb.castelobranco.core.domain.model.Gender
import com.ipb.castelobranco.core.domain.repository.MembersRepository
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import timber.log.Timber
import java.time.LocalDate

@HiltWorker
class BirthdayNotificationWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val membersRepository: MembersRepository,
    private val themePreferences: ThemePreferences,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val enabled = themePreferences.birthdayNotificationsFlow.first()
        if (!enabled) return Result.success()

        val today = LocalDate.now()
        val snapshot = membersRepository.getCurrentSnapshot()

        val birthdays = when (snapshot) {
            is SnapshotState.Data -> snapshot.value.filter { it.month == today.monthValue && it.day == today.dayOfMonth }
            else -> emptyList()
        }

        if (birthdays.isEmpty()) return Result.success()

        if (!hasNotificationPermission()) return Result.success()

        ensureChannel()

        if (birthdays.size == 1) {
            notifySingle(birthdays.first())
        } else {
            notifyGroup(birthdays)
        }

        return Result.success()
    }

    private fun notifySingle(birthday: Birthday) {
        val message = randomMessage(birthday)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Aniversariante do dia")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(NOTIFICATION_ID_BASE, notification)
    }

    private fun notifyGroup(birthdays: List<Birthday>) {
        val manager = NotificationManagerCompat.from(applicationContext)

        // Summary
        val summaryText = "\uD83C\uDF82 ${birthdays.size} aniversariantes hoje!"
        val inboxStyle = NotificationCompat.InboxStyle()
            .setBigContentTitle(summaryText)
        birthdays.forEach { inboxStyle.addLine(randomMessage(it)) }

        val summary = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(summaryText)
            .setContentText("Toque para ver")
            .setStyle(inboxStyle)
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        // Individual notifications
        birthdays.forEachIndexed { index, birthday ->
            val message = randomMessage(birthday)
            val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Aniversariante do dia")
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setGroup(GROUP_KEY)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()
            manager.notify(NOTIFICATION_ID_BASE + 1 + index, notification)
        }

        manager.notify(NOTIFICATION_ID_SUMMARY, summary)
    }

    private fun randomMessage(birthday: Birthday): String {
        val prefix = when (birthday.gender) {
            Gender.MALE -> "do irm\u00E3o"
            Gender.FEMALE -> "da irm\u00E3"
            Gender.UNKNOWN -> "do irm\u00E3o"
        }
        val messages = listOf(
            "\uD83C\uDF82 ${birthday.name} est\u00E1 completando mais um ano de vida hoje!",
            "\uD83C\uDF88 Hoje: anivers\u00E1rio $prefix ${birthday.name}",
            "\u2728 Mais um ano de vida $prefix ${birthday.name}! Que Deus continue aben\u00E7oando essa caminhada",
            "\uD83D\uDC9B A igreja celebra hoje o anivers\u00E1rio $prefix ${birthday.name}!",
        )
        return messages.random()
    }

    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Anivers\u00E1rios",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifica\u00E7\u00F5es de anivers\u00E1rio dos membros"
        }
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val WORK_NAME = "birthday_daily_notification"
        const val CHANNEL_ID = "birthdays"
        private const val GROUP_KEY = "com.ipb.castelobranco.BIRTHDAYS"
        private const val NOTIFICATION_ID_BASE = 2000
        private const val NOTIFICATION_ID_SUMMARY = 1999
    }
}
