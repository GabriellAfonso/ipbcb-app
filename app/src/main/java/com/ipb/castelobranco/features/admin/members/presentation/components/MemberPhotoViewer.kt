package com.ipb.castelobranco.features.admin.members.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.ImageLoader
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
                modifier = Modifier
                    .offset(x = lerp(startLeft, endLeft, fraction), y = lerp(startTop, endTop, fraction))
                    .size(side)
                    .clickable(onClickLabel = "Abrir em tela cheia", onClick = onOpenFullScreen),
            )
        }
    }
}

/**
 * The photo (or the initials) full screen, over a dark background. The camera in the top-left
 * corner picks a photo, or removes the current one; the close button and system back leave.
 */
@Composable
fun MemberPhotoViewer(
    initials: String,
    photoUrl: String?,
    imageLoader: ImageLoader?,
    isBusy: Boolean,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDismiss: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
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
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .align(Alignment.Center),
            )
            if (isBusy) CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .safeDrawingPadding()
                    .padding(8.dp),
            ) {
                IconButton(onClick = { menuOpen = true }, enabled = !isBusy, colors = buttonColors) {
                    Icon(Icons.Filled.PhotoCamera, contentDescription = "Trocar foto")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Escolher foto") },
                        onClick = { menuOpen = false; onPickPhoto() },
                    )
                    if (photoUrl != null) {
                        DropdownMenuItem(
                            text = { Text("Remover foto") },
                            onClick = { menuOpen = false; onRemovePhoto() },
                        )
                    }
                }
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
        }
    }
}

private const val BUTTON_ALPHA = 0.5f
private const val ANIMATION_MS = 250
private const val PREVIEW_WIDTH = 0.8f

/** The square's top, as a fraction of the screen height: above the middle. */
private const val PREVIEW_TOP = 0.15f
private const val PREVIEW_CORNER = 8
private const val PREVIEW_INITIALS = 96
private const val FULL_SCREEN_INITIALS = 120
