package com.ipb.castelobranco.features.gallery.presentation.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.view.SoundEffectConstants
import android.webkit.MimeTypeMap
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryDialogHost
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryImage
import com.ipb.castelobranco.features.gallery.presentation.components.ManageAction
import com.ipb.castelobranco.features.gallery.presentation.components.ManageOverflowMenu
import com.ipb.castelobranco.features.gallery.presentation.components.PhotoDetailsSheet
import com.ipb.castelobranco.features.gallery.presentation.navigation.GalleryNav
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryDialogState
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryPermissions
import com.ipb.castelobranco.features.gallery.presentation.state.PhotoViewerUiState
import com.ipb.castelobranco.features.gallery.presentation.state.ViewerPhoto
import com.ipb.castelobranco.features.gallery.presentation.state.ViewerSource
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import java.io.File
import java.io.OutputStream

private const val LOADING_TITLE = "Carregando..."
private const val SAVED_MESSAGE = "Baixado com sucesso"
private const val SAVE_LABEL = "Baixar"
private const val SHARE_LABEL = "Compartilhar"
private const val SHARE_CHOOSER_TITLE = "Compartilhar imagem"
private const val SAVE_FEEDBACK_MS = 1_200L
private const val MAX_ZOOM = 5f
private const val SAVE_DIR = "ipb_castelobranco"
private const val DEFAULT_MIME = "image/jpeg"
private const val EDIT_LABEL = "Editar foto"
private const val MOVE_LABEL = "Mover"
private const val USE_AS_COVER_LABEL = "Usar como capa"
private const val DELETE_LABEL = "Apagar"
private const val TAG_PEOPLE_LABEL = "Marcar pessoas"
private const val DETAILS_LABEL = "Detalhes"

@Composable
fun PhotoScreen(
    source: ViewerSource,
    photoId: Long,
    viewModel: GalleryViewModel,
    nav: GalleryNav,
) {
    val state by viewModel.viewerState(source, photoId).collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Nenhuma foto sobrou no álbum: volta para ele.
    LaunchedEffect(state.isClosed) {
        if (state.isClosed) nav.back()
    }

    GalleryMessageHost(message, isLeaving = state.isClosed, viewModel, nav) {
        PhotoContent(
            state = state,
            previewLoader = viewModel.previewLoader,
            onBack = nav.back,
            onPageChanged = { currentId -> viewModel.onPageChanged(source, photoId, currentId) },
            onSave = { photo -> photo.original?.let { saveImageToGallery(context, it, photo.fileName) } },
            onShare = { photo -> photo.original?.let { sharePhoto(context, it) } },
            // Cada ação usa o álbum da própria foto: num resultado de filtro, as fotos vêm de vários.
            manage = PhotoManageActions(
                onEdit = { photo -> viewModel.openEditPhoto(photo.id) },
                onMove = { photo -> viewModel.openMovePhotos(photo.albumId, photo.id) },
                onUseAsCover = { photo -> viewModel.useAsCover(photo.albumId, photo.id) },
                onDelete = { photo -> viewModel.askDeletePhotos(photo.albumId, photo.id) },
                onTagPeople = { photo -> viewModel.openTagPhoto(photo.id) },
            ),
            isPickerOpen = dialog is GalleryDialogState.PeoplePicker,
        )
    }
    GalleryDialogHost(dialog, rememberDialogActions(viewModel))
}

/** Management of the photo on screen. */
data class PhotoManageActions(
    val onEdit: (ViewerPhoto) -> Unit,
    val onMove: (ViewerPhoto) -> Unit,
    val onUseAsCover: (ViewerPhoto) -> Unit,
    val onDelete: (ViewerPhoto) -> Unit,
    val onTagPeople: (ViewerPhoto) -> Unit,
)

@Composable
fun PhotoContent(
    state: PhotoViewerUiState,
    previewLoader: ImageLoader,
    onBack: () -> Unit,
    onPageChanged: (photoId: Long) -> Unit,
    onSave: (ViewerPhoto) -> Unit,
    onShare: (ViewerPhoto) -> Unit,
    manage: PhotoManageActions,
    /** The people picker is open: the details sheet steps aside for it. */
    isPickerOpen: Boolean = false,
) {
    val view = LocalView.current
    var showDetails by rememberSaveable { mutableStateOf(false) }
    val photos = state.photos
    // Recriado quando as fotos chegam, para abrir já na foto tocada.
    val pagerState = key(state.isLoading) { rememberPagerState(initialPage = state.currentIndex) { photos.size } }
    var isZoomed by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // A foto na tela saiu do álbum: o ViewModel aponta a próxima.
    LaunchedEffect(state.currentIndex, photos.size) {
        if (photos.isNotEmpty() && pagerState.currentPage != state.currentIndex) {
            pagerState.scrollToPage(state.currentIndex)
        }
    }
    LaunchedEffect(pagerState, photos) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { page -> photos.getOrNull(page)?.let { onPageChanged(it.id) } }
    }

    val current = photos.getOrNull(pagerState.currentPage)

    BaseScreen(
        tabName = current?.title ?: LOADING_TITLE,
        logoRes = R.drawable.ic_galery,
        showBackArrow = true,
        onBackClick = onBack,
        extraActions = {
            current?.let { photo ->
                IconButton(onClick = { showDetails = true }) {
                    Icon(Icons.Outlined.Info, contentDescription = DETAILS_LABEL)
                }
                ManageOverflowMenu(photoMenu(photo, state.permissions, manage))
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            if (state.isLoading || photos.isEmpty()) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
                return@Box
            }
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    userScrollEnabled = !isZoomed,
                    key = { photos[it].id },
                ) { index ->
                    ZoomablePage(
                        resetKey = pagerState.currentPage,
                        onZoomChanged = { isZoomed = it },
                    ) {
                        GalleryImage(
                            image = photos[index].image,
                            previewLoader = previewLoader,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    var isSaveCoolingDown by remember { mutableStateOf(false) }
                    Button(
                        // Sem original no aparelho não há o que salvar ou compartilhar.
                        enabled = !isSaveCoolingDown && current?.canSaveOrShare == true,
                        onClick = {
                            val photo = current ?: return@Button
                            isSaveCoolingDown = true
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onSave(photo)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    message = SAVED_MESSAGE,
                                    duration = SnackbarDuration.Indefinite
                                )
                            }
                            scope.launch {
                                delay(SAVE_FEEDBACK_MS)
                                snackbarHostState.currentSnackbarData?.dismiss()
                                isSaveCoolingDown = false
                            }
                        }
                    ) {
                        Text(SAVE_LABEL)
                    }
                    Button(
                        enabled = current?.canSaveOrShare == true,
                        onClick = {
                            val photo = current ?: return@Button
                            view.playSoundEffect(SoundEffectConstants.CLICK)
                            onShare(photo)
                        },
                    ) {
                        Text(SHARE_LABEL)
                    }
                }
            }
            if (showDetails && !isPickerOpen && current != null) {
                PhotoDetailsSheet(
                    photo = current,
                    canTag = state.permissions.canManage,
                    onTag = { manage.onTagPeople(current) },
                    onDismiss = { showDetails = false },
                )
            }
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.Center),
                snackbar = { data: SnackbarData ->
                    Snackbar(
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.padding(horizontal = 24.dp)
                    ) {
                        Text(
                            text = data.visuals.message,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            )
        }
    }
}

private fun photoMenu(photo: ViewerPhoto, permissions: GalleryPermissions, manage: PhotoManageActions) =
    buildList {
        if (permissions.canManage) {
            add(ManageAction(EDIT_LABEL) { manage.onEdit(photo) })
            add(ManageAction(TAG_PEOPLE_LABEL) { manage.onTagPeople(photo) })
            add(ManageAction(MOVE_LABEL) { manage.onMove(photo) })
            add(ManageAction(USE_AS_COVER_LABEL) { manage.onUseAsCover(photo) })
        }
        if (permissions.canDelete) add(ManageAction(DELETE_LABEL) { manage.onDelete(photo) })
    }

/** Pinch to zoom, pan while zoomed, double tap to reset. Resets when [resetKey] changes. */
@Composable
private fun ZoomablePage(
    resetKey: Any,
    onZoomChanged: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    LaunchedEffect(resetKey) {
        scale = 1f
        offset = Offset.Zero
        onZoomChanged(false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        scale = 1f
                        offset = Offset.Zero
                        onZoomChanged(false)
                    }
                )
            }
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()

                        // Só atualiza e consome quando há zoom, ou arrasto com zoom
                        if (zoom != 1f || (scale > 1f && pan != Offset.Zero)) {
                            scale = (scale * zoom).coerceIn(1f, MAX_ZOOM)
                            onZoomChanged(scale > 1f)
                            if (scale > 1f) {
                                offset += pan
                                // Consome os eventos para o Pager não rodar
                                event.changes.forEach { it.consume() }
                            }
                        }
                        if (scale <= 1f) {
                            onZoomChanged(false)
                            offset = Offset.Zero
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            }
    ) {
        content()
    }
}

private fun sharePhoto(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, SHARE_CHOOSER_TITLE))
}

/** Copies the original to the device's Pictures, named after the photo and typed by its extension. */
private fun saveImageToGallery(context: Context, sourceFile: File, displayName: String) {
    val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(sourceFile.extension.lowercase())
        ?: DEFAULT_MIME
    val contentValues = ContentValues().apply {
        put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        put(
            MediaStore.Images.Media.RELATIVE_PATH,
            "${android.os.Environment.DIRECTORY_PICTURES}/$SAVE_DIR"
        )
        put(MediaStore.Images.Media.IS_PENDING, 1)
    }

    val resolver = context.contentResolver
    val uri = resolver.insert(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ) ?: return

    resolver.openOutputStream(uri)?.use { output: OutputStream ->
        sourceFile.inputStream().use { input ->
            input.copyTo(output)
        }
    }

    contentValues.clear()
    contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
    resolver.update(uri, contentValues, null, null)
}
