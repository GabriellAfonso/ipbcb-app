package com.ipb.castelobranco.features.gallery.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.PermissionErrorPlaceholder
import com.ipb.castelobranco.features.gallery.domain.model.Album
import com.ipb.castelobranco.features.gallery.presentation.components.AlbumItem
import com.ipb.castelobranco.features.gallery.presentation.navigation.GalleryNav
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryDownloadState
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryViewModel

private const val HTTP_UNAUTHORIZED = 401
private const val HTTP_FORBIDDEN = 403
private const val PENDING_WIFI_MESSAGE = "Aguardando WiFi para baixar a galeria…"
private const val RESUMING_MESSAGE = "Download pausado. Continua automaticamente em instantes."
private const val NO_ACCESS_MESSAGE = "Disponível apenas para membros."

@Composable
fun GalleryScreen(
    nav: GalleryNav,
    viewModel: GalleryViewModel,
    albums: List<Album>,
    isLoggedIn: Boolean,
    onNavigateToAuth: () -> Unit,
) {
    GalleryContent(
        actions = nav,
        viewModel = viewModel,
        albums = albums,
        isLoggedIn = isLoggedIn,
        onNavigateToAuth = onNavigateToAuth
    )
}

@Composable
fun GalleryContent(
    viewModel: GalleryViewModel,
    actions: GalleryNav,
    albums: List<Album>,
    isLoggedIn: Boolean = true,
    onNavigateToAuth: () -> Unit = {},
) {
    val downloadState by viewModel.downloadState.collectAsState()
    val isOnWifi by viewModel.isOnWifi.collectAsState()

    BaseScreen(
        tabName = "Galeria",
        logoRes = R.drawable.ic_galery,
        showBackArrow = true,
        onBackClick = actions.back,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (!isLoggedIn) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    PermissionErrorPlaceholder(
                        message = "Faça login para acessar a galeria.",
                        onLoginClick = onNavigateToAuth,
                        showLoginButton = true,
                    )
                }
                return@BaseScreen
            }

            // Banner de progresso não-bloqueante (visível mesmo com álbuns na grid)
            when {
                // 403 com álbuns no aparelho: a grid fica, e o aviso aparece acima dela. Sem álbuns, o
                // placeholder abaixo trata o erro.
                downloadState.errorCode == HTTP_FORBIDDEN && albums.isNotEmpty() ->
                    MessageBanner(downloadState.error ?: NO_ACCESS_MESSAGE)
                downloadState.error != null -> { /* tratado no bloco abaixo */ }
                downloadState.isDownloading -> DownloadProgressBanner(downloadState)
                downloadState.isResuming -> MessageBanner(RESUMING_MESSAGE)
                downloadState.isPending && !isOnWifi -> WaitingForWifiBanner(
                    onDownloadWithMobileData = { viewModel.downloadWithMobileData() },
                )
                downloadState.isPending -> MessageBanner(PENDING_WIFI_MESSAGE)
            }

            if (albums.isNotEmpty()) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(albums, key = { it.id }) { album ->
                        AlbumItem(
                            album = album,
                            viewModel = viewModel,
                            onClick = { actions.toAlbum(album.id) },
                        )
                    }
                }
            } else if (!downloadState.isDownloading) {
                // Galeria vazia e nenhum download em andamento
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    val errorMsg = downloadState.error
                    when {
                        !downloadState.isResolved -> CircularProgressIndicator()

                        // Só 401 é problema de sessão. Em 403 o usuário está logado e não é
                        // membro — logar de novo não muda isso. Nos demais erros a falha é de
                        // rede/servidor, e mandar o usuário para o login não resolve nada.
                        errorMsg != null -> when (downloadState.errorCode) {
                            HTTP_UNAUTHORIZED -> PermissionErrorPlaceholder(
                                message = errorMsg,
                                onLoginClick = onNavigateToAuth,
                                showLoginButton = true,
                            )
                            HTTP_FORBIDDEN -> PermissionErrorPlaceholder(
                                message = errorMsg,
                                onLoginClick = onNavigateToAuth,
                                showLoginButton = false,
                            )
                            else -> DownloadErrorPlaceholder(
                                message = errorMsg,
                                onRetryClick = { viewModel.retryDownload() },
                            )
                        }

                        else -> EmptyGalleryPlaceholder(
                            onDownloadClick = { viewModel.downloadAllPhotos() },
                        )
                    }
                }
            }
        }
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
private fun DownloadErrorPlaceholder(message: String, onRetryClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        OutlinedButton(onClick = onRetryClick) {
            Text("Tentar novamente")
        }
    }
}

@Composable
private fun EmptyGalleryPlaceholder(onDownloadClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Nenhum álbum disponível localmente.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDownloadClick,
            modifier = Modifier.height(56.dp),
        ) {
            Text("Baixar Galeria Completa")
        }
    }
}
