package com.ipb.castelobranco.features.gallery.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.ElasticPullToRefresh
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKey
import com.ipb.castelobranco.features.gallery.domain.trash.TrashKind
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryImage
import com.ipb.castelobranco.features.gallery.presentation.navigation.GalleryNav
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoImage
import com.ipb.castelobranco.features.gallery.presentation.state.TrashEvent
import com.ipb.castelobranco.features.gallery.presentation.state.TrashRow
import com.ipb.castelobranco.features.gallery.presentation.state.TrashUiState
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.TrashTexts
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.TrashViewModel

private val ThumbnailSize = 64.dp

@Composable
fun TrashScreen(viewModel: TrashViewModel, nav: GalleryNav) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is TrashEvent.Close -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                    nav.back()
                }
                is TrashEvent.Message -> {
                    val albumId = event.openAlbumId
                    val result = snackbarHostState.showSnackbar(
                        message = event.text,
                        actionLabel = albumId?.let { TrashTexts.OPEN_ALBUM_LABEL },
                        duration = if (albumId != null) SnackbarDuration.Long else SnackbarDuration.Short,
                    )
                    if (albumId != null && result == SnackbarResult.ActionPerformed) nav.toAlbum(albumId)
                }
            }
        }
    }

    BaseScreen(
        tabName = TrashTexts.TITLE,
        logoRes = R.drawable.ic_galery,
        showBackArrow = true,
        onBackClick = nav.back,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            TrashContent(
                state = state,
                previewLoader = viewModel.previewLoader,
                onRefresh = viewModel::refresh,
                onRetry = viewModel::retry,
                onRestore = viewModel::restore,
            )
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
fun TrashContent(
    state: TrashUiState,
    previewLoader: ImageLoader?,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onRestore: (TrashKey) -> Unit,
) {
    when {
        state.isLoading -> Centered { CircularProgressIndicator() }
        state.error != null -> Centered {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(24.dp))
                OutlinedButton(onClick = onRetry) { Text(TrashTexts.RETRY_LABEL) }
            }
        }
        else -> ElasticPullToRefresh(
            isRefreshing = state.isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            TrashList(state, previewLoader, onRestore)
        }
    }
}

@Composable
private fun TrashList(state: TrashUiState, previewLoader: ImageLoader?, onRestore: (TrashKey) -> Unit) {
    val listState = rememberLazyListState()
    // Rows follow the explanation, so a row's list index is its position + 1.
    LaunchedEffect(state.highlighted) {
        val index = state.rows.indexOfFirst { it.key == state.highlighted }
        if (index >= 0) listState.animateScrollToItem(index + 1)
    }
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                text = TrashTexts.EXPLANATION,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.isEmpty) {
            item {
                Text(
                    text = TrashTexts.EMPTY,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 48.dp),
                )
            }
        }
        items(state.rows, key = { "${it.key.kind}-${it.key.id}" }) { row ->
            TrashRowItem(
                row = row,
                previewLoader = previewLoader,
                isRestoring = state.restoring == row.key,
                canRestore = state.restoring == null,
                isHighlighted = state.highlighted == row.key,
                onRestore = { onRestore(row.key) },
            )
        }
    }
}

@Composable
private fun TrashRowItem(
    row: TrashRow,
    previewLoader: ImageLoader?,
    isRestoring: Boolean,
    canRestore: Boolean,
    isHighlighted: Boolean,
    onRestore: () -> Unit,
) {
    val colors = if (isHighlighted) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    } else {
        CardDefaults.cardColors()
    }
    Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumbnail = Modifier.size(ThumbnailSize).clip(RoundedCornerShape(8.dp))
            if (previewLoader != null) {
                GalleryImage(
                    image = row.thumbnailUrl?.let { PhotoImage.Preview(it) } ?: PhotoImage.None,
                    previewLoader = previewLoader,
                    contentScale = ContentScale.Crop,
                    modifier = thumbnail,
                )
            } else {
                Box(thumbnail)
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = row.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(row.kindLabel, style = MaterialTheme.typography.labelMedium)
                val details = listOfNotNull(row.deletedLine, row.countsLine, row.uploadedLine, row.purgeLine)
                details.forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (isRestoring) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                TextButton(onClick = onRestore, enabled = canRestore) { Text(TrashTexts.RESTORE_LABEL) }
            }
        }
    }
}

@Composable
private fun Centered(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) { content() }
}

@Preview(showBackground = true)
@Composable
private fun TrashContentPreview() {
    val album = TrashKey(TrashKind.ALBUM, 7)
    TrashContent(
        state = TrashUiState(
            isLoading = false,
            rows = listOf(
                TrashRow(
                    key = album,
                    title = "Retiro 2026",
                    kindLabel = "Álbum",
                    deletedLine = "Apagado por Maria Souza em 29/09/2026 11:03",
                    countsLine = "2 subálbuns · 41 fotos",
                    uploadedLine = null,
                    purgeLine = "Some em 29/10/2026",
                    thumbnailUrl = null,
                ),
                TrashRow(
                    key = TrashKey(TrashKind.PHOTO, 301),
                    title = "IMG_0042.jpg",
                    kindLabel = "Foto",
                    deletedLine = "Apagado por usuário desconhecido em 28/09/2026 06:12",
                    countsLine = null,
                    uploadedLine = "Enviada por Ana Paula",
                    purgeLine = "Some em 28/10/2026",
                    thumbnailUrl = null,
                ),
            ),
            highlighted = album,
        ),
        previewLoader = null,
        onRefresh = {},
        onRetry = {},
        onRestore = {},
    )
}
