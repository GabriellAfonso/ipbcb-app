package com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens.ChordChartCreateScreen
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens.ChordChartDetailScreen
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.screens.ChordChartsScreen
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel.ChordChartCreateViewModel
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel.ChordChartDetailViewModel
import com.ipb.castelobranco.features.worshiphub.chordcharts.presentation.viewmodel.ChordChartsViewModel
import com.ipb.castelobranco.features.worshiphub.hub.presentation.navigation.WorshipHubRoutes
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack

private object ChordChartsRoutes {
    const val List   = "chord_charts_list"
    const val Detail = "chord_chart_detail/{chordChartId}"
    const val Create = "chord_chart_create"

    fun detail(id: Int) = "chord_chart_detail/$id"
}

fun NavGraphBuilder.chordChartsGraph(navController: NavHostController) {
    navigation(
        route            = WorshipHubRoutes.Button3,
        startDestination = ChordChartsRoutes.List,
    ) {
        composable(ChordChartsRoutes.List) {
            val viewModel: ChordChartsViewModel = hiltViewModel()
            ChordChartsScreen(
                viewModel         = viewModel,
                onChordChartClick = { id -> navController.navigate(ChordChartsRoutes.detail(id)) },
                onCreateClick     = { navController.navigate(ChordChartsRoutes.Create) },
                onBackClick       = { navController.safePopBackStack() },
            )
        }

        composable(ChordChartsRoutes.Create) {
            val viewModel: ChordChartCreateViewModel = hiltViewModel()
            ChordChartCreateScreen(
                viewModel   = viewModel,
                onBackClick = { navController.safePopBackStack() },
            )
        }

        composable(
            route     = ChordChartsRoutes.Detail,
            arguments = listOf(navArgument("chordChartId") { type = NavType.IntType }),
        ) {
            val viewModel: ChordChartDetailViewModel = hiltViewModel()
            ChordChartDetailScreen(
                viewModel   = viewModel,
                onBackClick = { navController.safePopBackStack() },
            )
        }
    }
}
