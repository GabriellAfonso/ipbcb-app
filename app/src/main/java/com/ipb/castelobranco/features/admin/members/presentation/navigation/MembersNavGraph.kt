package com.ipb.castelobranco.features.admin.members.presentation.navigation

import android.widget.Toast
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.admin.members.presentation.screens.MemberFormScreen
import com.ipb.castelobranco.features.admin.members.presentation.screens.MemberHistoryScreen
import com.ipb.castelobranco.features.admin.members.presentation.screens.MemberProfileScreen
import com.ipb.castelobranco.features.admin.members.presentation.screens.MembersListScreen
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent

/** Routes internal to the members area (`CLAUDE.md`: feature-local routes live with the graph). */
object MembersRoutes {
    const val GRAPH = "graph/admin/members"

    const val ARG_MEMBER_ID = "memberId"

    const val LIST = "MembersList"
    const val PROFILE = "MembersProfile/{$ARG_MEMBER_ID}"
    const val FORM = "MembersForm?$ARG_MEMBER_ID={$ARG_MEMBER_ID}"
    const val HISTORY = "MembersHistory/{$ARG_MEMBER_ID}"

    fun profile(memberId: Int) = "MembersProfile/$memberId"
    fun newMember() = "MembersForm"
    fun editMember(memberId: Int) = "MembersForm?$ARG_MEMBER_ID=$memberId"
    fun history(memberId: Int) = "MembersHistory/$memberId"
}

/**
 * The members area, nested inside `adminGraph` like `reportsGraph`. Each screen has its own
 * ViewModel: what they share — the roll — lives in the singleton repository, in memory, so no
 * graph-scoped ViewModel is needed to keep the list and the profile in step.
 */
fun NavGraphBuilder.membersGraph(navController: NavHostController) {

    /** 403 leaves the area; 404 goes back to the list. Both say why. */
    fun handleNavigation(event: MembersEvent) {
        when (event) {
            is MembersEvent.LeaveArea -> {
                toast(navController, event.message)
                navController.popBackStack(MembersRoutes.GRAPH, inclusive = true)
            }
            is MembersEvent.MemberGone -> {
                toast(navController, event.message)
                navController.popBackStack(MembersRoutes.LIST, inclusive = false)
            }
            MembersEvent.Deleted -> navController.popBackStack(MembersRoutes.LIST, inclusive = false)
            else -> Unit
        }
    }

    val memberIdArgument = navArgument(MembersRoutes.ARG_MEMBER_ID) { type = NavType.IntType }

    navigation(
        route = MembersRoutes.GRAPH,
        startDestination = MembersRoutes.LIST,
    ) {
        composable(MembersRoutes.LIST) {
            MembersListScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.safePopBackStack() },
                onOpenMember = { id -> navController.navigate(MembersRoutes.profile(id)) },
                onAddMember = { navController.navigate(MembersRoutes.newMember()) },
                onNavigationEvent = ::handleNavigation,
            )
        }

        composable(MembersRoutes.PROFILE, arguments = listOf(memberIdArgument)) {
            MemberProfileScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.safePopBackStack() },
                onEdit = { id -> navController.navigate(MembersRoutes.editMember(id)) },
                onOpenHistory = { id -> navController.navigate(MembersRoutes.history(id)) },
                onNavigationEvent = ::handleNavigation,
            )
        }

        composable(
            MembersRoutes.FORM,
            arguments = listOf(
                navArgument(MembersRoutes.ARG_MEMBER_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val editing = entry.arguments?.getString(MembersRoutes.ARG_MEMBER_ID) != null
            MemberFormScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.safePopBackStack() },
                onEvent = { event ->
                    when {
                        event is MembersEvent.Saved && editing -> navController.safePopBackStack()
                        event is MembersEvent.Saved -> navController.navigate(MembersRoutes.profile(event.memberId)) {
                            popUpTo(MembersRoutes.FORM) { inclusive = true }
                        }
                        else -> handleNavigation(event)
                    }
                },
            )
        }

        composable(MembersRoutes.HISTORY, arguments = listOf(memberIdArgument)) {
            MemberHistoryScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.safePopBackStack() },
                onNavigationEvent = ::handleNavigation,
            )
        }
    }
}

private fun toast(navController: NavHostController, message: String) {
    Toast.makeText(navController.context, message, Toast.LENGTH_LONG).show()
}
