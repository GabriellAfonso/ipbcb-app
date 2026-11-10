package com.ipb.castelobranco.features.gallery.presentation.components

import android.net.Uri
import android.view.View
import android.view.ViewTreeObserver
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.RepeatModeUtil
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import java.io.File

private const val MUTE_LABEL = "Desativar som"
private const val UNMUTE_LABEL = "Ativar som"
private const val CONTROL_ALPHA = 0.5f

/**
 * One player for the whole viewer: only the settled video page uses it, so a single decoder is ever
 * alive. Released when the viewer leaves.
 */
@Composable
fun rememberGalleryPlayer(): ExoPlayer {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}

/**
 * A video page. While [isActive] (the pager settled on it) it owns [player]: loads the local [file],
 * starts playing, muted or not as [muted] says; leaving the page pauses. Controls are Media3's own —
 * play/pause, seek bar and the repeat toggle (off by default) — plus a sound button shown with them.
 * The controls sit at the very bottom, right above the navigation bar; [onControlsChange] tells the viewer
 * whether they show and how tall they are above that bar, so its own buttons can step up over them.
 * [topInset] is the viewer's top bar: the sound button goes under it.
 */
@OptIn(UnstableApi::class)
@Composable
fun GalleryVideoPage(
    file: File,
    player: ExoPlayer,
    isActive: Boolean,
    muted: Boolean,
    onMutedChange: (Boolean) -> Unit,
    failedText: String,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    onControlsChange: (VideoControlsLayout) -> Unit = {},
) {
    var failed by remember(file) { mutableStateOf(false) }
    var layout by remember(file) { mutableStateOf(VideoControlsLayout(visible = true, heightPx = 0)) }
    val currentIsActive by rememberUpdatedState(isActive)
    val currentOnControlsChange by rememberUpdatedState(onControlsChange)
    val navigationBarPx = WindowInsets.navigationBars.getBottom(LocalDensity.current)
    val report = { next: VideoControlsLayout ->
        layout = next
        if (currentIsActive) currentOnControlsChange(next)
    }
    LaunchedEffect(isActive) {
        if (isActive) onControlsChange(layout)
    }

    DisposableEffect(player, file, isActive) {
        if (!isActive) return@DisposableEffect onDispose { }
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                failed = true
            }
        }
        player.addListener(listener)
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.prepare()
        player.playWhenReady = true
        onDispose {
            player.removeListener(listener)
            player.pause()
        }
    }
    LaunchedEffect(player, muted, isActive) {
        if (isActive) player.volume = if (muted) 0f else 1f
    }

    Box(modifier = modifier.background(Color.Black)) {
        if (failed) {
            Text(
                text = failedText,
                color = Color.White,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            return@Box
        }
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    setRepeatToggleModes(RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    val controller = findViewById<View>(androidx.media3.ui.R.id.exo_controller)
                    val timeBar = findViewById<View>(androidx.media3.ui.R.id.exo_progress)
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            val visible = visibility == View.VISIBLE
                            report(layout.copy(visible = visible, heightPx = if (visible) layout.heightPx else 0))
                        },
                    )
                    // Media3 slides its bars with translationY: read where the seek bar is drawn on every frame,
                    // so the viewer's buttons follow it down as closely as they follow it up.
                    val followSeekBar = ViewTreeObserver.OnPreDrawListener {
                        if (controller != null && timeBar != null) {
                            val height = if (controller.visibility == View.VISIBLE) {
                                val drawnTop = timeBar.topIn(controller) + timeBar.translationY.toInt()
                                (controller.height - controller.paddingBottom - drawnTop).coerceAtLeast(0)
                            } else {
                                0
                            }
                            if (height != layout.heightPx) report(layout.copy(heightPx = height))
                        }
                        true
                    }
                    // The observer belongs to the window: only listen while this page is attached to it.
                    addOnAttachStateChangeListener(
                        object : View.OnAttachStateChangeListener {
                            override fun onViewAttachedToWindow(view: View) {
                                view.viewTreeObserver.addOnPreDrawListener(followSeekBar)
                            }

                            override fun onViewDetachedFromWindow(view: View) {
                                view.viewTreeObserver.removeOnPreDrawListener(followSeekBar)
                            }
                        },
                    )
                }
            },
            update = { view ->
                view.player = if (isActive) player else null
                // The seek bar stays clear of the navigation bar; the video keeps the whole screen.
                view.findViewById<View>(androidx.media3.ui.R.id.exo_controller)
                    ?.setPadding(0, 0, 0, navigationBarPx)
            },
            onRelease = { view -> view.player = null },
            modifier = Modifier.fillMaxSize(),
        )
        AnimatedVisibility(
            visible = layout.visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd).padding(top = topInset).padding(8.dp),
        ) {
            IconButton(
                onClick = { onMutedChange(!muted) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = CONTROL_ALPHA)),
            ) {
                Icon(
                    imageVector = if (muted) {
                        Icons.AutoMirrored.Filled.VolumeOff
                    } else {
                        Icons.AutoMirrored.Filled.VolumeUp
                    },
                    contentDescription = if (muted) UNMUTE_LABEL else MUTE_LABEL,
                    tint = Color.White,
                )
            }
        }
    }
}

/**
 * Whether the player's controls show, and how much of them is drawn above the navigation bar right now, in
 * pixels: it shrinks frame by frame while they slide away.
 */
data class VideoControlsLayout(val visible: Boolean, val heightPx: Int)

/** [View.getTop] counted from [ancestor] instead of the direct parent. */
private fun View.topIn(ancestor: View): Int {
    var y = top
    var parent = parent
    while (parent is View && parent !== ancestor) {
        y += parent.top
        parent = parent.parent
    }
    return y
}
