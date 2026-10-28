package com.ipb.castelobranco.features.bible.presentation.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.bible.presentation.screens.BibleIndexScreen
import com.ipb.castelobranco.features.bible.domain.model.BiblePlace
import com.ipb.castelobranco.features.bible.presentation.screens.BibleReaderScreen
import com.ipb.castelobranco.features.bible.presentation.screens.BibleVideoPlacesScreen
import com.ipb.castelobranco.features.bible.presentation.viewmodel.BibleVideoPlacesViewModel
import com.ipb.castelobranco.features.bible.presentation.viewmodel.BibleViewModel
import com.ipb.castelobranco.features.bible.presentation.viewmodel.BibleVideosViewModel
import kotlinx.coroutines.flow.StateFlow

object BibleRoutes {
    const val Reader      = "BibleReader"
    const val Index       = "BibleIndex"
    const val ArgInitTab  = "tab"

    fun indexRoute(tab: String = "book") = "$Index?$ArgInitTab=$tab"

    private const val VIDEO_PLACES = "BibleVideoPlaces"
    const val VideoPlacesPattern = "$VIDEO_PLACES/{${BibleVideoPlacesViewModel.ARG_VIDEO_ID}}" +
        "?${BibleVideoPlacesViewModel.ARG_PRESET_BOOK}={${BibleVideoPlacesViewModel.ARG_PRESET_BOOK}}" +
        "&${BibleVideoPlacesViewModel.ARG_PRESET_CHAPTER}={${BibleVideoPlacesViewModel.ARG_PRESET_CHAPTER}}"

    /** [preset] is a place added to the draft but not saved (the chapter being read, for an existing video). */
    fun videoPlacesRoute(videoId: Long, preset: BiblePlace? = null): String =
        "$VIDEO_PLACES/$videoId" +
            "?${BibleVideoPlacesViewModel.ARG_PRESET_BOOK}=${preset?.book.orEmpty()}" +
            "&${BibleVideoPlacesViewModel.ARG_PRESET_CHAPTER}=${preset?.chapter ?: NO_CHAPTER}"

    private const val NO_CHAPTER = 0
}

/**
 * [isLoggedIn] comes from the Activity-scoped `CoreViewModel` (see `galleryGraph`): progress controls show only for a
 * signed-in user.
 */
fun NavGraphBuilder.bibleGraph(navController: NavHostController, isLoggedIn: StateFlow<Boolean>) {
    navigation(
        route            = AppRoutes.BIBLE_GRAPH,
        startDestination = BibleRoutes.Reader,
    ) {
        composable(BibleRoutes.Reader) { entry ->
            val graphEntry = remember(entry) { navController.getBackStackEntry(AppRoutes.BIBLE_GRAPH) }
            val viewModel: BibleViewModel = hiltViewModel(graphEntry)
            val videosViewModel: BibleVideosViewModel = hiltViewModel(graphEntry)
            val loggedIn by isLoggedIn.collectAsStateWithLifecycle()
            BibleReaderScreen(
                viewModel = viewModel,
                videosViewModel = videosViewModel,
                isLoggedIn = loggedIn,
                onBack = { navController.safePopBackStack() },
                onOpenIndex = { tab -> navController.navigate(BibleRoutes.indexRoute(tab)) },
                onOpenVideoPlaces = { videoId, preset ->
                    navController.navigate(BibleRoutes.videoPlacesRoute(videoId, preset))
                },
            )
        }

        // Opened from another feature at a passage (AppRoutes.biblePassage); same reader, same graph VM.
        composable(
            route = AppRoutes.BIBLE_PASSAGE,
            arguments = listOf(
                navArgument(AppRoutes.BIBLE_PASSAGE_BOOK) { type = NavType.StringType },
                navArgument(AppRoutes.BIBLE_PASSAGE_CHAPTER) { type = NavType.IntType },
                navArgument(AppRoutes.BIBLE_PASSAGE_VERSE) {
                    type = NavType.IntType
                    defaultValue = 1
                },
                navArgument(AppRoutes.BIBLE_PASSAGE_VERSES) {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val graphEntry = remember(entry) { navController.getBackStackEntry(AppRoutes.BIBLE_GRAPH) }
            val viewModel: BibleViewModel = hiltViewModel(graphEntry)
            // During the first composition, not in an effect: the first frame already asks for the passage instead
            // of showing the last chapter read. Saved, so coming back to this entry does not apply it again.
            rememberSaveable {
                entry.arguments?.let { args ->
                    viewModel.openPassage(
                        bookAbbrev = args.getString(AppRoutes.BIBLE_PASSAGE_BOOK).orEmpty(),
                        chapter = args.getInt(AppRoutes.BIBLE_PASSAGE_CHAPTER, 1),
                        verse = args.getInt(AppRoutes.BIBLE_PASSAGE_VERSE, 1),
                        highlighted = AppRoutes.parseVerseRanges(args.getString(AppRoutes.BIBLE_PASSAGE_VERSES)),
                    )
                }
                true
            }
            val videosViewModel: BibleVideosViewModel = hiltViewModel(graphEntry)
            val loggedIn by isLoggedIn.collectAsStateWithLifecycle()
            BibleReaderScreen(
                viewModel = viewModel,
                videosViewModel = videosViewModel,
                isLoggedIn = loggedIn,
                onBack = { navController.safePopBackStack() },
                onOpenIndex = { tab -> navController.navigate(BibleRoutes.indexRoute(tab)) },
                onOpenVideoPlaces = { videoId, preset ->
                    navController.navigate(BibleRoutes.videoPlacesRoute(videoId, preset))
                },
            )
        }

        composable(
            route = "${BibleRoutes.Index}?${BibleRoutes.ArgInitTab}={${BibleRoutes.ArgInitTab}}",
            arguments = listOf(
                navArgument(BibleRoutes.ArgInitTab) {
                    type = NavType.StringType
                    defaultValue = "book"
                },
            ),
        ) { entry ->
            val tab = entry.arguments?.getString(BibleRoutes.ArgInitTab) ?: "book"
            val graphEntry = remember(entry) { navController.getBackStackEntry(AppRoutes.BIBLE_GRAPH) }
            val viewModel: BibleViewModel = hiltViewModel(graphEntry)
            val loggedIn by isLoggedIn.collectAsStateWithLifecycle()
            BibleIndexScreen(
                viewModel = viewModel,
                initialTabKey = tab,
                isLoggedIn = loggedIn,
                onBack = { navController.safePopBackStack() },
            )
        }

        // Managers only: reached from the videos sheet. Its own VM: the draft belongs to this entry.
        composable(
            route = BibleRoutes.VideoPlacesPattern,
            arguments = listOf(
                navArgument(BibleVideoPlacesViewModel.ARG_VIDEO_ID) { type = NavType.LongType },
                navArgument(BibleVideoPlacesViewModel.ARG_PRESET_BOOK) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(BibleVideoPlacesViewModel.ARG_PRESET_CHAPTER) {
                    type = NavType.IntType
                    defaultValue = 0
                },
            ),
        ) {
            val viewModel: BibleVideoPlacesViewModel = hiltViewModel()
            BibleVideoPlacesScreen(
                viewModel = viewModel,
                onBack = { navController.safePopBackStack() },
            )
        }
    }
}
