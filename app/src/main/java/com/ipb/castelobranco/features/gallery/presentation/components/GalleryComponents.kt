package com.ipb.castelobranco.features.gallery.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.alpha
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import com.ipb.castelobranco.features.gallery.presentation.state.AlbumTile
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoImage
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoTile

private val PlaceholderGrey = Color(0xFFBDBDBD)
private const val NAME_OVERLAY_ALPHA = 0.5f
private const val NOT_ON_DEVICE_ALPHA = 0.45f
private const val BADGE_ALPHA = 0.55f
private const val PLAY_LABEL = "Vídeo"
private const val NOT_ON_DEVICE_LABEL = "Não está no aparelho"

/** A square album tile: its cover, or black when there is none, with the name over the bottom. */
@Composable
fun AlbumItem(
    album: AlbumTile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            // Hidden from members (only undownloaded videos below): shown dimmed while organizing.
            .alpha(if (album.onDevice) 1f else NOT_ON_DEVICE_ALPHA)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            if (album.cover != null) {
                AsyncImage(
                    model = album.cover,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(Color.Black.copy(alpha = NAME_OVERLAY_ALPHA))
                    .padding(8.dp),
            ) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (!album.onDevice) NotOnDeviceIcon(Modifier.align(Alignment.TopEnd))
        }
    }
}

/**
 * A photo or video cell: the image, and for a video a play icon with its duration. An undownloaded
 * video (only in "Organizar") is dimmed with a "not on this device" icon.
 */
@Composable
fun PhotoTileImage(
    tile: PhotoTile,
    previewLoader: ImageLoader,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        GalleryImage(
            image = tile.image,
            previewLoader = previewLoader,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().alpha(if (tile.onDevice) 1f else NOT_ON_DEVICE_ALPHA),
        )
        if (tile.isVideo) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(4.dp)
                    .background(Color.Black.copy(alpha = BADGE_ALPHA), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp),
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = PLAY_LABEL,
                    tint = Color.White,
                    modifier = Modifier.size(14.dp),
                )
                tile.duration?.let {
                    Text(text = it, style = MaterialTheme.typography.labelSmall, color = Color.White)
                }
            }
        }
        if (!tile.onDevice) NotOnDeviceIcon(Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun NotOnDeviceIcon(modifier: Modifier) {
    Icon(
        Icons.Outlined.CloudOff,
        contentDescription = NOT_ON_DEVICE_LABEL,
        tint = Color.White,
        modifier = modifier
            .padding(4.dp)
            .background(Color.Black.copy(alpha = BADGE_ALPHA), RoundedCornerShape(4.dp))
            .padding(2.dp)
            .size(16.dp),
    )
}

/**
 * Renders a [PhotoImage]: the original from disk, the server preview through [previewLoader], or a
 * grey placeholder (also when the preview fails to load).
 */
@Composable
fun GalleryImage(
    image: PhotoImage,
    previewLoader: ImageLoader,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    when (image) {
        is PhotoImage.Original -> AsyncImage(
            model = image.file,
            contentDescription = null,
            contentScale = contentScale,
            modifier = modifier,
        )
        // A frame of the local video, decoded by the preview loader's video decoder: works offline.
        is PhotoImage.VideoFrame -> AsyncImage(
            model = image.file,
            imageLoader = previewLoader,
            contentDescription = null,
            contentScale = contentScale,
            placeholder = ColorPainter(Color.Black),
            error = ColorPainter(Color.Black),
            modifier = modifier,
        )
        is PhotoImage.Preview -> AsyncImage(
            model = image.url,
            imageLoader = previewLoader,
            contentDescription = null,
            contentScale = contentScale,
            placeholder = ColorPainter(PlaceholderGrey),
            error = ColorPainter(PlaceholderGrey),
            modifier = modifier,
        )
        PhotoImage.None -> Box(modifier = modifier.background(PlaceholderGrey))
    }
}
