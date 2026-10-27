package com.ipb.castelobranco.features.studies.presentation.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.studies.presentation.screens.SundaySchoolLessonFormScreen
import com.ipb.castelobranco.features.studies.presentation.screens.SundaySchoolManageScreen
import com.ipb.castelobranco.features.studies.presentation.screens.SundaySchoolYearsScreen

/**
 * EBD management (`specs/studies/spec.md` §5): opened from the admin panel card through
 * [AppRoutes.SUNDAY_SCHOOL_MANAGE]. Lives in the studies feature so the preview uses the reader's renderer.
 */
fun NavGraphBuilder.sundaySchoolManagement(navController: NavHostController) {
    composable(AppRoutes.SUNDAY_SCHOOL_MANAGE) {
        SundaySchoolManageScreen(
            viewModel = hiltViewModel(),
            onBackClick = { navController.safePopBackStack() },
            onOpenLesson = { id, year -> navController.navigate(StudiesRoutes.sundaySchoolLessonForm(id, year)) },
            onOpenYears = { navController.navigate(StudiesRoutes.SundaySchoolYears) },
        )
    }

    composable(
        route = StudiesRoutes.SundaySchoolLessonForm,
        arguments = listOf(
            navArgument(StudiesRoutes.ARG_LESSON_ID) {
                type = NavType.IntType
                defaultValue = StudiesRoutes.NEW_LESSON
            },
            navArgument(StudiesRoutes.ARG_YEAR) {
                type = NavType.IntType
                defaultValue = 0
            },
        ),
    ) {
        SundaySchoolLessonFormScreen(
            viewModel = hiltViewModel(),
            onClose = { navController.safePopBackStack() },
        )
    }

    composable(StudiesRoutes.SundaySchoolYears) {
        SundaySchoolYearsScreen(
            viewModel = hiltViewModel(),
            onBackClick = { navController.safePopBackStack() },
        )
    }
}
