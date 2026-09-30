package com.ipb.castelobranco.features.gallery.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.gallery.presentation.components.AlbumItem
import com.ipb.castelobranco.features.gallery.presentation.components.AlbumUploadsPanel
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryDialogHost
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryGridKeys
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryImage
import com.ipb.castelobranco.features.gallery.presentation.components.ManageAction
import com.ipb.castelobranco.features.gallery.presentation.components.ManageOverflowMenu
import com.ipb.castelobranco.features.gallery.presentation.components.OrganizeActions
import com.ipb.castelobranco.features.gallery.presentation.components.SelectionActions
import com.ipb.castelobranco.features.gallery.presentation.navigation.GalleryNav
import com.ipb.castelobranco.features.gallery.presentation.state.AlbumUiState
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/** 6 columns: a sub-album spans 3 (two per row), a photo spans 2 (three per row). */
private const val GRID_COLUMNS = 6
private const val ALBUM_SPAN = 3
private const val PHOTO_SPAN = 2
private const val DEFAULT_TITLE = "Álbum"
private const val EMPTY_MESSAGE = "Nenhuma foto neste álbum."
private const val ORGANIZE_TITLE = "Organizar"
private const val ORGANIZE_HINT = "Toque e segure para arrastar."
private const val ADD_LABEL = "Adicionar"
private const val ADD_PHOTOS_LABEL = "Adicionar fotos"
private const val NEW_ALBUM_LABEL = "Novo álbum"
private const val EDIT_ALBUM_LABEL = "Editar álbum"
private const val MOVE_ALBUM_LABEL = "Mover álbum"
private const val ORGANIZE_LABEL = "Organizar"
private const val CHANGE_COVER_LABEL = "Trocar capa"
private const val REMOVE_COVER_LABEL = "Remover capa"
private const val DELETE_ALBUM_LABEL = "Apagar álbum"
private const val SELECTED_BORDER_DP = 3

/** Everything the album screen can ask for; each is a plain event, decided in the ViewModel. */
data class AlbumActions(
    val onBack: () -> Unit,
    val onAlbumClick: (Long) -> Unit,
    val onPhotoClick: (Long) -> Unit,
    val onPhotoLongPress: (Long) -> Unit,
    val onAddPhotos: () -> Unit,
    val onNewAlbum: () -> Unit,
    val onEditAlbum: () -> Unit,
    val onMoveAlbum: () -> Unit,
    val onChangeCover: () -> Unit,
    val onRemoveCover: () -> Unit,
    val onDeleteAlbum: () -> Unit,
    val onOrganize: () -> Unit,
    val onOrganizeMove: (from: Any?, to: Any?) -> Unit,
    val onOrganizeSave: () -> Unit,
    val onOrganizeCancel: () -> Unit,
    val onSelectionMove: () -> Unit,
    val onSelectionDelete: () -> Unit,
    val onSelectionClose: () -> Unit,
    val onDismissUpload: (String) -> Unit,
)

@Composable
fun AlbumScreen(
    albumId: Long,
    viewModel: GalleryViewModel,
    nav: GalleryNav,
) {
    val state by viewModel.albumState(albumId).collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    GalleryMessageEffect(message, viewModel::consumeMessage)

    // Álbum apagado (no servidor ou aqui): sobe um nível. Se o de baixo também saiu, ele faz o mesmo.
    LaunchedEffect(state.isRemoved) {
        if (state.isRemoved) nav.back()
    }

    val pickPhotos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris ->
        viewModel.addPhotos(albumId, uris.map { it.toString() })
    }
    val pickCover = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.setCoverFromPicked(albumId, it.toString()) }
    }
    val imagesOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    BackHandler(enabled = state.isSelecting) { viewModel.clearSelection() }
    BackHandler(enabled = state.isOrganizing) { viewModel.cancelOrganize() }

    AlbumContent(
        state = state,
        previewLoader = viewModel.previewLoader,
        actions = AlbumActions(
            onBack = nav.back,
            onAlbumClick = nav.toAlbum,
            onPhotoClick = { photoId ->
                if (state.isSelecting) viewModel.togglePhotoSelection(albumId, photoId)
                else nav.toPhoto(albumId, photoId)
            },
            onPhotoLongPress = { photoId -> viewModel.onPhotoLongPress(albumId, photoId) },
            onAddPhotos = { pickPhotos.launch(imagesOnly) },
            onNewAlbum = { viewModel.openCreateAlbum(albumId) },
            onEditAlbum = { viewModel.openEditAlbum(albumId) },
            onMoveAlbum = { viewModel.openMoveAlbum(albumId) },
            onChangeCover = { pickCover.launch(imagesOnly) },
            onRemoveCover = { viewModel.askRemoveCover(albumId) },
            onDeleteAlbum = { viewModel.askDeleteAlbum(albumId) },
            onOrganize = { viewModel.startOrganize(albumId) },
            onOrganizeMove = viewModel::moveOrganizeItem,
            onOrganizeSave = viewModel::saveOrganize,
            onOrganizeCancel = viewModel::cancelOrganize,
            onSelectionMove = { viewModel.openMovePhotos(albumId) },
            onSelectionDelete = { viewModel.askDeletePhotos(albumId) },
            onSelectionClose = viewModel::clearSelection,
            onDismissUpload = viewModel::dismissUpload,
        ),
    )
    GalleryDialogHost(dialog, rememberDialogActions(viewModel))
}

@Composable
fun AlbumContent(
    state: AlbumUiState,
    previewLoader: ImageLoader,
    actions: AlbumActions,
) {
    val title = when {
        state.isOrganizing -> ORGANIZE_TITLE
        state.isSelecting -> selectedTitle(state.selection.size)
        else -> state.title.ifBlank { DEFAULT_TITLE }
    }
    BaseScreen(
        tabName = title,
        logoRes = R.drawable.ic_galery,
        showBackArrow = true,
        onBackClick = when {
            state.isOrganizing -> actions.onOrganizeCancel
            state.isSelecting -> actions.onSelectionClose
            else -> actions.onBack
        },
        extraActions = { AlbumTopActions(state, actions) },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                state.isLoading || state.isRemoved -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> Column(Modifier.fillMaxSize()) {
                    AlbumUploadsPanel(state.uploads, actions.onDismissUpload)
                    AlbumGrid(state, previewLoader, actions)
                }
            }
            if (state.permissions.canManage && !state.isOrganizing && !state.isSelecting && !state.isLoading) {
                AddButton(actions, Modifier.align(Alignment.BottomEnd).padding(16.dp))
            }
        }
    }
}

@Composable
private fun AlbumTopActions(state: AlbumUiState, actions: AlbumActions) {
    when {
        state.isOrganizing -> OrganizeActions(state.isSavingOrder, actions.onOrganizeCancel, actions.onOrganizeSave)
        state.isSelecting -> SelectionActions(
            canDelete = state.permissions.canDelete,
            onMove = actions.onSelectionMove,
            onDelete = actions.onSelectionDelete,
            onClose = actions.onSelectionClose,
        )
        else -> ManageOverflowMenu(albumMenu(state, actions))
    }
}

private fun albumMenu(state: AlbumUiState, actions: AlbumActions): List<ManageAction> {
    if (state.isLoading || state.isRemoved) return emptyList()
    val permissions = state.permissions
    return buildList {
        if (permissions.canManage) {
            add(ManageAction(EDIT_ALBUM_LABEL, actions.onEditAlbum))
            add(ManageAction(MOVE_ALBUM_LABEL, actions.onMoveAlbum))
            add(ManageAction(CHANGE_COVER_LABEL, actions.onChangeCover))
            if (state.subAlbums.size > 1 || state.photos.size > 1) add(ManageAction(ORGANIZE_LABEL, actions.onOrganize))
        }
        if (state.canRemoveCover) add(ManageAction(REMOVE_COVER_LABEL, actions.onRemoveCover))
        if (permissions.canDelete) add(ManageAction(DELETE_ALBUM_LABEL, actions.onDeleteAlbum))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AlbumGrid(state: AlbumUiState, previewLoader: ImageLoader, actions: AlbumActions) {
    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to ->
        actions.onOrganizeMove(from.key, to.key)
    }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(GRID_COLUMNS),
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding = PaddingValues(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = GalleryGridKeys.HEADER, span = { GridItemSpan(maxLineSpan) }) {
            AlbumHeader(state)
        }
        items(
            state.subAlbums,
            key = { GalleryGridKeys.album(it.id) },
            span = { GridItemSpan(ALBUM_SPAN) },
        ) { album ->
            ReorderableItem(reorderState, key = GalleryGridKeys.album(album.id), enabled = state.isOrganizing) {
                AlbumItem(
                    album = album,
                    onClick = { if (!state.isOrganizing) actions.onAlbumClick(album.id) },
                    modifier = Modifier.longPressDraggableHandle(enabled = state.isOrganizing),
                )
            }
        }
        items(
            state.photos,
            key = { GalleryGridKeys.photo(it.id) },
            span = { GridItemSpan(PHOTO_SPAN) },
        ) { photo ->
            ReorderableItem(reorderState, key = GalleryGridKeys.photo(photo.id), enabled = state.isOrganizing) {
                val selected = photo.id in state.selection
                val tileModifier = if (state.isOrganizing) {
                    Modifier.longPressDraggableHandle()
                } else {
                    Modifier.combinedClickable(
                        onClick = { actions.onPhotoClick(photo.id) },
                        onLongClick = { actions.onPhotoLongPress(photo.id) },
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .then(tileModifier),
                ) {
                    GalleryImage(
                        image = photo.image,
                        previewLoader = previewLoader,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                    if (selected) SelectedOverlay()
                }
            }
        }
    }
}

@Composable
private fun SelectedOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .border(SELECTED_BORDER_DP.dp, MaterialTheme.colorScheme.primary),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
        )
    }
}

/** "+" with a small menu: add photos here, or a sub-album. */
@Composable
private fun AddButton(actions: AlbumActions, modifier: Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        FloatingActionButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.Add, contentDescription = ADD_LABEL)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(ADD_PHOTOS_LABEL) }, onClick = {
                expanded = false
                actions.onAddPhotos()
            })
            DropdownMenuItem(text = { Text(NEW_ALBUM_LABEL) }, onClick = {
                expanded = false
                actions.onNewAlbum()
            })
        }
    }
}

@Composable
private fun AlbumHeader(state: AlbumUiState) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        if (state.isOrganizing) {
            Text(text = ORGANIZE_HINT, style = MaterialTheme.typography.bodySmall)
            return@Column
        }
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

private fun selectedTitle(count: Int) = if (count == 1) "1 selecionada" else "$count selecionadas"
