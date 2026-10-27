package com.ipb.castelobranco.features.bible.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ipb.castelobranco.features.bible.domain.model.BiblePlace
import com.ipb.castelobranco.features.bible.domain.model.BibleVideo
import com.ipb.castelobranco.features.bible.domain.model.BibleVideoKind
import com.ipb.castelobranco.features.bible.presentation.viewmodel.BibleVideosUiState
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** Everything the sheet can ask for; the screen wires each to the ViewModel or to an intent. */
data class BibleVideosSheetActions(
    val onDismiss: () -> Unit,
    val onOpen: (BibleVideo) -> Unit,
    val onAdd: (url: String) -> Unit,
    val onClearAddError: () -> Unit,
    val onEditPlaces: (BibleVideo) -> Unit,
    val onRemoveFromPlace: (BibleVideo, BiblePlace) -> Unit,
    val onDelete: (BibleVideo) -> Unit,
    val onStartReorder: (BiblePlace) -> Unit,
    val onMove: (from: Int, to: Int) -> Unit,
    val onDragStopped: () -> Unit,
    val onStopReorder: () -> Unit,
    val onToggleWatched: (BibleVideo) -> Unit,
)

private const val WATCHED_ALPHA = 0.5f
private const val REMOVE_FROM_BOOK = "Remover deste livro"
private const val REMOVE_FROM_CHAPTER = "Remover deste capítulo"
private const val THUMB_WIDTH_DP = 136

/** The current chapter's and book's videos, opened from the play icon of the reader. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BibleVideosSheet(
    state: BibleVideosUiState,
    actions: BibleVideosSheetActions,
) {
    var showAddDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<BibleVideo?>(null) }

    ModalBottomSheet(
        onDismissRequest = actions.onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        val reorder = state.reorder
        if (reorder != null) {
            ReorderList(state = state, actions = actions)
        } else {
            VideoList(
                state = state,
                actions = actions,
                onAddClick = { showAddDialog = true },
                onDeleteClick = { pendingDelete = it },
            )
        }
    }

    if (showAddDialog) {
        AddVideoDialog(
            isAdding = state.isAdding,
            error = state.addError,
            onConfirm = actions.onAdd,
            onDismiss = {
                showAddDialog = false
                actions.onClearAddError()
            },
        )
        // Closes once a send finished without error (the places screen opens on top).
        var wasAdding by remember { mutableStateOf(false) }
        LaunchedEffect(state.isAdding, state.addError) {
            if (wasAdding && !state.isAdding && state.addError == null) showAddDialog = false
            wasAdding = state.isAdding
        }
    }

    pendingDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Excluir vídeo?") },
            text = { Text("O vídeo sai de todos os lugares onde aparece.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    actions.onDelete(video)
                }) { Text("Excluir") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun VideoList(
    state: BibleVideosUiState,
    actions: BibleVideosSheetActions,
    onAddClick: () -> Unit,
    onDeleteClick: (BibleVideo) -> Unit,
) {
    val chapterPlace = state.chapterPlace ?: return
    val bookPlace = BiblePlace.of(chapterPlace.book, null)
    LazyColumn(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        if (state.sections.isEmpty) {
            item(key = "empty") {
                Text(
                    text = "Nenhum vídeo aqui ainda.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        listOf(
            Triple(state.chapterTitle, chapterPlace, state.sections.chapter),
            Triple(state.bookName, bookPlace, state.sections.book),
        ).forEach { (title, place, videos) ->
            if (videos.isEmpty()) return@forEach
            item(key = "header-${place.chapter}") {
                SectionHeader(
                    title = title,
                    canReorder = state.canManage && videos.size > 1,
                    onReorder = { actions.onStartReorder(place) },
                )
            }
            items(videos, key = { "${place.chapter}-${it.id}" }) { video ->
                VideoCard(
                    video = video,
                    watched = state.progress?.isWatched(video.id),
                    onClick = { actions.onOpen(video) },
                    onToggleWatched = { actions.onToggleWatched(video) },
                    menu = if (state.canManage) {
                        {
                            VideoMenu(
                                removeLabel = if (place.isWholeBook) REMOVE_FROM_BOOK else REMOVE_FROM_CHAPTER,
                                canDelete = state.canDelete,
                                onEditPlaces = { actions.onEditPlaces(video) },
                                onRemove = { actions.onRemoveFromPlace(video, place) },
                                onDelete = { onDeleteClick(video) },
                            )
                        }
                    } else {
                        null
                    },
                )
            }
        }
        if (state.canManage) {
            item(key = "add") {
                TextButton(
                    onClick = onAddClick,
                    enabled = !state.isBusy,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text("Adicionar link", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, canReorder: Boolean, onReorder: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        if (canReorder) TextButton(onClick = onReorder) { Text("Reordenar") }
    }
}

/** [watched] is null when signed out (no check). [menu] only for managers. */
@Composable
private fun VideoCard(
    video: BibleVideo,
    watched: Boolean?,
    onClick: () -> Unit,
    onToggleWatched: () -> Unit,
    menu: (@Composable () -> Unit)?,
    dragHandle: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        dragHandle?.invoke()
        VideoThumbnail(video = video, watched = watched == true)
        Text(
            text = video.title,
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (watched != null) {
            IconButton(onClick = onToggleWatched) {
                Icon(
                    imageVector = if (watched) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                    contentDescription = if (watched) "Desmarcar como visto" else "Marcar como visto",
                    tint = if (watched) MaterialTheme.colorScheme.primary else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        menu?.invoke()
    }
}

@Composable
private fun VideoThumbnail(video: BibleVideo, watched: Boolean) {
    Box(
        modifier = Modifier
            .width(THUMB_WIDTH_DP.dp)
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.SmartDisplay,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize().alpha(if (watched) WATCHED_ALPHA else 1f),
        )
        Row(
            modifier = Modifier.align(Alignment.BottomStart).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (video.kind == BibleVideoKind.PLAYLIST) Badge("Playlist")
            if (watched) Badge("Visto")
        }
    }
}

@Composable
private fun Badge(text: String) {
    Surface(color = Color.Black.copy(alpha = 0.75f), shape = RoundedCornerShape(4.dp)) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Composable
private fun VideoMenu(
    removeLabel: String,
    canDelete: Boolean,
    onEditPlaces: () -> Unit,
    onRemove: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = "Mais opções")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Editar locais") }, onClick = {
                expanded = false
                onEditPlaces()
            })
            DropdownMenuItem(text = { Text(removeLabel) }, onClick = {
                expanded = false
                onRemove()
            })
            if (canDelete) {
                DropdownMenuItem(text = { Text("Excluir vídeo") }, onClick = {
                    expanded = false
                    onDelete()
                })
            }
        }
    }
}

/** One section with drag handles; the order is saved when a drag stops. */
@Composable
private fun ReorderList(state: BibleVideosUiState, actions: BibleVideosSheetActions) {
    val reorder = state.reorder ?: return
    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        actions.onMove(from.index, to.index)
    }
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (reorder.place.isWholeBook) state.bookName else state.chapterTitle,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            if (state.isBusy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            TextButton(onClick = actions.onStopReorder) { Text("Concluir") }
        }
        LazyColumn(state = listState, contentPadding = PaddingValues(bottom = 16.dp)) {
            items(reorder.videos, key = { it.id }) { video ->
                ReorderableItem(reorderState, key = video.id) {
                    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
                        VideoCard(
                            video = video,
                            watched = null,
                            onClick = {},
                            onToggleWatched = {},
                            menu = null,
                            dragHandle = {
                                Icon(
                                    Icons.Filled.DragHandle,
                                    contentDescription = "Arrastar",
                                    modifier = Modifier
                                        .draggableHandle(onDragStopped = actions.onDragStopped)
                                        .padding(end = 8.dp),
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddVideoDialog(
    isAdding: Boolean,
    error: String?,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isAdding) onDismiss() },
        title = { Text("Adicionar vídeo") },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text("Link do YouTube") },
                singleLine = true,
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                enabled = !isAdding,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(url) }, enabled = url.isNotBlank() && !isAdding) {
                if (isAdding) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Adicionar")
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !isAdding) { Text("Cancelar") } },
    )
}
