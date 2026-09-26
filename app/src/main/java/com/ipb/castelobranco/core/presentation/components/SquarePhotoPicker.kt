package com.ipb.castelobranco.core.presentation.components

import android.app.Activity
import android.content.Context
import android.graphics.Color
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.yalantis.ucrop.UCrop
import timber.log.Timber
import java.io.File

private const val CROPPED_PREFIX = "cropped_"
private const val CROPPED_SUFFIX = ".jpg"
private const val MAX_SIDE_PX = 512
private const val IMAGE_MIME = "image/*"
private const val CROP_TITLE = "Recortar Foto"

/**
 * Picks an image from the device, crops it square with UCrop and hands back the JPEG bytes.
 *
 * UCrop can only write its result to a file, so the crop lands in `cacheDir` for the instant
 * between the crop result and the read — then it is deleted. Leftovers from a process that died in
 * between are swept every time the picker opens. Nothing picked here stays on the device: the
 * member photo depends on it (LGPD), the profile photo just stops leaving dead files behind.
 *
 * @return the action that opens the picker
 */
@Composable
fun rememberSquarePhotoPicker(
    onPicked: (ByteArray) -> Unit,
    onCancelled: () -> Unit = {},
): () -> Unit {
    val context = LocalContext.current
    val currentOnPicked by rememberUpdatedState(onPicked)
    val currentOnCancelled by rememberUpdatedState(onCancelled)

    val cropLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val output = result.data
            ?.takeIf { result.resultCode == Activity.RESULT_OK }
            ?.let(UCrop::getOutput)
        val bytes = output?.let { readAndDelete(context, it) }

        if (bytes == null || bytes.isEmpty()) currentOnCancelled() else currentOnPicked(bytes)
    }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) {
            currentOnCancelled()
            return@rememberLauncherForActivityResult
        }
        val destination = Uri.fromFile(
            File(context.cacheDir, "$CROPPED_PREFIX${System.currentTimeMillis()}$CROPPED_SUFFIX")
        )
        val options = UCrop.Options().apply {
            setStatusBarColor(Color.BLACK)
            setToolbarColor(Color.BLACK)
            setToolbarWidgetColor(Color.WHITE)
            setToolbarTitle(CROP_TITLE)
            setHideBottomControls(false)
        }
        val intent = UCrop.of(uri, destination)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(MAX_SIDE_PX, MAX_SIDE_PX)
            .withOptions(options)
            .getIntent(context)

        cropLauncher.launch(intent)
    }

    return {
        sweepLeftovers(context)
        pickLauncher.launch(IMAGE_MIME)
    }
}

private fun readAndDelete(context: Context, uri: Uri): ByteArray? {
    val bytes = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
    }.onFailure { Timber.w(it, "Could not read cropped photo") }.getOrNull()
    uri.path?.let { File(it).delete() }
    return bytes
}

private fun sweepLeftovers(context: Context) {
    context.cacheDir
        .listFiles { file -> file.name.startsWith(CROPPED_PREFIX) && file.name.endsWith(CROPPED_SUFFIX) }
        ?.forEach { it.delete() }
}

@Preview(showBackground = true)
@Composable
private fun SquarePhotoPickerPreview() {
    val pick = rememberSquarePhotoPicker(onPicked = {})
    Button(onClick = pick) { Text("Escolher foto") }
}
