package com.ipb.castelobranco.features.settings.presentation.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.settings.presentation.screens.GallerySettingsScreen
import com.ipb.castelobranco.features.settings.presentation.screens.LogViewerScreen
import com.ipb.castelobranco.features.settings.presentation.screens.SettingsScreen

object SettingsRoutes {
    const val GALLERY = "settings/gallery"
}

/** Configurações (start: [AppRoutes.SETTINGS]), its Galeria screen and the hidden logs. */
fun NavGraphBuilder.settingsGraph(navController: NavHostController) {
    navigation(route = AppRoutes.SETTINGS_GRAPH, startDestination = AppRoutes.SETTINGS) {
        composable(AppRoutes.SETTINGS) {
            SettingsScreen(
                onBackClick = { navController.safePopBackStack() },
                onNavigateToLogViewer = { navController.navigate(AppRoutes.LOG_VIEWER) },
                onNavigateToGallery = { navController.navigate(SettingsRoutes.GALLERY) },
            )
        }

        composable(SettingsRoutes.GALLERY) {
            GallerySettingsScreen(onBackClick = { navController.safePopBackStack() })
        }

        composable(AppRoutes.LOG_VIEWER) {
            LogViewerScreen(onBackClick = { navController.safePopBackStack() })
        }
    }
}
