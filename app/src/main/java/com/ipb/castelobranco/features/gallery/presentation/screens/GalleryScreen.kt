package com.ipb.castelobranco.features.gallery.presentation.screens

import android.widget.Toast
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.PermissionErrorPlaceholder
import com.ipb.castelobranco.features.gallery.presentation.components.AlbumItem
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryDialogActions
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryDialogHost
import com.ipb.castelobranco.features.gallery.presentation.components.GalleryGridKeys
import com.ipb.castelobranco.features.gallery.presentation.components.ManageAction
import com.ipb.castelobranco.features.gallery.presentation.components.ManageOverflowMenu
import com.ipb.castelobranco.features.gallery.presentation.components.OrganizeActions
import com.ipb.castelobranco.features.gallery.presentation.navigation.GalleryNav
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryDownloadState
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryMessage
import com.ipb.castelobranco.features.gallery.presentation.state.GalleryRootUiState
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val TITLE = "Galeria"
private const val LOGIN_MESSAGE = "Faça login para acessar a galeria."
private const val PENDING_WIFI_MESSAGE = "Aguardando WiFi para baixar a galeria…"
private const val RESUMING_MESSAGE = "Download pausado. Continua automaticamente em instantes."
private const val NO_ACCESS_MESSAGE = "Disponível apenas para membros."
private const val NO_ALBUMS_MESSAGE = "Nenhum álbum disponível."
private const val RETRY_LABEL = "Tentar novamente"
private const val ORGANIZE_TITLE = "Organizar"
private const val ORGANIZE_LABEL = "Organizar"
private const val NEW_ALBUM_LABEL = "Novo álbum"

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
    GalleryMessageEffect(message, viewModel::consumeMessage)
    BackHandler(enabled = state.isOrganizing) { viewModel.cancelOrganize() }

    GalleryContent(
        state = state,
        isLoggedIn = isLoggedIn,
        onBack = nav.back,
        onAlbumClick = nav.toAlbum,
        onNavigateToAuth = onNavigateToAuth,
        onRetrySync = viewModel::retrySync,
        onRetryDownload = viewModel::retryDownload,
        onDownloadWithMobileData = viewModel::downloadWithMobileData,
        manage = RootManageActions(
            onNewAlbum = { viewModel.openCreateAlbum(null) },
            onOrganize = { viewModel.startOrganize(null) },
            onOrganizeMove = viewModel::moveOrganizeItem,
            onOrganizeSave = viewModel::saveOrganize,
            onOrganizeCancel = viewModel::cancelOrganize,
        ),
    )
    GalleryDialogHost(dialog, rememberDialogActions(viewModel))
}

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
    )
}

/** Shows a removal notice once, from whichever gallery screen is on top when it arrives. */
@Composable
internal fun GalleryMessageEffect(message: GalleryMessage?, onShown: () -> Unit) {
    val context = LocalContext.current
    LaunchedEffect(message) {
        if (message != null) {
            Toast.makeText(context, message.text, Toast.LENGTH_SHORT).show()
            onShown()
        }
    }
}

@Composable
fun GalleryContent(
    state: GalleryRootUiState,
    isLoggedIn: Boolean,
    onBack: () -> Unit,
    onAlbumClick: (Long) -> Unit,
    onNavigateToAuth: () -> Unit,
    onRetrySync: () -> Unit,
    onRetryDownload: () -> Unit,
    onDownloadWithMobileData: () -> Unit,
    manage: RootManageActions,
) {
    val download = state.download
    val canManage = isLoggedIn && state.permissions.canManage

    BaseScreen(
        tabName = if (state.isOrganizing) ORGANIZE_TITLE else TITLE,
        logoRes = R.drawable.ic_galery,
        showBackArrow = true,
        onBackClick = if (state.isOrganizing) manage.onOrganizeCancel else onBack,
        extraActions = {
            when {
                state.isOrganizing ->
                    OrganizeActions(state.isSavingOrder, manage.onOrganizeCancel, manage.onOrganizeSave)
                canManage && state.albums.size > 1 ->
                    ManageOverflowMenu(listOf(ManageAction(ORGANIZE_LABEL, manage.onOrganize)))
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (!isLoggedIn) {
                    CenteredBox {
                        PermissionErrorPlaceholder(
                            message = LOGIN_MESSAGE,
                            onLoginClick = onNavigateToAuth,
                            showLoginButton = true,
                        )
                    }
                    return@Column
                }

                // Banner não-bloqueante (visível mesmo com álbuns na grid)
                when {
                    // 403 com a galeria no aparelho: a grid fica, e o aviso aparece acima dela.
                    state.showMembersOnlyNotice -> MessageBanner(NO_ACCESS_MESSAGE)
                    download.errorCode == HTTP_FORBIDDEN && state.albums.isNotEmpty() ->
                        MessageBanner(download.error ?: NO_ACCESS_MESSAGE)
                    download.error != null -> { /* tratado no bloco abaixo */ }
                    download.isDownloading -> DownloadProgressBanner(download)
                    download.isResuming -> MessageBanner(RESUMING_MESSAGE)
                    download.isPending && !state.isOnWifi -> WaitingForWifiBanner(onDownloadWithMobileData)
                    download.isPending -> MessageBanner(PENDING_WIFI_MESSAGE)
                }

                when {
                    state.albums.isNotEmpty() -> RootAlbumGrid(state, onAlbumClick, manage.onOrganizeMove)

                    state.isLoading -> CenteredBox { CircularProgressIndicator() }

                    // Sem índice: a galeria nunca carregou. Só 401 é problema de sessão; em 403 o usuário
                    // está logado e não é membro — logar de novo não muda isso.
                    state.syncError != null -> CenteredBox {
                        when (state.syncErrorCode) {
                            HTTP_UNAUTHORIZED -> PermissionErrorPlaceholder(
                                message = state.syncError,
                                onLoginClick = onNavigateToAuth,
                                showLoginButton = true,
                            )
                            HTTP_FORBIDDEN -> PermissionErrorPlaceholder(
                                message = state.syncError,
                                onLoginClick = onNavigateToAuth,
                                showLoginButton = false,
                            )
                            else -> ErrorPlaceholder(message = state.syncError, onRetryClick = onRetrySync)
                        }
                    }

                    download.error != null && !download.isDownloading -> CenteredBox {
                        when (download.errorCode) {
                            HTTP_UNAUTHORIZED -> PermissionErrorPlaceholder(
                                message = download.error,
                                onLoginClick = onNavigateToAuth,
                                showLoginButton = true,
                            )
                            HTTP_FORBIDDEN -> PermissionErrorPlaceholder(
                                message = download.error,
                                onLoginClick = onNavigateToAuth,
                                showLoginButton = false,
                            )
                            else -> ErrorPlaceholder(message = download.error, onRetryClick = onRetryDownload)
                        }
                    }

                    else -> CenteredBox {
                        Text(
                            text = NO_ALBUMS_MESSAGE,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                        )
                    }
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
        }
    }
}

/** The root albums; in "Organizar" they are dragged after a long press instead of opened. */
@Composable
private fun RootAlbumGrid(
    state: GalleryRootUiState,
    onAlbumClick: (Long) -> Unit,
    onOrganizeMove: (from: Any?, to: Any?) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val reorderState = rememberReorderableLazyGridState(gridState) { from, to -> onOrganizeMove(from.key, to.key) }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
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
private fun DownloadProgressBanner(state: GalleryDownloadState) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(
                    text = if (state.total > 0)
                        "Baixando fotos: ${state.downloaded} / ${state.total}"
                    else
                        "Iniciando download…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            if (state.total > 0) {
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { state.downloaded.toFloat() / state.total.toFloat() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Banner informativo de uma linha, sem ação — não é estado de erro. */
@Composable
private fun MessageBanner(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun WaitingForWifiBanner(onDownloadWithMobileData: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Aguardando WiFi…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onDownloadWithMobileData) {
                Text("Usar dados móveis")
            }
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
