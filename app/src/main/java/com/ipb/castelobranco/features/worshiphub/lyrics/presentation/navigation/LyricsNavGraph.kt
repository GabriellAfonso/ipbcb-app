package com.ipb.castelobranco.features.worshiphub.lyrics.presentation.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.worshiphub.hub.presentation.navigation.WorshipHubRoutes
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.screens.LyricsCreateScreen
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.screens.LyricsDetailScreen
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.screens.LyricsScreen
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.viewmodel.LyricsCreateViewModel
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.viewmodel.LyricsDetailViewModel
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.viewmodel.LyricsViewModel

private object LyricsRoutes {
    const val List   = "lyrics_list"
    const val Detail = "lyrics_detail/{lyricsId}"
    const val Create = "lyrics_create"

    fun detail(id: Int) = "lyrics_detail/$id"
}

/** Opens the lyrics list from outside the worship hub (a notification tap); back goes to the hub. */
fun NavController.navigateToLyrics() {
    navigate(AppRoutes.WORSHIP_HUB_GRAPH)
    navigate(WorshipHubRoutes.Button4)
}

fun NavGraphBuilder.lyricsGraph(navController: NavHostController) {
    navigation(
        route            = WorshipHubRoutes.Button4,
        startDestination = LyricsRoutes.List,
    ) {
        composable(LyricsRoutes.List) {
            val viewModel: LyricsViewModel = hiltViewModel()
            LyricsScreen(
                viewModel     = viewModel,
                onLyricsClick = { id -> navController.navigate(LyricsRoutes.detail(id)) },
                onCreateClick = { navController.navigate(LyricsRoutes.Create) },
                onBackClick   = { navController.safePopBackStack() },
            )
        }

        composable(LyricsRoutes.Create) {
            val viewModel: LyricsCreateViewModel = hiltViewModel()
            LyricsCreateScreen(
                viewModel   = viewModel,
                onBackClick = { navController.safePopBackStack() },
            )
        }

        composable(
            route     = LyricsRoutes.Detail,
            arguments = listOf(navArgument("lyricsId") { type = NavType.IntType }),
        ) {
            val viewModel: LyricsDetailViewModel = hiltViewModel()
            LyricsDetailScreen(
                viewModel   = viewModel,
                onBackClick = { navController.safePopBackStack() },
            )
        }
    }
}
