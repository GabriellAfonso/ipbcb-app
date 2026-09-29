package com.ipb.castelobranco.features.gallery.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.gallery.presentation.screens.AlbumScreen
import com.ipb.castelobranco.features.gallery.presentation.screens.GalleryScreen
import com.ipb.castelobranco.features.gallery.presentation.screens.PhotoScreen
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryViewModel
import kotlinx.coroutines.flow.StateFlow

@Stable
data class GalleryNav(
    val back: () -> Unit,
    val toAlbum: (albumId: Long) -> Unit,
    val toPhoto: (albumId: Long, photoId: Long) -> Unit,
)

object GalleryRoutes {
    const val GALLERY = "GalleryMain"
    const val ARG_ALBUM_ID = "albumId"
    const val ARG_PHOTO_ID = "photoId"
    const val ALBUM = "Album/{$ARG_ALBUM_ID}"
    const val PHOTO = "Photo/{$ARG_ALBUM_ID}/{$ARG_PHOTO_ID}"

    fun album(albumId: Long) = "Album/$albumId"

    /** [albumId] is the album the viewer pages through — the one it was opened from. */
    fun photo(albumId: Long, photoId: Long) = "Photo/$albumId/$photoId"
}

/**
 * [isLoggedIn] chega de fora porque o `CoreViewModel` vive no escopo da Activity, não no back
 * stack entry de [AppRoutes.CORE]. Buscá-lo por `getBackStackEntry(CORE)` daria uma segunda
 * instância, sem `initialize()`, presa em "deslogado".
 */
fun NavGraphBuilder.galleryGraph(
    navController: NavHostController,
    isLoggedIn: StateFlow<Boolean>,
    onNavigateToAuth: () -> Unit,
) {
    val nav = GalleryNav(
        back = { navController.safePopBackStack() },
        toAlbum = { albumId -> navController.navigate(GalleryRoutes.album(albumId)) },
        toPhoto = { albumId, photoId -> navController.navigate(GalleryRoutes.photo(albumId, photoId)) },
    )

    navigation(
        route = AppRoutes.GALLERY_GRAPH,
        startDestination = GalleryRoutes.GALLERY,
    ) {
        composable(GalleryRoutes.GALLERY) { entry ->
            val viewModel = graphViewModel(navController, entry)
            val loggedIn by isLoggedIn.collectAsStateWithLifecycle()
            GalleryScreen(
                viewModel = viewModel,
                isLoggedIn = loggedIn,
                nav = nav,
                onNavigateToAuth = onNavigateToAuth,
            )
        }

        composable(
            route = GalleryRoutes.ALBUM,
            arguments = listOf(navArgument(GalleryRoutes.ARG_ALBUM_ID) { type = NavType.LongType }),
        ) { entry ->
            val albumId = entry.arguments?.getLong(GalleryRoutes.ARG_ALBUM_ID) ?: 0L
            AlbumScreen(albumId = albumId, viewModel = graphViewModel(navController, entry), nav = nav)
        }

        composable(
            route = GalleryRoutes.PHOTO,
            arguments = listOf(
                navArgument(GalleryRoutes.ARG_ALBUM_ID) { type = NavType.LongType },
                navArgument(GalleryRoutes.ARG_PHOTO_ID) { type = NavType.LongType },
            ),
        ) { entry ->
            val albumId = entry.arguments?.getLong(GalleryRoutes.ARG_ALBUM_ID) ?: 0L
            val photoId = entry.arguments?.getLong(GalleryRoutes.ARG_PHOTO_ID) ?: 0L
            PhotoScreen(
                albumId = albumId,
                photoId = photoId,
                viewModel = graphViewModel(navController, entry),
                nav = nav,
            )
        }
    }
}

@Composable
private fun graphViewModel(navController: NavHostController, entry: NavBackStackEntry): GalleryViewModel {
    val graphEntry = remember(entry) { navController.getBackStackEntry(AppRoutes.GALLERY_GRAPH) }
    return hiltViewModel(graphEntry)
}
