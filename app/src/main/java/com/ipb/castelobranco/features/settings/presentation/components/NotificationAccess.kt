package com.ipb.castelobranco.features.settings.presentation.components

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ipb.castelobranco.core.presentation.base.findActivity

/**
 * Returns a callback that makes sure the app can post notifications: asks for `POST_NOTIFICATIONS` (Android 13+)
 * and, when the system no longer shows the dialog or notifications are off in system settings, opens the app's
 * notification settings instead.
 */
@Composable
fun rememberNotificationAccessRequest(): () -> Unit {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    // Saveable: the permission dialog can outlive a recomposition or an activity recreation.
    var rationaleBeforeRequest by rememberSaveable { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted || activity == null) return@rememberLauncherForActivityResult
        val rationaleAfterRequest = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.POST_NOTIFICATIONS,
        )
        // False before and after: the system denied without a dialog, so only settings can grant it now.
        if (!rationaleBeforeRequest && !rationaleAfterRequest) context.openAppNotificationSettings()
    }

    return {
        when {
            NotificationManagerCompat.from(context).areNotificationsEnabled() -> Unit
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && activity != null &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED -> {
                rationaleBeforeRequest = ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS,
                )
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            else -> context.openAppNotificationSettings()
        }
    }
}

private fun Context.openAppNotificationSettings() {
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null))
    }
    startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
