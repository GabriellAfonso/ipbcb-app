package com.ipb.castelobranco.features.admin.reports.presentation.navigation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.admin.reports.hub.presentation.screens.ReportsHubScreen
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.screens.CollectionSettingsScreen
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.screens.HymnCardScreen
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.screens.HymnalReportScreen
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.screens.ServiceWindowsScreen
import com.ipb.castelobranco.features.admin.reports.hymnal.presentation.viewmodel.HymnalReportViewModel

/** Routes internal to the reports area, as `CLAUDE.md` requires of a feature-local graph. */
object ReportsRoutes {
    const val GRAPH = "graph/admin/reports"

    const val HUB = "ReportsHub"
    const val HYMNAL_REPORT = "ReportsHymnalReport"
    const val HYMN_CARD = "ReportsHymnCard"
    const val SERVICE_WINDOWS = "ReportsServiceWindows"
    const val COLLECTION_SETTINGS = "ReportsCollectionSettings"
}

/**
 * The reports area, nested inside `adminGraph` the way `worshipHubGraph` nests its sub-graphs.
 *
 * The nesting earns its place: [HymnalReportViewModel] is resolved against the graph entry, so the
 * hymn card opened from any list reuses the period the report already loaded instead of fetching
 * its own.
 */
fun NavGraphBuilder.reportsGraph(navController: NavHostController) {

    @Composable
    fun reportViewModel(): HymnalReportViewModel =
        hiltViewModel(navController.getBackStackEntry(ReportsRoutes.GRAPH))

    /** A refused load (403): say why and leave the whole reports area, back to the panel. */
    fun leaveArea(message: String) {
        Toast.makeText(navController.context, message, Toast.LENGTH_LONG).show()
        navController.popBackStack(ReportsRoutes.GRAPH, inclusive = true)
    }

    navigation(
        route = ReportsRoutes.GRAPH,
        startDestination = ReportsRoutes.HUB,
    ) {
        composable(ReportsRoutes.HUB) {
            ReportsHubScreen(
                onBack = { navController.safePopBackStack() },
                onOpenHymnalReport = { navController.navigate(ReportsRoutes.HYMNAL_REPORT) },
            )
        }

        composable(ReportsRoutes.HYMNAL_REPORT) {
            HymnalReportScreen(
                viewModel = reportViewModel(),
                onBack = { navController.safePopBackStack() },
                onOpenServiceWindows = { navController.navigate(ReportsRoutes.SERVICE_WINDOWS) },
                onOpenCollectionSettings = {
                    navController.navigate(ReportsRoutes.COLLECTION_SETTINGS)
                },
                onOpenHymnCard = { navController.navigate(ReportsRoutes.HYMN_CARD) },
                onLeaveArea = ::leaveArea,
            )
        }

        composable(ReportsRoutes.HYMN_CARD) {
            HymnCardScreen(
                viewModel = reportViewModel(),
                onBack = { navController.safePopBackStack() },
            )
        }

        composable(ReportsRoutes.SERVICE_WINDOWS) {
            val reportVm = reportViewModel()
            ServiceWindowsScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.safePopBackStack() },
                // A created, edited or removed service changes which slices the report offers.
                onWindowsChanged = reportVm::refreshServiceWindows,
                onLeaveArea = ::leaveArea,
            )
        }

        composable(ReportsRoutes.COLLECTION_SETTINGS) {
            CollectionSettingsScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.safePopBackStack() },
            )
        }
    }
}
