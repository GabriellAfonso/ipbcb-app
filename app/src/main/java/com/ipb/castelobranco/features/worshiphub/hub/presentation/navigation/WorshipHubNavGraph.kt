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
import com.ipb.castelobranco.core.presentation.components.InDevelopmentScreen
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.navigation.chordChartsGraph
import com.ipb.castelobranco.features.worshiphub.lyrics.presentation.navigation.lyricsGraph
import com.ipb.castelobranco.features.worshiphub.register.presentation.screens.SundayRegistrationScreen
import com.ipb.castelobranco.features.worshiphub.songs.presentation.navigation.SongsRoutes
import com.ipb.castelobranco.features.worshiphub.songs.presentation.navigation.songsGraph
import com.ipb.castelobranco.features.worshiphub.hub.presentation.screens.WorshipHubScreen
import com.ipb.castelobranco.features.worshiphub.tables.presentation.screens.WorshipSongsTableScreen
import com.ipb.castelobranco.features.worshiphub.tables.presentation.viewmodel.SongsTableViewModel

@Stable
data class WorshipHubNav(
    val tables: () -> Unit,
    val songs: () -> Unit,
    val button3: () -> Unit,
    val button4: () -> Unit,
    val button5: () -> Unit,
    val button6: () -> Unit,
    val button7: () -> Unit,
    val button8: () -> Unit,
    val back: () -> Unit,
)

object WorshipHubRoutes {
    const val Hub     = "WorshipMain"
    const val Tables  = "WorshipTables"
    const val Songs   = "songs"
    const val Button3 = "button_3"
    const val Button4 = "button_4"
    const val Button5 = "button_5"
    const val Button6 = "button_6"
    const val Button7 = "button_7"
    const val Button8 = "button_8"
}

fun NavGraphBuilder.worshipHubGraph(navController: NavHostController) {
    navigation(
        route            = AppRoutes.WORSHIP_HUB_GRAPH,
        startDestination = WorshipHubRoutes.Hub,
    ) {
        composable(WorshipHubRoutes.Hub) {
            val nav = remember {
                WorshipHubNav(
                    tables  = { navController.navigate(WorshipHubRoutes.Tables) },
                    songs   = { navController.navigate(WorshipHubRoutes.Songs) },
                    button3 = { navController.navigate(WorshipHubRoutes.Button3) },
                    button4 = { navController.navigate(WorshipHubRoutes.Button4) },
                    button5 = { navController.navigate(WorshipHubRoutes.Button5) },
                    button6 = { navController.navigate(WorshipHubRoutes.Button6) },
                    button7 = { navController.navigate(WorshipHubRoutes.Button7) },
                    button8 = { navController.navigate(WorshipHubRoutes.Button8) },
                    back    = { navController.safePopBackStack() },
                )
            }
            WorshipHubScreen(nav = nav)
        }

        composable(WorshipHubRoutes.Tables) {
            val viewModel: SongsTableViewModel = hiltViewModel()
            WorshipSongsTableScreen(
                onBackClick = { navController.safePopBackStack() },
                onSongClick = { songId -> navController.navigate(SongsRoutes.detail(songId)) },
                onRegisterSundayClick = { navController.navigate(AppRoutes.sundayRegister()) },
                viewModel   = viewModel,
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

        composable(WorshipHubRoutes.Button5) { InDevelopmentScreen(onBack = { navController.safePopBackStack() }) }
        composable(WorshipHubRoutes.Button6) { InDevelopmentScreen(onBack = { navController.safePopBackStack() }) }
        composable(WorshipHubRoutes.Button7) { InDevelopmentScreen(onBack = { navController.safePopBackStack() }) }
        composable(WorshipHubRoutes.Button8) { InDevelopmentScreen(onBack = { navController.safePopBackStack() }) }
    }
}
