package com.ipb.castelobranco.features.admin.profiles.presentation.navigation

import android.widget.Toast
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.admin.profiles.presentation.screens.ProfileDetailScreen
import com.ipb.castelobranco.features.admin.profiles.presentation.screens.ProfilesListScreen
import com.ipb.castelobranco.features.admin.profiles.presentation.viewmodel.ProfileDetailViewModel

object ProfilesRoutes {
    const val GRAPH = "graph/admin/profiles"

    const val LIST = "ProfilesList"
    const val DETAIL = "ProfilesDetail/{${ProfileDetailViewModel.ARG_USER_ID}}"

    fun detail(userId: String) = "ProfilesDetail/$userId"
}

/**
 * "Perfis", nested inside `adminGraph` like Membros. The lists live in the singleton repository, so list and
 * detail need no shared ViewModel. The list opens on "Pendentes".
 */
fun NavGraphBuilder.profilesGraph(navController: NavHostController) {

    fun leaveArea(message: String) {
        Toast.makeText(navController.context, message, Toast.LENGTH_LONG).show()
        navController.popBackStack(ProfilesRoutes.GRAPH, inclusive = true)
    }

    navigation(route = ProfilesRoutes.GRAPH, startDestination = ProfilesRoutes.LIST) {
        composable(ProfilesRoutes.LIST) {
            ProfilesListScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.safePopBackStack() },
                onOpenAccount = { id -> navController.navigate(ProfilesRoutes.detail(id)) },
                onLeaveArea = ::leaveArea,
            )
        }

        composable(
            ProfilesRoutes.DETAIL,
            arguments = listOf(navArgument(ProfileDetailViewModel.ARG_USER_ID) { type = NavType.StringType }),
        ) {
            ProfileDetailScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.safePopBackStack() },
                onLeaveArea = ::leaveArea,
            )
        }
    }
}
