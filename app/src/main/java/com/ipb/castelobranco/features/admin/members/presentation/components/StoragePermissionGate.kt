package com.ipb.castelobranco.features.admin.members.presentation.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.ipb.castelobranco.core.presentation.base.findActivity
import kotlinx.coroutines.launch

/** Shown when the storage permission was refused; nothing is saved. */
const val STORAGE_PERMISSION_DENIED = "Para salvar a foto, permita o acesso ao armazenamento."

/** Shown when it was refused for good ("não perguntar de novo"): only the settings can grant it. */
const val STORAGE_PERMISSION_BLOCKED =
    "Permita o acesso ao armazenamento nas configurações do aparelho para salvar a foto."

/**
 * Runs [onGranted] once saving to the shared Pictures folder is allowed. Android 10+ needs no
 * permission for that and runs it straight away; Android 9 and older ask for
 * `WRITE_EXTERNAL_STORAGE` first, and a refusal reaches [onDenied] with the text to show.
 *
 * @return the action to wire to the "Baixar" button
 */
@Composable
fun rememberStoragePermissionGate(
    onGranted: () -> Unit,
    onDenied: suspend (String) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnGranted by rememberUpdatedState(onGranted)
    val currentOnDenied by rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            currentOnGranted()
        } else {
            // After a refusal Android only offers the rationale while it would still ask again.
            val canAskAgain = context.findActivity()
                ?.shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE) == true
            scope.launch {
                currentOnDenied(if (canAskAgain) STORAGE_PERMISSION_DENIED else STORAGE_PERMISSION_BLOCKED)
            }
        }
    }
    return {
        val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
            PackageManager.PERMISSION_GRANTED
        if (needsPermission) launcher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE) else currentOnGranted()
    }
}
