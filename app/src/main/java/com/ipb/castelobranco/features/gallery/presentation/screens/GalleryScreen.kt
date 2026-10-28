package com.ipb.castelobranco.features.gallery.presentation.screens

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import com.ipb.castelobranco.features.gallery.presentation.state.MessageAction
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.PermissionErrorPlaceholder
import com.ipb.castelobranco.core.presentation.components.access.MembersOnlyPlaceholder
import com.ipb.castelobranco.features.gallery.presentation.components.AlbumItem
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryDialogActions
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryDialogHost
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryGridKeys
import com.ipb.castelobranco.features.gallery.presentation.components.MediaDialogActions
import com.ipb.castelobranco.features.gallery.presentation.components.OrganizeActions
import com.ipb.castelobranco.features.gallery.presentation.components.PeoplePickerActions
import com.ipb.castelobranco.features.gallery.presentation.navigation.GalleryNav
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryMessage
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryRootUiState
import com.ipb.castelobranco.features.gallery.presentation.state.VideoCard
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.VideoTexts
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryViewModel
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.ipb.castelobranco.features.gallery.presentation.components.BandDownload
import com.ipb.castelobranco.features.gallery.presentation.components.BandLabel
import com.ipb.castelobranco.features.gallery.presentation.components.BandNotice
import com.ipb.castelobranco.features.gallery.presentation.components.BandOverlap
import com.ipb.castelobranco.features.gallery.presentation.components.BandText
import com.ipb.castelobranco.features.gallery.presentation.components.BandTitle
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryBand
import com.ipb.castelobranco.features.gallery.presentation.components.fullBleed
import com.ipb.castelobranco.features.gallery.presentation.components.PanelAction
import com.ipb.castelobranco.features.gallery.presentation.components.PanelMoreButton
import com.ipb.castelobranco.features.gallery.presentation.components.TopActionPanel
import com.ipb.castelobranco.features.gallery.presentation.components.overlappedBelow
import com.ipb.castelobranco.features.gallery.presentation.state.albumsLabel
import com.ipb.castelobranco.features.gallery.presentation.state.galleryCountLabel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val TITLE = "Galeria"
private const val LOGIN_MESSAGE = "Faça login para acessar a galeria."
private const val PENDING_WIFI_MESSAGE = "Aguardando WiFi para baixar a galeria…"
private const val WAITING_WIFI_MESSAGE = "Aguardando WiFi…"
private const val MOBILE_DATA_LABEL = "Usar dados móveis"
private const val STARTING_DOWNLOAD_MESSAGE = "Iniciando download…"
private const val BAND_LABEL = "Galeria da igreja"
private const val PEOPLE_HINT = "Quem aparece nas fotos"
private const val TRASH_HINT = "Álbuns e fotos apagados"
private const val ORGANIZE_HINT = "Arrastar a ordem dos álbuns"
private const val VIDEO_CARD_KEY = "video_card"
private val GridSpacing = 16.dp
private val BandBottomExtra = 14.dp
private const val RESUMING_MESSAGE = "Download pausado. Continua automaticamente em instantes."
private const val NO_ACCESS_MESSAGE = "Disponível apenas para membros."
private const val NO_ALBUMS_MESSAGE = "Nenhum álbum disponível."
private const val RETRY_LABEL = "Tentar novamente"
private const val DELETED_MESSAGE = "A galeria foi apagada deste aparelho."
private const val DOWNLOAD_GALLERY_LABEL = "Baixar galeria"
private const val ORGANIZE_TITLE = "Organizar"
private const val ORGANIZE_LABEL = "Organizar"
private const val NEW_ALBUM_LABEL = "Novo álbum"
private const val TRASH_LABEL = "Lixeira"
private const val PEOPLE_LABEL = "Pessoas"

@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    isLoggedIn: Boolean,
    nav: GalleryNav,
    onNavigateToAuth: () -> Unit,
) {
    val state by viewModel.rootState.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val dialog by viewModel.dialog.collectAsStateWithLifecycle()
    BackHandler(enabled = state.isOrganizing) { viewModel.cancelOrganize() }

    GalleryMessageHost(message, isLeaving = false, viewModel, nav) {
        GalleryContent(
            state = state,
            isLoggedIn = isLoggedIn,
            onBack = nav.back,
            onAlbumClick = nav.toAlbum,
            onTrash = nav.toTrash,
            onPeople = nav.toPeople,
            onNavigateToAuth = onNavigateToAuth,
            onRetrySync = viewModel::retrySync,
            onRetryDownload = viewModel::retryDownload,
            onDownloadWithMobileData = viewModel::downloadWithMobileData,
            onDownloadGallery = viewModel::downloadGallery,
            videoCard = VideoCardActions(
                onDismiss = viewModel::dismissVideoCard,
                onAccept = viewModel::acceptVideoCard,
            ),
            manage = RootManageActions(
                onNewAlbum = { viewModel.openCreateAlbum(null) },
                onOrganize = { viewModel.startOrganize(null) },
                onOrganizeMove = viewModel::moveOrganizeItem,
                onOrganizeSave = viewModel::saveOrganize,
                onOrganizeCancel = viewModel::cancelOrganize,
            ),
        )
    }
    GalleryDialogHost(dialog, rememberDialogActions(viewModel))
}

/** "Não, obrigado" / "Baixar" on the videos card. */
data class VideoCardActions(val onDismiss: () -> Unit = {}, val onAccept: () -> Unit = {})

/** The root's management events: a new root album, and "Organizar" for the root albums. */
data class RootManageActions(
    val onNewAlbum: () -> Unit,
    val onOrganize: () -> Unit,
    val onOrganizeMove: (from: Any?, to: Any?) -> Unit,
    val onOrganizeSave: () -> Unit,
    val onOrganizeCancel: () -> Unit,
)

/** The dialog events of every gallery screen, bound to the graph's ViewModel. */
@Composable
internal fun rememberDialogActions(viewModel: GalleryViewModel): GalleryDialogActions = remember(viewModel) {
    GalleryDialogActions(
        onNameChange = viewModel::onFormNameChange,
        onDescriptionChange = viewModel::onFormDescriptionChange,
        onDateChange = viewModel::onFormDateChange,
        onSave = viewModel::saveForm,
        onChooseTarget = viewModel::chooseMoveTarget,
        onConfirm = viewModel::confirm,
        onDismiss = viewModel::dismissDialog,
        picker = PeoplePickerActions(
            onQueryChange = viewModel::onPickerQueryChange,
            onToggle = viewModel::togglePickerPerson,
            onRetry = viewModel::retryPickerLoad,
            onConfirm = viewModel::savePicker,
            onDismiss = viewModel::dismissDialog,
        ),
        media = MediaDialogActions(
            onToggle = viewModel::toggleDownloadOption,
            onAnswerMobileData = viewModel::answerDownloadMobileData,
            onConfirm = viewModel::confirmDownload,
            onUploadNow = viewModel::confirmUploadOnMobileData,
        ),
    )
}

/**
 * Wraps a gallery screen with the snackbar that shows the graph's message once — from whichever
 * screen is on top. A screen that is leaving ([isLeaving]) leaves it to the one below. The message is
 * consumed before the snackbar shows (on a scope that outlives the effect), and its button runs here:
 * "Desfazer" restores, "Abrir álbum" opens the album.
 */
@Composable
internal fun GalleryMessageHost(
    message: GalleryMessage?,
    isLeaving: Boolean,
    viewModel: GalleryViewModel,
    nav: GalleryNav,
    content: @Composable () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(message, isLeaving) {
        if (message == null || isLeaving) return@LaunchedEffect
        viewModel.consumeMessage()
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val action = message.action
            val result = snackbarHostState.showSnackbar(
                message = message.text,
                actionLabel = action?.label,
                duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result != SnackbarResult.ActionPerformed) return@launch
            when (action) {
                is MessageAction.Undo -> viewModel.undo(action)
                is MessageAction.OpenAlbum -> nav.toAlbum(action.albumId)
                null -> Unit
            }
        }
    }
    Box(Modifier.fillMaxSize()) {
        content()
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }
}

@Composable
fun GalleryContent(
    state: GalleryRootUiState,
    isLoggedIn: Boolean,
    onBack: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onTrash: () -> Unit,
    onPeople: () -> Unit,
    onNavigateToAuth: () -> Unit,
    onRetrySync: () -> Unit,
    onRetryDownload: () -> Unit,
    onDownloadWithMobileData: () -> Unit,
    onDownloadGallery: () -> Unit,
    manage: RootManageActions,
    videoCard: VideoCardActions = VideoCardActions(),
) {
    val canManage = isLoggedIn && state.permissions.canManage
    var isPanelOpen by rememberSaveable { mutableStateOf(false) }
    val panelActions = buildList {
        if (isLoggedIn && state.showPeople) add(PanelAction(Icons.Filled.Groups, PEOPLE_LABEL, onPeople, PEOPLE_HINT))
        if (isLoggedIn && state.showTrash) add(PanelAction(Icons.Filled.Delete, TRASH_LABEL, onTrash, TRASH_HINT))
        if (canManage && state.albums.size > 1) {
            add(PanelAction(Icons.AutoMirrored.Filled.Sort, ORGANIZE_LABEL, manage.onOrganize, ORGANIZE_HINT))
        }
    }

    BaseScreen(
        tabName = if (state.isOrganizing) ORGANIZE_TITLE else TITLE,
        logoRes = R.drawable.ic_galery,
        showBackArrow = true,
        onBackClick = if (state.isOrganizing) manage.onOrganizeCancel else onBack,
        extraActions = {
            if (state.isOrganizing) {
                OrganizeActions(state.isSavingOrder, manage.onOrganizeCancel, manage.onOrganizeSave)
            } else {
                PanelMoreButton(hasActions = panelActions.isNotEmpty(), onClick = { isPanelOpen = true })
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val showGrid = isLoggedIn && !state.isPaused && state.albums.isNotEmpty()
            if (showGrid) {
                RootAlbumGrid(state, onAlbumClick, manage.onOrganizeMove, videoCard) {
                    GalleryBand(bottomPadding = BandOverlap + BandBottomExtra) {
                        BandLabel(BAND_LABEL)
                        BandTitle(albumsLabel(state.albums.size))
                        RootBandStatus(state, onDownloadWithMobileData, showCount = true)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    GalleryBand {
                        BandLabel(BAND_LABEL)
                        BandTitle(TITLE)
                        if (isLoggedIn && !state.isPaused) RootBandStatus(state, onDownloadWithMobileData, showCount = false)
                    }
                    RootPlaceholder(state, isLoggedIn, onNavigateToAuth, onRetrySync, onRetryDownload, onDownloadGallery)
                }
            }
            if (canManage && !state.isOrganizing && state.hasIndex) {
                FloatingActionButton(
                    onClick = manage.onNewAlbum,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = NEW_ALBUM_LABEL)
                }
            }
            TopActionPanel(
                visible = isPanelOpen && !state.isOrganizing,
                actions = panelActions,
                onDismiss = { isPanelOpen = false },
            )
        }
    }
}

/**
 * What the band says under the album count: a notice when there is one (`specs/gallery/spec.md` §6, item 3), else
 * the photo and video count.
 */
@Composable
private fun RootBandStatus(state: GalleryRootUiState, onDownloadWithMobileData: () -> Unit, showCount: Boolean) {
    val download = state.download
    when {
        // 403 com a galeria no aparelho: a grid fica, e o aviso aparece na faixa.
        state.showMembersOnlyNotice -> BandNotice(NO_ACCESS_MESSAGE)
        download.errorCode == HTTP_FORBIDDEN && state.albums.isNotEmpty() ->
            BandNotice(download.error ?: NO_ACCESS_MESSAGE)
        download.isDownloading && download.error == null -> BandDownload(
            text = if (download.total > 0) "Baixando fotos: ${download.downloaded} de ${download.total}"
            else STARTING_DOWNLOAD_MESSAGE,
            progress = download.total.takeIf { it > 0 }?.let { download.downloaded.toFloat() / it },
        )
        download.isResuming && download.error == null -> BandNotice(RESUMING_MESSAGE)
        download.isPending && download.error == null && !state.isOnWifi ->
            BandNotice(WAITING_WIFI_MESSAGE, MOBILE_DATA_LABEL, onDownloadWithMobileData)
        download.isPending && download.error == null -> BandNotice(PENDING_WIFI_MESSAGE)
        showCount -> galleryCountLabel(state.photoCount, state.videoCount)?.let { BandText(it) }
    }
    if (state.showLowSpace) BandNotice(VideoTexts.LOW_SPACE)
}

/** Below the band when there is no album to show: login, deleted, loading, errors or empty. */
@Composable
private fun RootPlaceholder(
    state: GalleryRootUiState,
    isLoggedIn: Boolean,
    onNavigateToAuth: () -> Unit,
    onRetrySync: () -> Unit,
    onRetryDownload: () -> Unit,
    onDownloadGallery: () -> Unit,
) {
    val download = state.download
    CenteredBox {
        when {
            !isLoggedIn -> PermissionErrorPlaceholder(
                message = LOGIN_MESSAGE,
                onLoginClick = onNavigateToAuth,
                showLoginButton = true,
            )
            // Apagada nas configurações: nada sincroniza até a pessoa pedir.
            state.isPaused -> DeletedPlaceholder(onDownloadGallery)
            state.isLoading -> CircularProgressIndicator()
            // Sem índice: a galeria nunca carregou. Só 401 é problema de sessão; em 403 o usuário
            // está logado e não é membro — logar de novo não muda isso.
            state.syncError != null -> when (state.syncErrorCode) {
                HTTP_UNAUTHORIZED -> PermissionErrorPlaceholder(
                    message = state.syncError,
                    onLoginClick = onNavigateToAuth,
                    showLoginButton = true,
                )
                HTTP_FORBIDDEN -> MembersOnlyPlaceholder(message = state.syncError)
                else -> ErrorPlaceholder(message = state.syncError, onRetryClick = onRetrySync)
            }
            download.error != null && !download.isDownloading -> when (download.errorCode) {
                HTTP_UNAUTHORIZED -> PermissionErrorPlaceholder(
                    message = download.error,
                    onLoginClick = onNavigateToAuth,
                    showLoginButton = true,
                )
                HTTP_FORBIDDEN -> MembersOnlyPlaceholder(message = download.error)
                else -> ErrorPlaceholder(message = download.error, onRetryClick = onRetryDownload)
            }
            else -> Text(
                text = NO_ALBUMS_MESSAGE,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * The band, then the root albums climbing over it; in "Organizar" the albums are dragged after a long press instead
 * of opened. The videos card is the first row.
 */
@Composable
private fun RootAlbumGrid(
    state: GalleryRootUiState,
    onAlbumClick: (Long) -> Unit,
    onOrganizeMove: (from: Any?, to: Any?) -> Unit,
    videoCard: VideoCardActions,
    band: @Composable () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to -> onOrganizeMove(from.key, to.key) }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(GridSpacing),
        horizontalArrangement = Arrangement.spacedBy(GridSpacing),
        contentPadding = PaddingValues(start = GridSpacing, end = GridSpacing, bottom = GridSpacing),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(key = GalleryGridKeys.HEADER, span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fullBleed(GridSpacing).overlappedBelow(BandOverlap + GridSpacing)) { band() }
        }
        state.videoCard?.let { card ->
            item(key = VIDEO_CARD_KEY, span = { GridItemSpan(maxLineSpan) }) { VideoDiscoveryCard(card, videoCard) }
        }
        items(state.albums, key = { GalleryGridKeys.album(it.id) }) { album ->
            ReorderableItem(reorderState, key = GalleryGridKeys.album(album.id), enabled = state.isOrganizing) {
                AlbumItem(
                    album = album,
                    onClick = { if (!state.isOrganizing) onAlbumClick(album.id) },
                    modifier = Modifier.longPressDraggableHandle(enabled = state.isOrganizing),
                )
            }
        }
    }
}

/** On the root only, above the albums, until either button is tapped — then never again. */
@Composable
private fun VideoDiscoveryCard(card: VideoCard, actions: VideoCardActions) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Movie, contentDescription = null)
                Text(card.title, style = MaterialTheme.typography.titleSmall)
            }
            Text(
                text = VideoCard.SUBTITLE,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = actions.onDismiss) { Text(VideoCard.DISMISS_LABEL) }
                TextButton(onClick = actions.onAccept) { Text(VideoCard.ACCEPT_LABEL) }
            }
        }
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
private fun DeletedPlaceholder(onDownloadClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = DELETED_MESSAGE,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onDownloadClick) {
            Text(DOWNLOAD_GALLERY_LABEL)
        }
    }
}

@Composable
private fun ErrorPlaceholder(message: String, onRetryClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onRetryClick) {
            Text(RETRY_LABEL)
        }
    }
}
