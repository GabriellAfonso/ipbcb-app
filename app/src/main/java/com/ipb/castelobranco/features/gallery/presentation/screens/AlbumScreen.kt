package com.ipb.castelobranco.features.gallery.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.gallery.presentation.components.AlbumItem
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryImage
import com.ipb.castelobranco.features.gallery.presentation.navigation.GalleryNav
import com.ipb.castelobranco.features.gallery.presentation.state.AlbumUiState
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryViewModel

/** 6 columns: a sub-album spans 3 (two per row), a photo spans 2 (three per row). */
private const val GRID_COLUMNS = 6
private const val ALBUM_SPAN = 3
private const val PHOTO_SPAN = 2
private const val DEFAULT_TITLE = "Álbum"
private const val EMPTY_MESSAGE = "Nenhuma foto neste álbum."
private const val HEADER_KEY = "header"
private const val ALBUM_KEY_PREFIX = "album-"
private const val PHOTO_KEY_PREFIX = "photo-"

@Composable
fun AlbumScreen(
    albumId: Long,
    viewModel: GalleryViewModel,
    nav: GalleryNav,
) {
    val state by viewModel.albumState(albumId).collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    GalleryMessageEffect(message, viewModel::consumeMessage)

    // Álbum apagado no servidor: sobe um nível. Se o de baixo também saiu, ele faz o mesmo.
    LaunchedEffect(state.isRemoved) {
        if (state.isRemoved) nav.back()
    }

    AlbumContent(
        state = state,
        previewLoader = viewModel.previewLoader,
        onBack = nav.back,
        onAlbumClick = nav.toAlbum,
        onPhotoClick = { photoId -> nav.toPhoto(albumId, photoId) },
    )
}

@Composable
fun AlbumContent(
    state: AlbumUiState,
    previewLoader: ImageLoader,
    onBack: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onPhotoClick: (Long) -> Unit,
) {
    BaseScreen(
        tabName = state.title.ifBlank { DEFAULT_TITLE },
        logoRes = R.drawable.ic_galery,
        showBackArrow = true,
        onBackClick = onBack,
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                state.isLoading || state.isRemoved -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(GRID_COLUMNS),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = HEADER_KEY, span = { GridItemSpan(maxLineSpan) }) {
                        AlbumHeader(state)
                    }
                    items(
                        state.subAlbums,
                        key = { ALBUM_KEY_PREFIX + it.id },
                        span = { GridItemSpan(ALBUM_SPAN) },
                    ) { album ->
                        AlbumItem(album = album, onClick = { onAlbumClick(album.id) })
                    }
                    items(
                        state.photos,
                        key = { PHOTO_KEY_PREFIX + it.id },
                        span = { GridItemSpan(PHOTO_SPAN) },
                    ) { photo ->
                        GalleryImage(
                            image = photo.image,
                            previewLoader = previewLoader,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(1f)
                                .clickable { onPhotoClick(photo.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AlbumHeader(state: AlbumUiState) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        state.subtitle?.let {
            Text(text = it, style = MaterialTheme.typography.labelMedium)
        }
        state.eventDate?.let {
            Text(text = it, style = MaterialTheme.typography.bodySmall)
        }
        state.description?.let {
            Text(text = it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
        }
        if (state.isEmpty) {
            Text(
                text = EMPTY_MESSAGE,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
            )
        }
    }
}
