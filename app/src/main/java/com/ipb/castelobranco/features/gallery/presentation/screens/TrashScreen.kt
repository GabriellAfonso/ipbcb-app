package com.ipb.castelobranco.features.gallery.presentation.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
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

private const val MUTED_ALPHA = 0.75f
private const val TEXT_ALPHA = 0.85f

private val ThumbnailSize = 48.dp
private val RestoreButtonSize = 40.dp
private val BandCorner = 28.dp
private val SoonDot = Color(0xFFF2A300)

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

/** What the list shows, in order; the highlighted row is found by its place here. */
private sealed interface TrashItem {
    val key: String

    data object Band : TrashItem {
        override val key = "band"
    }

    data class Group(val title: String, val isSoon: Boolean) : TrashItem {
        override val key = "group-$title"
    }

    data class Entry(val row: TrashRow) : TrashItem {
        override val key = "${row.key.kind}-${row.key.id}"
    }

    data object Footer : TrashItem {
        override val key = "footer"
    }
}

private fun trashItems(state: TrashUiState): List<TrashItem> = buildList {
    add(TrashItem.Band)
    if (state.soonRows.isNotEmpty()) {
        add(TrashItem.Group(TrashTexts.SOON_GROUP, isSoon = true))
        state.soonRows.forEach { add(TrashItem.Entry(it)) }
    }
    if (state.laterRows.isNotEmpty()) {
        add(TrashItem.Group(TrashTexts.LATER_GROUP, isSoon = false))
        state.laterRows.forEach { add(TrashItem.Entry(it)) }
    }
    add(TrashItem.Footer)
}

@Composable
private fun TrashList(state: TrashUiState, previewLoader: ImageLoader?, onRestore: (TrashKey) -> Unit) {
    val listState = rememberLazyListState()
    val items = trashItems(state)
    LaunchedEffect(state.highlighted) {
        val index = items.indexOfFirst { it is TrashItem.Entry && it.row.key == state.highlighted }
        if (index >= 0) listState.animateScrollToItem(index)
    }
    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        items(items, key = { it.key }) { item ->
            when (item) {
                TrashItem.Band -> TrashBand(total = state.rows.size, soon = state.soonRows.size)
                is TrashItem.Group -> GroupTitle(item.title, item.isSoon)
                is TrashItem.Entry -> TrashRowItem(
                    row = item.row,
                    previewLoader = previewLoader,
                    isRestoring = state.restoring == item.row.key,
                    canRestore = state.restoring == null,
                    isHighlighted = state.highlighted == item.row.key,
                    onRestore = { onRestore(item.row.key) },
                )
                TrashItem.Footer -> TrashFooter(isEmpty = state.isEmpty)
            }
        }
    }
}

/** "Lixeira", the item count and how many go this week. */
@Composable
private fun TrashBand(total: Int, soon: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = BandCorner, bottomEnd = BandCorner))
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 22.dp),
    ) {
        Text(
            text = TrashTexts.TITLE,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = MUTED_ALPHA),
        )
        Text(
            text = TrashTexts.itemCount(total),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Text(
            text = TrashTexts.soonLine(soon),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = TEXT_ALPHA),
        )
    }
}

@Composable
private fun GroupTitle(title: String, isSoon: Boolean) {
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSoon) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(SoonDot))
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    val background = if (isHighlighted) MaterialTheme.colorScheme.tertiaryContainer else Color.Transparent
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val thumbnail = Modifier.size(ThumbnailSize).clip(RoundedCornerShape(10.dp))
        if (previewLoader != null) {
            GalleryImage(
                image = row.thumbnailUrl?.let { PhotoImage.Preview(it) } ?: PhotoImage.None,
                previewLoader = previewLoader,
                contentScale = ContentScale.Crop,
                modifier = thumbnail,
            )
        } else {
            Box(thumbnail.background(MaterialTheme.colorScheme.surfaceVariant))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = row.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            listOf(row.kindLine, row.detailLine).forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (isRestoring) {
            Box(Modifier.size(RestoreButtonSize), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        } else {
            FilledTonalIconButton(
                onClick = onRestore,
                enabled = canRestore,
                modifier = Modifier.size(RestoreButtonSize),
            ) {
                Icon(Icons.Filled.Restore, contentDescription = TrashTexts.RESTORE_LABEL)
            }
        }
    }
}

/** The 30-day rule at the bottom, or the empty text. */
@Composable
private fun TrashFooter(isEmpty: Boolean) {
    if (isEmpty) {
        Text(
            text = TrashTexts.EMPTY,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
        )
    }
    Text(
        text = TrashTexts.EXPLANATION,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 40.dp),
    )
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
                    kindLine = "Álbum · 2 subálbuns · 41 fotos",
                    detailLine = "Apagado por Maria em 29/09 · some em 29/10",
                    isPurgeSoon = true,
                    thumbnailUrl = null,
                ),
                TrashRow(
                    key = TrashKey(TrashKind.PHOTO, 301),
                    title = "IMG_0042.jpg",
                    kindLine = "Foto",
                    detailLine = "Apagado por usuário desconhecido em 20/10 · some em 19/11",
                    isPurgeSoon = false,
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
