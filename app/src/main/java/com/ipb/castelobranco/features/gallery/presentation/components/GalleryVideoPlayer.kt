package com.ipb.castelobranco.features.gallery.presentation.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
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
 * play/pause, seek bar and the repeat toggle (off by default) — plus a sound button over them.
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
) {
    var failed by remember(file) { mutableStateOf(false) }

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
                }
            },
            update = { view -> view.player = if (isActive) player else null },
            onRelease = { view -> view.player = null },
            modifier = Modifier.fillMaxSize(),
        )
        IconButton(
            onClick = { onMutedChange(!muted) },
            colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = CONTROL_ALPHA)),
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Icon(
                imageVector = if (muted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (muted) UNMUTE_LABEL else MUTE_LABEL,
                tint = Color.White,
            )
        }
    }
}
