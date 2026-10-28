package com.ipb.castelobranco.features.gallery.presentation.screens

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.view.WindowCompat
import com.ipb.castelobranco.core.presentation.base.findActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryDialogHost
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryImage
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryVideoPage
import com.ipb.castelobranco.features.gallery.presentation.components.rememberGalleryPlayer
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
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.VideoTexts
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import java.io.File
import java.io.OutputStream

private const val SAVED_LABEL = "Baixada"
private const val BACK_LABEL = "Voltar"
private const val DETAIL_SEPARATOR = " · "
private const val SAVED_FEEDBACK_MS = 2_000L
private const val TOP_SHADE_ALPHA = 0.6f
private const val BOTTOM_SHADE_ALPHA = 0.7f
private const val MUTED_ALPHA = 0.75f
private const val TAG_ALPHA = 0.45f
private const val DISABLED_ALPHA = 0.38f
/** "Baixada": the light green of the app's detail text on green. */
private val SavedGreen = Color(0xFF8ED1BA)
private const val SAVE_LABEL = "Baixar"
private const val SHARE_LABEL = "Compartilhar"
private const val SHARE_CHOOSER_TITLE = "Compartilhar"
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
    /** Identifies this opening of the viewer (its back stack entry). */
    viewerId: String,
    source: ViewerSource,
    photoId: Long,
    viewModel: GalleryViewModel,
    nav: GalleryNav,
) {
    val state by viewModel.viewerState(viewerId, source, photoId).collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    val muted by viewModel.videoMuted.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Black screen in both themes: light system bar icons while the viewer is open.
    val view = LocalView.current
    DisposableEffect(view) {
        val window = view.context.findActivity()?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val lightStatus = controller?.isAppearanceLightStatusBars
        val lightNavigation = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = false
        controller?.isAppearanceLightNavigationBars = false
        onDispose {
            lightStatus?.let { controller.isAppearanceLightStatusBars = it }
            lightNavigation?.let { controller.isAppearanceLightNavigationBars = it }
        }
    }

    // Nenhuma foto sobrou no álbum: volta para ele.
    LaunchedEffect(state.isClosed) {
        if (state.isClosed) nav.back()
    }

    GalleryMessageHost(message, isLeaving = state.isClosed, viewModel, nav) {
        PhotoContent(
            state = state,
            previewLoader = viewModel.previewLoader,
            onBack = nav.back,
            onPageChanged = { currentId -> viewModel.onPageChanged(viewerId, currentId) },
            onSave = { photo -> photo.file?.let { saveToDevice(context, it, photo.fileName, photo) } },
            onShare = { photo -> photo.file?.let { shareFile(context, it, photo.mimeType) } },
            // Cada ação usa o álbum da própria foto: num resultado de filtro, as fotos vêm de vários.
            manage = PhotoManageActions(
                onEdit = { photo -> viewModel.openEditPhoto(photo.id) },
                onMove = { photo -> viewModel.openMovePhotos(photo.albumId, photo.id) },
                onUseAsCover = { photo -> viewModel.useAsCover(photo.albumId, photo.id) },
                onDelete = { photo -> viewModel.askDeletePhotos(photo.albumId, photo.id) },
                onTagPeople = { photo -> viewModel.openTagPhoto(photo.id) },
            ),
            isPickerOpen = dialog is GalleryDialogState.PeoplePicker,
            video = VideoControls(muted = muted, onMutedChange = viewModel::setVideoMuted),
        )
    }
    GalleryDialogHost(dialog, rememberDialogActions(viewModel))
}

/** The sound of the videos: muted until turned on, then kept while the gallery stays open. */
data class VideoControls(val muted: Boolean = true, val onMutedChange: (Boolean) -> Unit = {})

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
    video: VideoControls = VideoControls(),
) {
    val view = LocalView.current
    val player = rememberGalleryPlayer()
    val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    var showDetails by rememberSaveable { mutableStateOf(false) }
    var showChrome by rememberSaveable { mutableStateOf(true) }
    var savedPhotoId by remember { mutableStateOf<Long?>(null) }
    val photos = state.photos
    // Recriado quando as fotos chegam, para abrir já na foto tocada.
    val pagerState = key(state.isLoading) { rememberPagerState(initialPage = state.currentIndex) { photos.size } }
    var isZoomed by remember { mutableStateOf(false) }

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
    // "Baixada" stays a moment on the button, then it is "Baixar" again.
    LaunchedEffect(savedPhotoId) {
        if (savedPhotoId != null) {
            delay(SAVED_FEEDBACK_MS)
            savedPhotoId = null
        }
    }

    val current = photos.getOrNull(pagerState.currentPage)

    // Landscape on a video: the video alone, full screen. Back in portrait, the pager is where it was.
    val landscapeVideo = current?.videoFile?.takeIf { isLandscape && current.isVideo }
    if (landscapeVideo != null) {
        GalleryVideoPage(
            file = landscapeVideo,
            player = player,
            isActive = true,
            muted = video.muted,
            onMutedChange = video.onMutedChange,
            failedText = VideoTexts.PLAYBACK_FAILED,
            modifier = Modifier.fillMaxSize(),
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        if (state.isLoading || photos.isEmpty()) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
            ViewerTopBar(title = state.title, detail = null, onBack = onBack, menu = {})
            return@Box
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !isZoomed,
            key = { photos[it].id },
        ) { index ->
            val page = photos[index]
            val videoFile = page.videoFile
            if (page.isVideo && videoFile != null) {
                // Plays once the pager settles here, muted unless the sound was turned on.
                GalleryVideoPage(
                    file = videoFile,
                    player = player,
                    isActive = pagerState.settledPage == index,
                    muted = video.muted,
                    onMutedChange = video.onMutedChange,
                    failedText = VideoTexts.PLAYBACK_FAILED,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                ZoomablePage(
                    resetKey = pagerState.currentPage,
                    onZoomChanged = { isZoomed = it },
                    onTap = { showChrome = !showChrome },
                ) {
                    GalleryImage(
                        image = page.image,
                        previewLoader = previewLoader,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            ViewerTopBar(
                title = state.title,
                detail = current?.let { photo ->
                    listOfNotNull("${pagerState.currentPage + 1} de ${photos.size}", photo.dateTaken)
                        .joinToString(DETAIL_SEPARATOR)
                },
                onBack = onBack,
                menu = { current?.let { ManageOverflowMenu(photoMenu(it, state.permissions, manage)) } },
            )
        }

        AnimatedVisibility(
            visible = showChrome,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                current?.people?.takeIf { it.isNotEmpty() }?.let { PeopleTags(it) }
                ViewerBottomBar(
                    canSaveOrShare = current?.canSaveOrShare == true,
                    isSaved = current != null && savedPhotoId == current.id,
                    onSave = {
                        val photo = current ?: return@ViewerBottomBar
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        onSave(photo)
                        savedPhotoId = photo.id
                    },
                    onShare = {
                        val photo = current ?: return@ViewerBottomBar
                        view.playSoundEffect(SoundEffectConstants.CLICK)
                        onShare(photo)
                    },
                    onDetails = { showDetails = true },
                )
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
    }
}

/** Back, the album and "3 de 90 · date", and the photo's menu, over a dark gradient. */
@Composable
private fun ViewerTopBar(title: String, detail: String?, onBack: () -> Unit, menu: @Composable () -> Unit) {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = TOP_SHADE_ALPHA), Color.Transparent)))
                .statusBarsPadding()
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = BACK_LABEL)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                detail?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = MUTED_ALPHA))
                }
            }
            menu()
        }
    }
}

/** The first names of who is tagged, over the photo, right above the bottom bar. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PeopleTags(people: List<String>) {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        people.forEach { name ->
            Text(
                text = name.substringBefore(' '),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Color.Black.copy(alpha = TAG_ALPHA))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

/** "Baixar" ("Baixada" for a moment after saving), "Compartilhar" and "Detalhes", white, over a dark gradient. */
@Composable
private fun ViewerBottomBar(
    canSaveOrShare: Boolean,
    isSaved: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = BOTTOM_SHADE_ALPHA))))
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        ViewerAction(
            icon = if (isSaved) Icons.Filled.Check else Icons.Filled.Download,
            label = if (isSaved) SAVED_LABEL else SAVE_LABEL,
            enabled = canSaveOrShare && !isSaved,
            color = if (isSaved) SavedGreen else Color.White,
            onClick = onSave,
        )
        ViewerAction(Icons.Filled.Share, SHARE_LABEL, enabled = canSaveOrShare, onClick = onShare)
        ViewerAction(Icons.Outlined.Info, DETAILS_LABEL, enabled = true, onClick = onDetails)
    }
}

@Composable
private fun ViewerAction(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    color: Color = Color.White,
) {
    // "Baixada" keeps its color while disabled: it is a confirmation, not an unavailable action.
    val tint = if (enabled || color != Color.White) color else Color.White.copy(alpha = DISABLED_ALPHA)
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(icon, contentDescription = null, tint = tint)
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
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

/** Pinch to zoom, pan while zoomed, double tap to reset, a single tap is [onTap]. Resets when [resetKey] changes. */
@Composable
private fun ZoomablePage(
    resetKey: Any,
    onZoomChanged: (Boolean) -> Unit,
    onTap: () -> Unit = {},
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
                    onTap = { onTap() },
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

private fun shareFile(context: Context, file: File, mimeType: String) {
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",
        file
    )
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, SHARE_CHOOSER_TITLE))
}

/** A photo goes to Pictures, a video to Movies, each under the app's folder and typed by its MIME. */
private fun saveToDevice(context: Context, sourceFile: File, displayName: String, item: ViewerPhoto) {
    if (item.isVideo) saveVideoToGallery(context, sourceFile, displayName, item.mimeType)
    else saveImageToGallery(context, sourceFile, displayName)
}

private fun saveVideoToGallery(context: Context, sourceFile: File, displayName: String, mimeType: String) {
    val contentValues = ContentValues().apply {
        put(MediaStore.Video.Media.DISPLAY_NAME, displayName)
        put(MediaStore.Video.Media.MIME_TYPE, mimeType)
        put(MediaStore.Video.Media.RELATIVE_PATH, "${android.os.Environment.DIRECTORY_MOVIES}/$SAVE_DIR")
        put(MediaStore.Video.Media.IS_PENDING, 1)
    }
    val resolver = context.contentResolver
    val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues) ?: return
    resolver.openOutputStream(uri)?.use { output -> sourceFile.inputStream().use { it.copyTo(output) } }
    contentValues.clear()
    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
    resolver.update(uri, contentValues, null, null)
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
