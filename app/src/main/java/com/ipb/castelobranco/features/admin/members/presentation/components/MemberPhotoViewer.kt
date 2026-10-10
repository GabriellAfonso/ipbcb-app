package com.ipb.castelobranco.features.admin.members.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import kotlinx.coroutines.launch

/**
 * The photo (or the initials) popped in front of the screen as a square over a dimmed background,
 * like a chat app's profile photo. It grows out of [origin] — where the photo sits on the profile,
 * in screen coordinates — to a square above the middle, and shrinks back there when it closes.
 * A tap on it asks for full screen; a tap outside or back closes.
 *
 * @param originInitialsSize the initials' size on the profile, so they grow along with the square.
 */
@Composable
fun MemberPhotoPreview(
    initials: String,
    photoUrl: String?,
    imageLoader: ImageLoader?,
    origin: Rect?,
    originInitialsSize: TextUnit,
    onOpenFullScreen: () -> Unit,
    onDismiss: () -> Unit,
    photoRevision: Int = 0,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val close: () -> Unit = {
        scope.launch {
            progress.animateTo(0f, tween(ANIMATION_MS))
            currentOnDismiss()
        }
    }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(ANIMATION_MS)) }

    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // The dialog window may not start at the top of the screen: measure where it does.
        var windowOffset by remember { mutableStateOf<Offset?>(null) }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { windowOffset = it.positionOnScreen() }
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                    close()
                },
        ) {
            val offset = windowOffset ?: return@BoxWithConstraints
            val density = LocalDensity.current
            val endSide = maxWidth * PREVIEW_WIDTH
            val endLeft = (maxWidth - endSide) / 2
            val endTop = maxHeight * PREVIEW_TOP
            val (startLeft, startTop, startSide) = with(density) {
                origin?.let {
                    Triple((it.left - offset.x).toDp(), (it.top - offset.y).toDp(), it.width.toDp())
                }
            } ?: Triple(endLeft + endSide / 4, endTop + endSide / 4, endSide / 2)

            val fraction = progress.value
            val side = lerp(startSide, endSide, fraction)
            MemberAvatar(
                initials = initials,
                photoUrl = photoUrl,
                imageLoader = imageLoader,
                shape = RoundedCornerShape(lerp(startSide / 2, PREVIEW_CORNER.dp, fraction)),
                initialsSize = lerp(originInitialsSize, PREVIEW_INITIALS.sp, fraction),
                photoRevision = photoRevision,
                modifier = Modifier
                    .offset(x = lerp(startLeft, endLeft, fraction), y = lerp(startTop, endTop, fraction))
                    .size(side)
                    .clickable(onClickLabel = "Abrir em tela cheia", onClick = onOpenFullScreen),
            )
        }
    }
}

/**
 * The photo (or the initials) full screen, over a dark background. "Trocar foto" in the top-left
 * corner opens the picker straight away; the close button (top-right) and system back leave. At the
 * bottom, side by side and centred, "Apagar" removes the photo and "Baixar" saves it to the device
 * gallery — both only when there is a photo
 * (`specs/012-encrypted-session-photo-cache/contracts/photo-viewer-ui.md`).
 *
 * @param onPickPhoto null when the user may not change the photo (`manage` on `members`).
 * @param onRemovePhoto null when the user may not remove it (`owner` on `members`).
 * @param onDownload null when the user may not download it (`manage` on `members`).
 * @param isBusy an upload, removal or download is running: every action but closing waits.
 */
@Composable
fun MemberPhotoViewer(
    initials: String,
    photoUrl: String?,
    imageLoader: ImageLoader?,
    isBusy: Boolean,
    onPickPhoto: (() -> Unit)?,
    onRemovePhoto: (() -> Unit)?,
    onDownload: (() -> Unit)?,
    onDismiss: () -> Unit,
    photoRevision: Int = 0,
) {
    val canRemove = onRemovePhoto != null && photoUrl != null
    val canDownload = onDownload != null && photoUrl != null
    val buttonColors = IconButtonDefaults.iconButtonColors(
        containerColor = Color.Black.copy(alpha = BUTTON_ALPHA),
        contentColor = Color.White,
    )
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            MemberAvatar(
                initials = initials,
                photoUrl = photoUrl,
                imageLoader = imageLoader,
                shape = RectangleShape,
                initialsSize = FULL_SCREEN_INITIALS.sp,
                photoRevision = photoRevision,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .align(Alignment.Center),
            )
            if (isBusy) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            if (onPickPhoto != null) {
                ViewerTextButton(
                    label = "Trocar foto",
                    icon = Icons.Filled.Edit,
                    enabled = !isBusy,
                    onClick = onPickPhoto,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .safeDrawingPadding()
                        .padding(8.dp),
                )
            }
            IconButton(
                onClick = onDismiss,
                colors = buttonColors,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .safeDrawingPadding()
                    .padding(8.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Fechar")
            }
            if (canRemove || canDownload) Row(
                horizontalArrangement = Arrangement.spacedBy(BOTTOM_GAP.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .safeDrawingPadding()
                    .padding(BOTTOM_PADDING.dp),
            ) {
                if (canRemove) {
                    ViewerTextButton("Apagar", Icons.Filled.Delete, enabled = !isBusy) { onRemovePhoto?.invoke() }
                }
                if (canDownload) {
                    ViewerTextButton("Baixar", Icons.Filled.Download, enabled = !isBusy) { onDownload?.invoke() }
                }
            }
        }
    }
}

/** A translucent pill with an icon and a label, readable over any photo. */
@Composable
private fun ViewerTextButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(
            containerColor = Color.Black.copy(alpha = BUTTON_ALPHA),
            contentColor = Color.White,
            disabledContainerColor = Color.Black.copy(alpha = BUTTON_ALPHA),
            disabledContentColor = Color.White.copy(alpha = DISABLED_ALPHA),
        ),
        contentPadding = PaddingValues(start = 12.dp, end = 16.dp),
        modifier = modifier,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(label, modifier = Modifier.padding(start = 8.dp))
    }
}

@Preview(name = "Viewer · Admin (owner)")
@Composable
private fun MemberPhotoViewerOwnerPreview() = ViewerPreview(canRemove = true, canDownload = true)

@Preview(name = "Viewer · Liderança (manage)")
@Composable
private fun MemberPhotoViewerManagePreview() = ViewerPreview(canRemove = false, canDownload = true)

@Preview(name = "Viewer · só ver")
@Composable
private fun MemberPhotoViewerViewOnlyPreview() =
    ViewerPreview(canRemove = false, canDownload = false, canPick = false)

@Preview(name = "Viewer · sem foto")
@Composable
private fun MemberPhotoViewerNoPhotoPreview() =
    ViewerPreview(canRemove = true, canDownload = true, photoUrl = null)

@Composable
private fun ViewerPreview(
    canRemove: Boolean,
    canDownload: Boolean,
    canPick: Boolean = true,
    photoUrl: String? = "https://example.com/members/foto.jpg",
) {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        MemberPhotoViewer(
            initials = "AS",
            photoUrl = photoUrl,
            imageLoader = null,
            isBusy = false,
            onPickPhoto = {}.takeIf { canPick },
            onRemovePhoto = {}.takeIf { canRemove },
            onDownload = {}.takeIf { canDownload },
            onDismiss = {},
        )
    }
}

private const val BUTTON_ALPHA = 0.5f
private const val DISABLED_ALPHA = 0.5f
private const val BOTTOM_GAP = 16
private const val BOTTOM_PADDING = 16
private const val ANIMATION_MS = 250
private const val PREVIEW_WIDTH = 0.8f

/** The square's top, as a fraction of the screen height: above the middle. */
private const val PREVIEW_TOP = 0.15f
private const val PREVIEW_CORNER = 8
private const val PREVIEW_INITIALS = 96
private const val FULL_SCREEN_INITIALS = 120
