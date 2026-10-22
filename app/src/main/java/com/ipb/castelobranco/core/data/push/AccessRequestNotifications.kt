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
import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.presentation.CoreActivity
import com.ipb.castelobranco.core.presentation.navigation.NotificationTarget
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** The three access request notifications (`specs/016-access-requests/contracts/ui.md`). */
interface AccessRequestNotifier {
    fun showCreated(type: AccessRequestType)
    fun showApproved(type: AccessRequestType)
    fun showDenied(type: AccessRequestType, reason: String?)
}

/**
 * Its own "Pedidos de acesso" channel, so it can be muted apart from the setlist. Leaders get one fixed id for
 * new requests (a newer one replaces the older); the requester one per outcome.
 */
@Singleton
class AccessRequestNotifications @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : AccessRequestNotifier {

    override fun showCreated(type: AccessRequestType) {
        notify(
            id = CREATED_ID,
            title = if (type == AccessRequestType.ACCESS) "Novo pedido de acesso" else "Novo pedido de correção",
            text = "Toque para ver os pedidos pendentes.",
            target = NotificationTarget.TARGET_PROFILES_PENDING,
        )
    }

    override fun showApproved(type: AccessRequestType) {
        val access = type == AccessRequestType.ACCESS
        notify(
            id = DECIDED_ID,
            title = if (access) "Acesso liberado" else "Correção resolvida",
            text = if (access) {
                "Agora você pode ver o conteúdo dos membros."
            } else {
                "A liderança resolveu seu pedido de correção."
            },
            target = NotificationTarget.TARGET_PROFILE,
        )
    }

    override fun showDenied(type: AccessRequestType, reason: String?) {
        notify(
            id = DECIDED_ID,
            title = if (type == AccessRequestType.ACCESS) "Pedido de acesso negado" else "Pedido de correção negado",
            text = reason ?: "Fale com a liderança para saber mais.",
            target = NotificationTarget.TARGET_PROFILE,
        )
    }

    private fun notify(id: Int, title: String, text: String, target: String) {
        if (!hasPermission()) return
        ensureChannel()
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openIntent(id, target))
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private fun openIntent(requestCode: Int, target: String): PendingIntent {
        val intent = Intent(context, CoreActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(NotificationTarget.EXTRA_TARGET, target)
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
            description = "Pedidos de acesso e de correção de dados"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "access_requests"
        const val CHANNEL_NAME = "Pedidos de acesso"
        const val CREATED_ID = 3101
        const val DECIDED_ID = 3102
    }
}
