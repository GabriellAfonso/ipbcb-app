package com.ipb.castelobranco.features.admin.panel.presentation.navigation

import android.content.Intent
import androidx.compose.runtime.Stable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.admin.panel.presentation.screens.AdminScreen
import com.ipb.castelobranco.features.admin.members.presentation.navigation.MembersRoutes
import com.ipb.castelobranco.features.admin.members.presentation.navigation.membersGraph
import com.ipb.castelobranco.features.admin.register.presentation.screens.MusicRegistrationScreen
import com.ipb.castelobranco.features.admin.reports.presentation.navigation.ReportsRoutes
import com.ipb.castelobranco.features.admin.reports.presentation.navigation.reportsGraph
import com.ipb.castelobranco.features.admin.schedule.presentation.screens.AdminScheduleScreen
import java.time.LocalDate

@Stable
data class AdminNav(
    val back: () -> Unit,
    val register: () -> Unit,
    val schedule: () -> Unit,
    val reports: () -> Unit,
    val members: () -> Unit,
    val confirmSunday: (LocalDate) -> Unit = {},
)

object AdminRoutes {
    const val ADMIN    = "AdminMain"
    const val REGISTER = "AdminRegister"
    const val SCHEDULE = "AdminSchedule"

    /** Optional `YYYY-MM-DD`: opens "Registrar domingo" pre-filled with that Sunday's setlist. */
    const val ARG_DATE = "date"
    const val REGISTER_ROUTE = "$REGISTER?$ARG_DATE={$ARG_DATE}"

    fun register(date: LocalDate) = "$REGISTER?$ARG_DATE=$date"
}

/**
 * Opens the pre-filled register screen from outside administration (a notification tap). Goes through
 * the panel so back lands there.
 */
fun NavController.navigateToSundayConfirmation(date: LocalDate) {
    navigate(AppRoutes.ADMIN_GRAPH)
    navigate(AdminRoutes.register(date))
}

fun NavGraphBuilder.adminGraph(navController: NavHostController) {
    fun nav() = AdminNav(
        back     = { navController.safePopBackStack() },
        register = { navController.navigate(AdminRoutes.REGISTER) },
        schedule = { navController.navigate(AdminRoutes.SCHEDULE) },
        reports  = { navController.navigate(ReportsRoutes.GRAPH) },
        members  = { navController.navigate(MembersRoutes.GRAPH) },
        confirmSunday = { date -> navController.navigate(AdminRoutes.register(date)) },
    )

    navigation(
        route            = AppRoutes.ADMIN_GRAPH,
        startDestination = AdminRoutes.ADMIN,
    ) {
        composable(AdminRoutes.ADMIN) {
            AdminScreen(nav = nav())
        }

        composable(
            route = AdminRoutes.REGISTER_ROUTE,
            arguments = listOf(
                navArgument(AdminRoutes.ARG_DATE) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) {
            MusicRegistrationScreen(nav())
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
    }
}
