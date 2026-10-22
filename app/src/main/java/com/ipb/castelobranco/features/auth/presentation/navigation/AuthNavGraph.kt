package com.ipb.castelobranco.features.auth.presentation.navigation

import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.auth.presentation.screens.AuthScreen
import com.ipb.castelobranco.features.auth.presentation.screens.RegisterScreen
import com.ipb.castelobranco.features.auth.presentation.viewmodel.AuthViewModel

object AuthRoutes {
    const val AUTH = "AuthView"
    const val REGISTER = "RegisterView"
}

fun NavGraphBuilder.authGraph(
    navController: NavHostController,
    onAuthSuccess: () -> Unit,
) {
    navigation(
        route            = AppRoutes.AUTH_GRAPH,
        startDestination = AuthRoutes.AUTH,
    ) {
        composable(AuthRoutes.AUTH) { entry ->
            val graphEntry = remember(entry) { navController.getBackStackEntry(AppRoutes.AUTH_GRAPH) }
            val viewModel: AuthViewModel = hiltViewModel(graphEntry)
            AuthScreen(
                viewModel            = viewModel,
                onBackClick          = { navController.safePopBackStack() },
                onNavigateToRegister = { navController.navigate(AuthRoutes.REGISTER) },
                onAuthSuccess        = onAuthSuccess,
            )
        }

        composable(AuthRoutes.REGISTER) { entry ->
            val graphEntry = remember(entry) { navController.getBackStackEntry(AppRoutes.AUTH_GRAPH) }
            val viewModel: AuthViewModel = hiltViewModel(graphEntry)
            RegisterScreen(
                viewModel     = viewModel,
                onBackClick   = { navController.safePopBackStack() },
                onAuthSuccess = onAuthSuccess,
            )
        }
    }
}
