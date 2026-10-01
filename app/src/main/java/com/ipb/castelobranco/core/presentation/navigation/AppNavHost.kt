package com.ipb.castelobranco.core.presentation.navigation

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.logEvent
import com.ipb.castelobranco.features.admin.panel.presentation.navigation.adminGraph
import com.ipb.castelobranco.features.auth.presentation.navigation.authGraph
import com.ipb.castelobranco.features.bible.presentation.navigation.bibleGraph
import com.ipb.castelobranco.features.gallery.presentation.navigation.galleryGraph
import com.ipb.castelobranco.features.hymnal.presentation.navigation.hymnalGraph
import com.ipb.castelobranco.core.presentation.screens.BirthdaysScreen
import com.ipb.castelobranco.core.presentation.screens.CoreView
import com.ipb.castelobranco.core.presentation.viewmodel.CoreViewModel
import com.ipb.castelobranco.features.profile.presentation.screens.ProfileScreen
import com.ipb.castelobranco.features.schedule.presentation.screens.MonthScheduleScreen
import com.ipb.castelobranco.features.settings.presentation.screens.LogViewerScreen
import com.ipb.castelobranco.features.settings.presentation.screens.SettingsScreen
import com.ipb.castelobranco.features.studies.presentation.navigation.studiesGraph
import com.ipb.castelobranco.features.worshiphub.hub.presentation.navigation.worshipHubGraph

@Composable
fun AppNavHost(navController: NavHostController) {
    val context = LocalContext.current

    // Escopo da Activity (fora do NavHost): o boot roda mesmo quando o processo e recriado
    // direto numa rota interna, onde a CoreView nunca chega a ser composta.
    val coreViewModel: CoreViewModel = hiltViewModel()
    LaunchedEffect(Unit) { coreViewModel.initialize() }
    // Abertura do app e volta do background: a galeria confere o servidor.
    LifecycleEventEffect(Lifecycle.Event.ON_START) { coreViewModel.onAppForeground() }

    val appNavigator = remember(navController) {
        AppNavigator(
            navigateToProfile = { navController.navigate(AppRoutes.PROFILE) },
            navigateToAuth    = { navController.navigate(AppRoutes.AUTH_GRAPH) },
        )
    }

    val backStackEntry = navController.currentBackStackEntryAsState()
    LaunchedEffect(backStackEntry.value) {
        val route = backStackEntry.value?.destination?.route ?: return@LaunchedEffect
        FirebaseAnalytics.getInstance(context).logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            param(FirebaseAnalytics.Param.SCREEN_NAME, route)
            param(FirebaseAnalytics.Param.SCREEN_CLASS, route)
        }
    }

    CompositionLocalProvider(LocalAppNavigator provides appNavigator) {
        NavHost(
            navController        = navController,
            startDestination     = AppRoutes.CORE,
            enterTransition      = { EnterTransition.None },
            exitTransition       = { ExitTransition.None },
            popEnterTransition   = { EnterTransition.None },
            popExitTransition    = { ExitTransition.None },
        ) {

            composable(AppRoutes.CORE) {
                BackHandler {}
                CoreView(
                    onNavigateToAuth       = { navController.navigate(AppRoutes.AUTH_GRAPH) },
                    onNavigateToWorshipHub = { navController.navigate(AppRoutes.WORSHIP_HUB_GRAPH) },
                    onNavigateToSchedule   = { navController.navigate(AppRoutes.SCHEDULE) },
                    onNavigateToGallery    = { navController.navigate(AppRoutes.GALLERY_GRAPH) },
                    onNavigateToHymnal     = { navController.navigate(AppRoutes.HYMNAL_GRAPH) },
                    onNavigateToBible      = { navController.navigate(AppRoutes.BIBLE_GRAPH) },
                    onNavigateToStudies    = { navController.navigate(AppRoutes.STUDIES_GRAPH) },
                    onNavigateToSettings   = { navController.navigate(AppRoutes.SETTINGS) },
                    onNavigateToAdmin      = { navController.navigate(AppRoutes.ADMIN_GRAPH) },
                    onNavigateToBirthdays  = { navController.navigate(AppRoutes.BIRTHDAYS) },
                    onLogoutSuccess        = {},
                    viewModel              = coreViewModel,
                )
            }

            authGraph(
                navController = navController,
                onAuthSuccess = {
                    navController.navigate(AppRoutes.CORE) {
                        popUpTo(AppRoutes.AUTH_GRAPH) { inclusive = true }
                    }
                    Toast.makeText(context, "Sessão iniciada", Toast.LENGTH_SHORT).show()
                },
            )

            adminGraph(navController)
            worshipHubGraph(navController)
            galleryGraph(
                navController = navController,
                isLoggedIn = coreViewModel.isLoggedIn,
                onNavigateToAuth = { navController.navigate(AppRoutes.AUTH_GRAPH) },
            )
            hymnalGraph(navController)
            bibleGraph(navController)

            composable(AppRoutes.SCHEDULE) {
                MonthScheduleScreen(
                    onBackClick = { navController.safePopBackStack() },
                    onShare     = { text ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, text)
                        }
                        context.startActivity(Intent.createChooser(intent, "Compartilhar"))
                    },
                    onNavigateToAuth = { navController.navigate(AppRoutes.AUTH_GRAPH) },
                )
            }

            studiesGraph(navController)

            composable(AppRoutes.BIRTHDAYS) {
                BirthdaysScreen(
                    onBackClick      = { navController.safePopBackStack() },
                    onNavigateToAuth = { navController.navigate(AppRoutes.AUTH_GRAPH) },
                )
            }

            composable(AppRoutes.SETTINGS) {
                SettingsScreen(
                    onBackClick = { navController.safePopBackStack() },
                    onNavigateToLogViewer = { navController.navigate(AppRoutes.LOG_VIEWER) },
                )
            }

            composable(AppRoutes.LOG_VIEWER) {
                LogViewerScreen(onBackClick = { navController.safePopBackStack() })
            }

            composable(AppRoutes.PROFILE) {
                ProfileScreen(onBackClick = { navController.safePopBackStack() })
            }
        }
    }
}
