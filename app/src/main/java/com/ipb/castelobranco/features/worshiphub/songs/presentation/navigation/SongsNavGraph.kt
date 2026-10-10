package com.ipb.castelobranco.features.worshiphub.songs.presentation.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.worshiphub.hub.presentation.navigation.WorshipHubRoutes
import com.ipb.castelobranco.features.worshiphub.songs.presentation.screens.SongDetailScreen
import com.ipb.castelobranco.features.worshiphub.songs.presentation.screens.SongsListScreen
import com.ipb.castelobranco.features.worshiphub.songs.presentation.viewmodel.SongDetailViewModel
import com.ipb.castelobranco.features.worshiphub.songs.presentation.viewmodel.SongsListViewModel

internal object SongsRoutes {
    const val List   = "songs_list"
    const val Detail = "song_detail/{songId}"

    fun detail(id: Int) = "song_detail/$id"
}

fun NavGraphBuilder.songsGraph(navController: NavHostController) {
    navigation(
        route            = WorshipHubRoutes.Songs,
        startDestination = SongsRoutes.List,
    ) {
        composable(SongsRoutes.List) {
            val viewModel: SongsListViewModel = hiltViewModel()
            SongsListScreen(
                viewModel   = viewModel,
                onSongClick = { id -> navController.navigate(SongsRoutes.detail(id)) },
                onBackClick = { navController.safePopBackStack() },
            )
        }

        composable(
            route     = SongsRoutes.Detail,
            arguments = listOf(navArgument("songId") { type = NavType.IntType }),
        ) {
            val viewModel: SongDetailViewModel = hiltViewModel()
            SongDetailScreen(
                viewModel         = viewModel,
                onChordChartClick = { id -> navController.navigate("chord_chart_detail/$id") },
                onLyricsClick     = { id -> navController.navigate("lyrics_detail/$id") },
                onBackClick       = { navController.safePopBackStack() },
                onDeleted         = { navController.safePopBackStack() },
            )
        }
    }
}
