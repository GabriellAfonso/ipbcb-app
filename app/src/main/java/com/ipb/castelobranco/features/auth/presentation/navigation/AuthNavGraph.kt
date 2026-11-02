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
import com.ipb.castelobranco.features.auth.presentation.screens.PasswordRecoveryScreen
import com.ipb.castelobranco.features.auth.presentation.screens.RegisterScreen
import com.ipb.castelobranco.features.auth.presentation.viewmodel.AuthViewModel

object AuthRoutes {
    const val AUTH = "AuthView"
    const val REGISTER = "RegisterView"
    const val PASSWORD_RECOVERY = "PasswordRecoveryView"
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
                onNavigateToPasswordRecovery = { navController.navigate(AuthRoutes.PASSWORD_RECOVERY) },
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

        // Scoped to its own entry, not the graph: leaving and coming back must not keep an old code or step.
        composable(AuthRoutes.PASSWORD_RECOVERY) { entry ->
            val graphEntry = remember(entry) { navController.getBackStackEntry(AppRoutes.AUTH_GRAPH) }
            val authViewModel: AuthViewModel = hiltViewModel(graphEntry)
            PasswordRecoveryScreen(
                onBackClick = { navController.safePopBackStack() },
                onPasswordReset = {
                    authViewModel.onPasswordReset()
                    navController.safePopBackStack()
                },
            )
        }
    }
}
