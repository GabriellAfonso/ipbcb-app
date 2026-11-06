package com.ipb.castelobranco.features.worshiphub.hub.presentation.navigation

import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.navigation.ChordChartsRoutes
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.navigation.chordChartsGraph
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.navigation.lyricsGraph
import com.ipb.castelobranco.features.worshiphub.register.presentation.screens.SundayRegistrationScreen
import com.ipb.castelobranco.features.worshiphub.songs.presentation.navigation.SongsRoutes
import com.ipb.castelobranco.features.worshiphub.songs.presentation.navigation.songsGraph
import com.ipb.castelobranco.features.worshiphub.hub.presentation.screens.WorshipHubScreen
import com.ipb.castelobranco.features.worshiphub.tables.presentation.screens.REPERTOIRE_TAB_INDEX
import com.ipb.castelobranco.features.worshiphub.tables.presentation.screens.WorshipSongsTableScreen
import com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel.SongsTableViewModel

@Stable
data class WorshipHubNav(
    val tables: () -> Unit,
    /** Tables opened on the Repertório tab: "Montar repertório". */
    val repertoire: () -> Unit,
    val chordChart: (chordChartId: Int) -> Unit,
    val songDetail: (songId: Int) -> Unit,
    val songs: () -> Unit,
    val chordCharts: () -> Unit,
    val lyrics: () -> Unit,
    val back: () -> Unit,
)

object WorshipHubRoutes {
    const val Hub     = "WorshipMain"
    const val ARG_TAB = "tab"
    const val Tables  = "WorshipTables?$ARG_TAB={$ARG_TAB}"

    fun tables(tab: Int = 0) = "WorshipTables?$ARG_TAB=$tab"
    const val Songs   = "songs"
    /** Nested graphs: `chordChartsGraph` and `lyricsGraph`. */
    const val ChordChartsGraph = "chord_charts_graph"
    const val LyricsGraph      = "lyrics_graph"
}

fun NavGraphBuilder.worshipHubGraph(navController: NavHostController) {
    navigation(
        route            = AppRoutes.WORSHIP_HUB_GRAPH,
        startDestination = WorshipHubRoutes.Hub,
    ) {
        composable(WorshipHubRoutes.Hub) {
            val nav = remember {
                WorshipHubNav(
                    tables  = { navController.navigate(WorshipHubRoutes.tables()) },
                    repertoire = { navController.navigate(WorshipHubRoutes.tables(REPERTOIRE_TAB_INDEX)) },
                    chordChart = { id -> navController.navigate(ChordChartsRoutes.detail(id)) },
                    songDetail = { id -> navController.navigate(SongsRoutes.detail(id)) },
                    songs   = { navController.navigate(WorshipHubRoutes.Songs) },
                    chordCharts = { navController.navigate(WorshipHubRoutes.ChordChartsGraph) },
                    lyrics  = { navController.navigate(WorshipHubRoutes.LyricsGraph) },
                    back    = { navController.safePopBackStack() },
                )
            }
            WorshipHubScreen(nav = nav, viewModel = hiltViewModel())
        }

        composable(
            route = WorshipHubRoutes.Tables,
            arguments = listOf(navArgument(WorshipHubRoutes.ARG_TAB) { type = NavType.IntType; defaultValue = 0 }),
        ) { entry ->
            val viewModel: SongsTableViewModel = hiltViewModel()
            WorshipSongsTableScreen(
                onBackClick = { navController.safePopBackStack() },
                onSongClick = { songId -> navController.navigate(SongsRoutes.detail(songId)) },
                onRegisterSundayClick = { navController.navigate(AppRoutes.sundayRegister()) },
                viewModel   = viewModel,
                initialTabIndex = entry.arguments?.getInt(WorshipHubRoutes.ARG_TAB) ?: 0,
            )
        }

        // "Registrar domingo": from the Tables menu, or pre-filled from administration (pending Sunday, push).
        composable(
            route = AppRoutes.SUNDAY_REGISTER,
            arguments = listOf(
                navArgument(AppRoutes.ARG_SUNDAY_DATE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            SundayRegistrationScreen(onBackClick = { navController.safePopBackStack() })
        }

        songsGraph(navController)
        chordChartsGraph(navController)
        lyricsGraph(navController)
    }
}
