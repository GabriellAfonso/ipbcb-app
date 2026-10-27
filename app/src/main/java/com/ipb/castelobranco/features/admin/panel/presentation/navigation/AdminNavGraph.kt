package com.ipb.castelobranco.features.admin.panel.presentation.navigation

import android.content.Intent
import androidx.compose.runtime.Stable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.admin.panel.presentation.screens.AdminScreen
import com.ipb.castelobranco.features.admin.events.presentation.navigation.EventsAdminRoutes
import com.ipb.castelobranco.features.admin.events.presentation.navigation.eventsAdminGraph
import com.ipb.castelobranco.features.admin.members.presentation.navigation.MembersRoutes
import com.ipb.castelobranco.features.admin.members.presentation.navigation.membersGraph
import com.ipb.castelobranco.features.admin.profiles.presentation.navigation.ProfilesRoutes
import com.ipb.castelobranco.features.admin.profiles.presentation.navigation.profilesGraph
import com.ipb.castelobranco.features.admin.reports.presentation.navigation.ReportsRoutes
import com.ipb.castelobranco.features.admin.reports.presentation.navigation.reportsGraph
import com.ipb.castelobranco.features.admin.schedule.presentation.screens.AdminScheduleScreen
import java.time.LocalDate

@Stable
data class AdminNav(
    val back: () -> Unit,
    val schedule: () -> Unit,
    val reports: () -> Unit,
    val members: () -> Unit,
    val events: () -> Unit,
    val profiles: () -> Unit = {},
    val sundaySchool: () -> Unit = {},
    val confirmSunday: (LocalDate) -> Unit = {},
)

object AdminRoutes {
    const val ADMIN    = "AdminMain"
    const val SCHEDULE = "AdminSchedule"
}

/**
 * Opens the pre-filled "Registrar domingo" (worship hub) from a notification tap. Goes through the panel so back
 * lands there, as it does from the pending card.
 */
fun NavController.navigateToSundayConfirmation(date: LocalDate) {
    navigate(AppRoutes.ADMIN_GRAPH)
    navigate(AppRoutes.sundayRegister(date))
}

/** Opens "Perfis" (on Pendentes) from outside administration (a notification tap), through the panel. */
fun NavController.navigateToProfilesPending() {
    navigate(AppRoutes.ADMIN_GRAPH)
    navigate(ProfilesRoutes.GRAPH)
}

fun NavGraphBuilder.adminGraph(navController: NavHostController) {
    fun nav() = AdminNav(
        back     = { navController.safePopBackStack() },
        schedule = { navController.navigate(AdminRoutes.SCHEDULE) },
        reports  = { navController.navigate(ReportsRoutes.GRAPH) },
        members  = { navController.navigate(MembersRoutes.GRAPH) },
        events   = { navController.navigate(EventsAdminRoutes.GRAPH) },
        profiles = { navController.navigate(ProfilesRoutes.GRAPH) },
        // Managed inside the studies feature, which owns the lesson renderer the preview needs.
        sundaySchool = { navController.navigate(AppRoutes.SUNDAY_SCHOOL_MANAGE) },
        confirmSunday = { date -> navController.navigate(AppRoutes.sundayRegister(date)) },
    )

    navigation(
        route            = AppRoutes.ADMIN_GRAPH,
        startDestination = AdminRoutes.ADMIN,
    ) {
        composable(AdminRoutes.ADMIN) {
            AdminScreen(nav = nav())
        }

        composable(AdminRoutes.SCHEDULE) {
            AdminScheduleScreen(
                nav     = nav(),
                onShare = { text ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    navController.context.startActivity(
                        Intent.createChooser(intent, "Compartilhar escala")
                    )
                },
            )
        }

        // Reports is an area of its own inside administration: the hub plus the hymnal history
        // surfaces, nested so they can share one loaded period.
        reportsGraph(navController)

        // Members is its own area too: list, profile, form and history.
        membersGraph(navController)

        // Events too: list and form.
        eventsAdminGraph(navController)

        // "Perfis": app accounts and their access requests.
        profilesGraph(navController)
    }
}
