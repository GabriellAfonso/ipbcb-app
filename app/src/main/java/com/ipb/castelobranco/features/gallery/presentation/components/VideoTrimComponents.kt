package com.ipb.castelobranco.features.gallery.presentation.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.VideoTexts
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.roundToLong

private const val RANGE_CHECK_MS = 50L
private const val TIMELINE_FRAMES = 8
private const val FRAME_SIZE_PX = 160
private const val OUTSIDE_ALPHA = 0.6f
private const val CONTROL_ALPHA = 0.5f
private val TimelineHeight = 56.dp
private val HandleWidth = 3.dp
private val SelectionBorder = 2.dp
private val KnobSize = 16.dp
private val KnobBorder = 2.dp

/** How far from a handle a drag still takes that handle instead of the selection. */
private val HandleGrab = 32.dp

/** Room beside the frames at both ends: the knob and the finger around a handle at the very end. */
private val TouchMargin = 16.dp

/**
 * The preview, without autoplay and with sound. Playback stops at [endMs] and goes back to [startMs].
 * [videoKey] is the video's place in the batch: the same file may be picked twice. The player lets go of
 * its decoders while [isCutting], since phones have few of them.
 */
@OptIn(UnstableApi::class)
@Composable
fun TrimVideoPlayer(
    source: String,
    videoKey: Int,
    startMs: Long,
    endMs: Long,
    previewMs: Long,
    isCutting: Boolean,
    modifier: Modifier = Modifier,
) {
    val player = rememberGalleryPlayer()
    var isPlaying by remember(videoKey) { mutableStateOf(false) }

    DisposableEffect(player, videoKey) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        player.setMediaItem(MediaItem.fromUri(Uri.parse(source)))
        player.playWhenReady = false
        player.prepare()
        onDispose {
            player.removeListener(listener)
            player.pause()
        }
    }
    LaunchedEffect(player, videoKey, previewMs) {
        player.pause()
        player.seekTo(previewMs)
    }
    LaunchedEffect(player, isCutting) {
        if (isCutting) {
            player.stop()
        } else if (player.playbackState == Player.STATE_IDLE) {
            player.prepare()
        }
    }
    LaunchedEffect(player, isPlaying, startMs, endMs) {
        while (isPlaying) {
            if (player.currentPosition >= endMs) {
                player.pause()
                player.seekTo(startMs)
            }
            delay(RANGE_CHECK_MS)
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    useController = false
                    setKeepContentOnPlayerReset(true)
                    this.player = player
                }
            },
            onRelease = { view -> view.player = null },
            modifier = Modifier.fillMaxSize(),
        )
        if (!isCutting) {
            IconButton(
                onClick = { togglePlayback(player, isPlaying, startMs, endMs) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = Color.Black.copy(alpha = CONTROL_ALPHA)),
                modifier = Modifier.align(Alignment.Center).size(56.dp),
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) VideoTexts.PAUSE else VideoTexts.PLAY,
                    tint = Color.White,
                )
            }
        }
    }
}

private fun togglePlayback(player: ExoPlayer, isPlaying: Boolean, startMs: Long, endMs: Long) {
    if (isPlaying) {
        player.pause()
        return
    }
    val isOutside = player.currentPosition !in startMs until endMs
    if (isOutside || player.playbackState == Player.STATE_ENDED) player.seekTo(startMs)
    player.play()
}

private enum class TrimDrag { START, END, SELECTION }

/**
 * Frames of the video under two handles, each a thin line with a knob in the middle. Dragging near a handle moves
 * it; dragging inside the selection slides the whole selection, keeping its length; dragging outside moves the
 * nearer handle. Outside the selection the frames are dimmed.
 */
@Composable
fun TrimTimeline(
    source: String,
    durationMs: Long,
    startMs: Long,
    endMs: Long,
    enabled: Boolean,
    previewLoader: ImageLoader,
    onRangeChange: (startMs: Long, endMs: Long) -> Unit,
    onMoveSelection: (startMs: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val frameTimes = remember(durationMs) { List(TIMELINE_FRAMES) { durationMs * it / TIMELINE_FRAMES } }
    val outside = MaterialTheme.colorScheme.scrim.copy(alpha = OUTSIDE_ALPHA)
    val handle = MaterialTheme.colorScheme.primary
    val currentStart by rememberUpdatedState(startMs)
    val currentEnd by rememberUpdatedState(endMs)
    val currentOnRangeChange by rememberUpdatedState(onRangeChange)
    val currentOnMoveSelection by rememberUpdatedState(onMoveSelection)

    // The frames start [TouchMargin] in, so a handle at either end still has room around it to be grabbed: touches
    // are read on the whole box, before that padding. The handles sit near the screen edges, where the system back
    // gesture would take the drag: the timeline is excluded from it.
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TimelineHeight)
            .systemGestureExclusion()
            .pointerInput(enabled, durationMs) {
                if (!enabled || durationMs <= 0) return@pointerInput
                val grab = HandleGrab.toPx()
                val margin = TouchMargin.toPx()
                val track = (size.width - 2 * margin).coerceAtLeast(1f)
                var target = TrimDrag.SELECTION
                var originStart = 0L
                var originEnd = 0L
                var dragged = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        val x = offset.x - margin
                        val startX = track * currentStart / durationMs
                        val endX = track * currentEnd / durationMs
                        val toStart = abs(x - startX)
                        val toEnd = abs(x - endX)
                        target = when {
                            minOf(toStart, toEnd) <= grab -> if (toStart < toEnd) TrimDrag.START else TrimDrag.END
                            x in startX..endX -> TrimDrag.SELECTION
                            x < startX -> TrimDrag.START
                            else -> TrimDrag.END
                        }
                        originStart = currentStart
                        originEnd = currentEnd
                        dragged = 0f
                    },
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        dragged += amount
                        val deltaMs = (dragged / track * durationMs).roundToLong()
                        when (target) {
                            TrimDrag.START -> currentOnRangeChange(originStart + deltaMs, currentEnd)
                            TrimDrag.END -> currentOnRangeChange(currentStart, originEnd + deltaMs)
                            TrimDrag.SELECTION -> currentOnMoveSelection(originStart + deltaMs)
                        }
                    },
                )
            }
            .padding(horizontal = TouchMargin),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            frameTimes.forEach { timeMs ->
                AsyncImage(
                    model = remember(source, timeMs) {
                        ImageRequest.Builder(context)
                            .data(Uri.parse(source))
                            .videoFrameMillis(timeMs)
                            .size(FRAME_SIZE_PX)
                            .build()
                    },
                    imageLoader = previewLoader,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                )
            }
        }
        Canvas(modifier = Modifier.fillMaxSize()) {
            val startX = size.width * startMs / durationMs
            val endX = size.width * endMs / durationMs
            drawRect(outside, size = Size(startX, size.height))
            drawRect(outside, topLeft = Offset(endX, 0f), size = Size(size.width - endX, size.height))
            // A thin frame along the selection joins the two handles.
            val border = SelectionBorder.toPx()
            drawRect(handle, topLeft = Offset(startX, 0f), size = Size(endX - startX, border))
            drawRect(handle, topLeft = Offset(startX, size.height - border), size = Size(endX - startX, border))
            listOf(startX, endX).forEach { x -> drawTrimHandle(x, handle) }
        }
    }
}

/** A "knife": a thin line across the frames with a round knob in the middle to grab. */
private fun DrawScope.drawTrimHandle(x: Float, color: Color) {
    val line = HandleWidth.toPx()
    drawRect(color, topLeft = Offset(x - line / 2, 0f), size = Size(line, size.height))
    val center = Offset(x, size.height / 2)
    drawCircle(color, radius = KnobSize.toPx() / 2, center = center)
    drawCircle(Color.White, radius = KnobSize.toPx() / 2, center = center, style = Stroke(KnobBorder.toPx()))
}

