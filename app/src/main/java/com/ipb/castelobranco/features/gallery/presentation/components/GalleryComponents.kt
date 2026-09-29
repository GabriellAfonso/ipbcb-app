package com.ipb.castelobranco.features.gallery.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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

private val PlaceholderGrey = Color(0xFFBDBDBD)
private const val NAME_OVERLAY_ALPHA = 0.5f

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
        }
    }
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
