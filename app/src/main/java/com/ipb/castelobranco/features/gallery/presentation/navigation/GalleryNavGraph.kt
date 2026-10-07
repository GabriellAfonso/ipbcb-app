package com.ipb.castelobranco.features.gallery.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.ipb.castelobranco.features.gallery.presentation.screens.PeopleScreen
import com.ipb.castelobranco.features.gallery.presentation.screens.PhotoScreen
import com.ipb.castelobranco.features.gallery.presentation.screens.TrashScreen
import com.ipb.castelobranco.features.gallery.presentation.state.ViewerSource
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.GalleryViewModel
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.PeopleViewModel
import com.ipb.castelobranco.features.gallery.presentation.viewmodel.TrashViewModel
import kotlinx.coroutines.flow.StateFlow

@Stable
data class GalleryNav(
    val back: () -> Unit,
    val toAlbum: (albumId: Long) -> Unit,
    val toPhoto: (albumId: Long, photoId: Long) -> Unit,
    val toTrash: () -> Unit,
    val toPeople: () -> Unit = {},
    /** The viewer over the photos with every one of [memberIds]. */
    val toPeoplePhoto: (memberIds: Set<Long>, photoId: Long) -> Unit = { _, _ -> },
)

object GalleryRoutes {
    const val GALLERY = "GalleryMain"
    const val ARG_ALBUM_ID = "albumId"
    const val ARG_PHOTO_ID = "photoId"
    const val ALBUM = "Album/{$ARG_ALBUM_ID}"
    const val PHOTO = "Photo/{$ARG_ALBUM_ID}/{$ARG_PHOTO_ID}"
    const val TRASH = "GalleryTrash"
    const val ARG_MEMBER_IDS = "memberIds"
    const val PEOPLE = "GalleryPeople"
    const val PEOPLE_PHOTO = "PeoplePhoto/{$ARG_MEMBER_IDS}/{$ARG_PHOTO_ID}"
    private const val ID_SEPARATOR = ","

    /** [memberIds] travel comma-separated: the viewer pages through the photos with all of them. */
    fun peoplePhoto(memberIds: Set<Long>, photoId: Long) =
        "PeoplePhoto/${memberIds.sorted().joinToString(ID_SEPARATOR)}/$photoId"

    fun memberIdsOf(raw: String?): Set<Long> =
        raw.orEmpty().split(ID_SEPARATOR).mapNotNull { it.trim().toLongOrNull() }.toSet()

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
        toTrash = { navController.navigate(GalleryRoutes.TRASH) },
        toPeople = { navController.navigate(GalleryRoutes.PEOPLE) },
        toPeoplePhoto = { memberIds, photoId ->
            navController.navigate(GalleryRoutes.peoplePhoto(memberIds, photoId))
        },
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
            val viewModel = graphViewModel(navController, entry)
            PhotoScreen(
                viewerId = viewerId(entry, viewModel),
                source = ViewerSource.Album(albumId),
                photoId = photoId,
                viewModel = viewModel,
                nav = nav,
            )
        }

        composable(
            route = GalleryRoutes.PEOPLE_PHOTO,
            arguments = listOf(
                navArgument(GalleryRoutes.ARG_MEMBER_IDS) { type = NavType.StringType },
                navArgument(GalleryRoutes.ARG_PHOTO_ID) { type = NavType.LongType },
            ),
        ) { entry ->
            val memberIds = GalleryRoutes.memberIdsOf(entry.arguments?.getString(GalleryRoutes.ARG_MEMBER_IDS))
            val photoId = entry.arguments?.getLong(GalleryRoutes.ARG_PHOTO_ID) ?: 0L
            val viewModel = graphViewModel(navController, entry)
            PhotoScreen(
                viewerId = viewerId(entry, viewModel),
                source = ViewerSource.People(memberIds),
                photoId = photoId,
                viewModel = viewModel,
                nav = nav,
            )
        }

        // The filter: its own ViewModel, so the selection lasts only this visit.
        composable(GalleryRoutes.PEOPLE) { entry ->
            PeopleScreen(
                viewModel = hiltViewModel<PeopleViewModel>(),
                galleryViewModel = graphViewModel(navController, entry),
                nav = nav,
            )
        }

        // Its own ViewModel, tied to this entry: the list is read again on every visit.
        composable(GalleryRoutes.TRASH) {
            TrashScreen(viewModel = hiltViewModel<TrashViewModel>(), nav = nav)
        }
    }
}

/** Lives in the viewer's entry: cleared when the entry leaves the stack, kept across a rotation. */
private class ViewerLease : ViewModel()

/** The viewer's id is its entry's; the [GalleryViewModel] drops that viewer once the entry is gone. */
@Composable
private fun viewerId(entry: NavBackStackEntry, galleryViewModel: GalleryViewModel): String {
    val viewerId = entry.id
    viewModel(viewModelStoreOwner = entry) {
        ViewerLease().apply { addCloseable { galleryViewModel.closeViewer(viewerId) } }
    }
    return viewerId
}

@Composable
private fun graphViewModel(navController: NavHostController, entry: NavBackStackEntry): GalleryViewModel {
    val graphEntry = remember(entry) { navController.getBackStackEntry(AppRoutes.GALLERY_GRAPH) }
    return hiltViewModel(graphEntry)
}
