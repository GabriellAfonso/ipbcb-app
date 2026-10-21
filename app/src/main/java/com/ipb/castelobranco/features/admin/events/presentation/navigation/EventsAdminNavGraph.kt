package com.ipb.castelobranco.features.admin.events.presentation.navigation

import android.widget.Toast
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.admin.events.presentation.screens.EventFormScreen
import com.ipb.castelobranco.features.admin.events.presentation.screens.EventsAdminListScreen
import com.ipb.castelobranco.features.admin.events.presentation.state.EventsAdminEvent

/** Routes internal to the events management area (`CLAUDE.md`: feature-local routes live with the graph). */
object EventsAdminRoutes {
    const val GRAPH = "graph/admin/events"

    const val ARG_EVENT_ID = "eventId"

    const val LIST = "EventsAdminList"
    const val FORM = "EventsAdminForm?$ARG_EVENT_ID={$ARG_EVENT_ID}"

    fun newEvent() = "EventsAdminForm"
    fun event(eventId: Int) = "EventsAdminForm?$ARG_EVENT_ID=$eventId"
}

/**
 * The events management area, nested inside `adminGraph` like `membersGraph`. Each screen has its own
 * ViewModel and nothing is shared: the list is read again whenever it comes back to the screen.
 */
fun NavGraphBuilder.eventsAdminGraph(navController: NavHostController) {

    /** A refused read leaves the area; 404, a delete and a save go back to the list. */
    fun handleNavigation(event: EventsAdminEvent) {
        when (event) {
            is EventsAdminEvent.LeaveArea -> {
                toast(navController, event.message)
                navController.popBackStack(EventsAdminRoutes.GRAPH, inclusive = true)
            }
            is EventsAdminEvent.EventGone -> {
                toast(navController, event.message)
                navController.popBackStack(EventsAdminRoutes.LIST, inclusive = false)
            }
            EventsAdminEvent.Saved, EventsAdminEvent.Deleted ->
                navController.popBackStack(EventsAdminRoutes.LIST, inclusive = false)
            is EventsAdminEvent.ShowMessage -> Unit
        }
    }

    navigation(
        route = EventsAdminRoutes.GRAPH,
        startDestination = EventsAdminRoutes.LIST,
    ) {
        composable(EventsAdminRoutes.LIST) {
            EventsAdminListScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.safePopBackStack() },
                onOpenEvent = { id -> navController.navigate(EventsAdminRoutes.event(id)) },
                onNewEvent = { navController.navigate(EventsAdminRoutes.newEvent()) },
                onNavigationEvent = ::handleNavigation,
            )
        }

        composable(
            EventsAdminRoutes.FORM,
            arguments = listOf(
                navArgument(EventsAdminRoutes.ARG_EVENT_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            EventFormScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.safePopBackStack() },
                onEvent = ::handleNavigation,
            )
        }
    }
}

private fun toast(navController: NavHostController, message: String) {
    Toast.makeText(navController.context, message, Toast.LENGTH_LONG).show()
}
