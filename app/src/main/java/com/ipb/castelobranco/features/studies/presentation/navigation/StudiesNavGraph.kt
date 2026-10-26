package com.ipb.castelobranco.features.studies.presentation.navigation

import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.ipb.castelobranco.core.presentation.navigation.AppRoutes
import com.ipb.castelobranco.core.presentation.navigation.safePopBackStack
import com.ipb.castelobranco.features.studies.domain.model.BibleReference
import com.ipb.castelobranco.features.studies.domain.model.DoctrineKind
import com.ipb.castelobranco.features.studies.presentation.screens.CatechismScreen
import com.ipb.castelobranco.features.studies.presentation.screens.ConfessionChapterScreen
import com.ipb.castelobranco.features.studies.presentation.screens.ConfessionScreen
import com.ipb.castelobranco.features.studies.presentation.screens.DoctrineListScreen
import com.ipb.castelobranco.features.studies.presentation.screens.StudiesScreen

object StudiesRoutes {
    const val Hub = "StudiesHub"
    const val DoctrineList = "DoctrineList"

    const val ARG_SLUG = "slug"
    const val ARG_CHAPTER = "chapter"
    const val ARG_PARAGRAPH = "paragraph"

    const val Catechism = "DoctrineCatechism/{$ARG_SLUG}"
    const val Confession = "DoctrineConfession/{$ARG_SLUG}"
    const val ConfessionChapter = "DoctrineConfessionChapter/{$ARG_SLUG}/{$ARG_CHAPTER}?$ARG_PARAGRAPH={$ARG_PARAGRAPH}"

    fun catechism(slug: String) = "DoctrineCatechism/$slug"
    fun confession(slug: String) = "DoctrineConfession/$slug"
    fun confessionChapter(slug: String, chapter: Int, paragraph: Int = 0) =
        "DoctrineConfessionChapter/$slug/$chapter?$ARG_PARAGRAPH=$paragraph"
}

fun NavGraphBuilder.studiesGraph(navController: NavHostController) {
    val openBible: (BibleReference) -> Unit = { reference ->
        val highlight = reference.verses.map { it.start..it.end }
        navController.navigate(
            AppRoutes.biblePassage(reference.book, reference.chapter, reference.firstVerse, highlight),
        )
    }

    navigation(
        route            = AppRoutes.STUDIES_GRAPH,
        startDestination = StudiesRoutes.Hub,
    ) {
        composable(StudiesRoutes.Hub) {
            StudiesScreen(
                onBackClick = { navController.safePopBackStack() },
                onOpenDoctrine = { navController.navigate(StudiesRoutes.DoctrineList) },
            )
        }

        composable(StudiesRoutes.DoctrineList) {
            DoctrineListScreen(
                viewModel = hiltViewModel(),
                onBackClick = { navController.safePopBackStack() },
                onOpenDocument = { document ->
                    val route = when (document.kind) {
                        DoctrineKind.CATECHISM -> StudiesRoutes.catechism(document.slug)
                        DoctrineKind.CONFESSION -> StudiesRoutes.confession(document.slug)
                    }
                    navController.navigate(route)
                },
            )
        }

        composable(
            route = StudiesRoutes.Catechism,
            arguments = listOf(navArgument(StudiesRoutes.ARG_SLUG) { type = NavType.StringType }),
        ) {
            CatechismScreen(
                viewModel = hiltViewModel(),
                onBackClick = { navController.safePopBackStack() },
                onReferenceClick = openBible,
            )
        }

        composable(
            route = StudiesRoutes.Confession,
            arguments = listOf(navArgument(StudiesRoutes.ARG_SLUG) { type = NavType.StringType }),
        ) { entry ->
            val slug = entry.arguments?.getString(StudiesRoutes.ARG_SLUG).orEmpty()
            ConfessionScreen(
                viewModel = hiltViewModel(),
                onBackClick = { navController.safePopBackStack() },
                onOpenChapter = { chapter, paragraph ->
                    navController.navigate(StudiesRoutes.confessionChapter(slug, chapter, paragraph))
                },
            )
        }

        composable(
            route = StudiesRoutes.ConfessionChapter,
            arguments = listOf(
                navArgument(StudiesRoutes.ARG_SLUG) { type = NavType.StringType },
                navArgument(StudiesRoutes.ARG_CHAPTER) { type = NavType.IntType },
                navArgument(StudiesRoutes.ARG_PARAGRAPH) {
                    type = NavType.IntType
                    defaultValue = 0
                },
            ),
        ) {
            ConfessionChapterScreen(
                viewModel = hiltViewModel(),
                onBackClick = { navController.safePopBackStack() },
            )
        }
    }
}
