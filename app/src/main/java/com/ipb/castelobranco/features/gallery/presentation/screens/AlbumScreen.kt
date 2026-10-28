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
import com.ipb.castelobranco.features.gallery.presentation.components.PhotoTileImage
import com.ipb.castelobranco.features.gallery.presentation.components.OrganizeActions
import com.ipb.castelobranco.features.gallery.presentation.components.SelectionActions
import com.ipb.castelobranco.features.gallery.presentation.components.SelectionPeopleActions
import com.ipb.castelobranco.features.gallery.presentation.navigation.GalleryNav
import com.ipb.castelobranco.features.gallery.presentation.state.AlbumUiState
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryViewModel
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HideImage
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.ipb.castelobranco.features.gallery.presentation.components.BandChips
import com.ipb.castelobranco.features.gallery.presentation.components.BandText
import com.ipb.castelobranco.features.gallery.presentation.components.BandTitle
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryBand
import com.ipb.castelobranco.features.gallery.presentation.components.fullBleed
import com.ipb.castelobranco.core.presentation.components.PanelAction
import com.ipb.castelobranco.core.presentation.components.PanelHeader
import com.ipb.castelobranco.core.presentation.components.PanelMoreButton
import com.ipb.castelobranco.core.presentation.components.TopActionPanel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

/** 6 columns: a sub-album spans 3 (two per row), a photo spans 2 (three per row). */
private const val GRID_COLUMNS = 6
private const val ALBUM_SPAN = 3
private const val PHOTO_SPAN = 2
private const val ALBUMS_SECTION = "Álbuns"
private const val PHOTOS_SECTION = "Fotos"
private const val MEDIA_SECTION = "Fotos e vídeos"
private const val GALLERY_TITLE = "Galeria"
private const val EMPTY_MESSAGE = "Nenhuma foto neste álbum."
private const val ORGANIZE_TITLE = "Organizar"
private const val ORGANIZE_HINT = "Toque e segure para arrastar."
private const val ADD_LABEL = "Adicionar"
private const val ADD_PHOTOS_LABEL = "Adicionar fotos e vídeos"
private const val NEW_ALBUM_LABEL = "Novo álbum"
private const val EDIT_ALBUM_LABEL = "Editar álbum"
private const val MOVE_ALBUM_LABEL = "Mover álbum"
private const val ORGANIZE_LABEL = "Organizar"
private const val CHANGE_COVER_LABEL = "Trocar capa"
private const val REMOVE_COVER_LABEL = "Remover capa"
private const val DELETE_ALBUM_LABEL = "Apagar álbum"
private const val SELECTED_BORDER_DP = 3
private const val EDIT_ALBUM_HINT = "Nome, data e descrição"
private const val MOVE_ALBUM_HINT = "Para dentro de outro álbum"
private const val ORGANIZE_HINT_SHORT = "Arrastar a ordem"
private const val DELETE_ALBUM_HINT = "Vai para a lixeira"
private const val DETAIL_SEPARATOR = " · "
private val GridEdge = 8.dp
private val PhotoCorner = 6.dp
private val SectionBarWidth = 4.dp
private val SectionBarHeight = 16.dp

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
    val onSelectionAddPeople: () -> Unit,
    val onSelectionRemovePeople: () -> Unit,
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
    val imagesAndVideos = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)

    BackHandler(enabled = state.isSelecting) { viewModel.clearSelection() }
    BackHandler(enabled = state.isOrganizing) { viewModel.cancelOrganize() }

    GalleryMessageHost(message, isLeaving = state.isRemoved, viewModel, nav) {
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
                onAddPhotos = { pickPhotos.launch(imagesAndVideos) },
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
                onSelectionAddPeople = { viewModel.openBulkTag(albumId, remove = false) },
                onSelectionRemovePeople = { viewModel.openBulkTag(albumId, remove = true) },
                onDismissUpload = viewModel::dismissUpload,
            ),
        )
    }
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
        else -> GALLERY_TITLE
    }
    var isPanelOpen by rememberSaveable { mutableStateOf(false) }
    val panelActions = albumPanelActions(state, actions)
    BaseScreen(
        tabName = title,
        logoRes = R.drawable.ic_galery,
        showBackArrow = true,
        onBackClick = when {
            state.isOrganizing -> actions.onOrganizeCancel
            state.isSelecting -> actions.onSelectionClose
            else -> actions.onBack
        },
        extraActions = { AlbumTopActions(state, actions, hasPanel = panelActions.isNotEmpty()) { isPanelOpen = true } },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            when {
                state.isLoading || state.isRemoved -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                else -> AlbumGrid(state, previewLoader, actions)
            }
            if (state.permissions.canManage && !state.isOrganizing && !state.isSelecting && !state.isLoading) {
                AddButton(actions, Modifier.align(Alignment.BottomEnd).padding(16.dp))
            }
            TopActionPanel(
                visible = isPanelOpen && !state.isOrganizing && !state.isSelecting,
                actions = panelActions,
                onDismiss = { isPanelOpen = false },
                header = PanelHeader(
                    name = state.title,
                    detail = listOfNotNull(state.eventDate, state.countLabel).joinToString(DETAIL_SEPARATOR)
                        .ifEmpty { null },
                    cover = state.cover,
                    showCover = true,
                ),
            )
        }
    }
}

@Composable
private fun AlbumTopActions(state: AlbumUiState, actions: AlbumActions, hasPanel: Boolean, onOpenPanel: () -> Unit) {
    when {
        state.isOrganizing -> OrganizeActions(state.isSavingOrder, actions.onOrganizeCancel, actions.onOrganizeSave)
        state.isSelecting -> SelectionActions(
            canDelete = state.permissions.canDelete,
            onMove = actions.onSelectionMove,
            onDelete = actions.onSelectionDelete,
            onClose = actions.onSelectionClose,
            people = if (state.permissions.canManage) {
                SelectionPeopleActions(
                    canRemove = state.canRemovePeople,
                    onAdd = actions.onSelectionAddPeople,
                    onRemove = actions.onSelectionRemovePeople,
                )
            } else {
                null
            },
        )
        else -> PanelMoreButton(hasActions = hasPanel, onClick = onOpenPanel)
    }
}

/** The actions panel of an album (`specs/gallery/spec.md` §1.5), by level (§8.1). */
private fun albumPanelActions(state: AlbumUiState, actions: AlbumActions): List<PanelAction> {
    if (state.isLoading || state.isRemoved) return emptyList()
    val permissions = state.permissions
    return buildList {
        if (permissions.canManage) {
            add(PanelAction(Icons.Filled.Edit, EDIT_ALBUM_LABEL, actions.onEditAlbum, EDIT_ALBUM_HINT))
            add(PanelAction(Icons.AutoMirrored.Filled.DriveFileMove, MOVE_ALBUM_LABEL, actions.onMoveAlbum, MOVE_ALBUM_HINT))
            add(PanelAction(Icons.Filled.Image, CHANGE_COVER_LABEL, actions.onChangeCover))
            if (state.subAlbums.size > 1 || state.photos.size > 1) {
                add(PanelAction(Icons.AutoMirrored.Filled.Sort, ORGANIZE_LABEL, actions.onOrganize, ORGANIZE_HINT_SHORT))
            }
        }
        if (state.canRemoveCover) add(PanelAction(Icons.Filled.HideImage, REMOVE_COVER_LABEL, actions.onRemoveCover))
        if (permissions.canDelete) {
            add(
                PanelAction(
                    Icons.Filled.Delete, DELETE_ALBUM_LABEL, actions.onDeleteAlbum, DELETE_ALBUM_HINT, isDanger = true,
                ),
            )
        }
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
        contentPadding = PaddingValues(start = GridEdge, end = GridEdge, bottom = GridEdge),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = GalleryGridKeys.HEADER, span = { GridItemSpan(maxLineSpan) }) {
            AlbumHeader(state, actions.onDismissUpload)
        }
        // A full-line title per group keeps the first photo off a sub-album's row.
        if (state.subAlbums.isNotEmpty()) {
            item(key = GalleryGridKeys.ALBUMS_SECTION, span = { GridItemSpan(maxLineSpan) }) {
                SectionTitle(ALBUMS_SECTION)
            }
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
        if (state.photos.isNotEmpty()) {
            item(key = GalleryGridKeys.PHOTOS_SECTION, span = { GridItemSpan(maxLineSpan) }) {
                val label = if (state.photos.any { it.isVideo }) MEDIA_SECTION else PHOTOS_SECTION
                SectionTitle(label)
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
                        .clip(RoundedCornerShape(PhotoCorner))
                        .then(tileModifier),
                ) {
                    PhotoTileImage(photo, previewLoader, Modifier.fillMaxSize())
                    if (selected) SelectedOverlay()
                }
            }
        }
    }
}

/** A group's title, with a short `primary` bar before it. */
@Composable
private fun SectionTitle(label: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            Modifier
                .size(width = SectionBarWidth, height = SectionBarHeight)
                .clip(RoundedCornerShape(percent = 50))
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(text = label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
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

/** The album's band — date and count, name, description — then the uploads and the empty message. */
@Composable
private fun AlbumHeader(state: AlbumUiState, onDismissUpload: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        GalleryBand(modifier = Modifier.fullBleed(GridEdge)) {
            BandChips(listOfNotNull(state.eventDate, state.countLabel))
            BandTitle(state.title)
            val text = if (state.isOrganizing) ORGANIZE_HINT else state.description
            text?.let { BandText(it, Modifier.padding(top = 4.dp)) }
        }
        AlbumUploadsPanel(state.uploads, onDismissUpload)
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
