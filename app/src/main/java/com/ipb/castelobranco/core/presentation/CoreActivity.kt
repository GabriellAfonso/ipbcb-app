package com.ipb.castelobranco.core.presentation

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.ipb.castelobranco.core.presentation.navigation.AppNavHost
import com.ipb.castelobranco.core.presentation.navigation.NotificationTarget
import kotlinx.coroutines.flow.MutableStateFlow
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.ipb.castelobranco.BuildConfig
import com.ipb.castelobranco.core.data.logging.CrashlyticsTree
import com.ipb.castelobranco.core.data.logging.LogBufferTree
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class CoreActivity : ComponentActivity() {

    private lateinit var appUpdateManager: AppUpdateManager

    /** A tapped notification's destination, until `AppNavHost` has navigated to it. */
    private val notificationTarget = MutableStateFlow<NotificationTarget?>(null)

    @Suppress("unused")
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — no action needed */ }

    companion object {
        private const val EXTRA_APP_MESSAGE = "extra_app_message"
        private const val UPDATE_REQUEST_CODE = 500

        fun newRootIntent(context: Context, message: String? = null): Intent {
            return Intent(context, CoreActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                if (!message.isNullOrBlank()) putExtra(EXTRA_APP_MESSAGE, message)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initTimber()
        enableEdgeToEdge()

        appUpdateManager = AppUpdateManagerFactory.create(this)

        // A recreated activity already handled the intent that launched it.
        if (savedInstanceState == null) readNotificationTarget(intent)

        setContent {
            IPBCasteloBrancoTheme(dynamicColor = false) {
                val navController = rememberNavController()
                AppNavHost(
                    navController = navController,
                    notificationTarget = notificationTarget,
                    onNotificationTargetHandled = { notificationTarget.value = null },
                )
            }
        }

        checkForImmediateUpdate()
        requestNotificationPermissionIfNeeded()
    }

    /** `singleTop`: a notification tapped while the app is open arrives here instead of a new activity. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readNotificationTarget(intent)
    }

    private fun readNotificationTarget(intent: Intent?) {
        val target = NotificationTarget.fromExtras(
            target = intent?.getStringExtra(NotificationTarget.EXTRA_TARGET),
            date = intent?.getStringExtra(NotificationTarget.EXTRA_DATE),
        ) ?: return
        notificationTarget.value = target
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // If the user somehow dismissed the update screen, re-trigger it.
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                @Suppress("DEPRECATION")
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    AppUpdateType.IMMEDIATE,
                    this,
                    UPDATE_REQUEST_CODE
                )
            }
        }
    }

    private fun initTimber() {
        if (Timber.forest().isEmpty()) {
            Timber.plant(LogBufferTree)
            if (BuildConfig.DEBUG) {
                Timber.plant(Timber.DebugTree())
            } else {
                Timber.plant(CrashlyticsTree())
            }
        }
    }

    private fun checkForImmediateUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                @Suppress("DEPRECATION")
                appUpdateManager.startUpdateFlowForResult(
                    info,
                    AppUpdateType.IMMEDIATE,
                    this,
                    UPDATE_REQUEST_CODE
                )
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE && resultCode != RESULT_OK) {
            // User cancelled or update failed — finish so they can't use an outdated version.
            finish()
        }
    }
}

fun Activity.restartApp(message: String? = null) {
    startActivity(CoreActivity.newRootIntent(this, message))
    finish()
}
