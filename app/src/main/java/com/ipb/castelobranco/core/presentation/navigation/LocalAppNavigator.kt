package com.ipb.castelobranco.core.presentation.navigation

import androidx.compose.runtime.staticCompositionLocalOf

data class AppNavigator(
    val navigateToProfile: () -> Unit,
    val navigateToAuth: () -> Unit,
)

val LocalAppNavigator = staticCompositionLocalOf<AppNavigator> {
    error("No AppNavigator provided — wrap your content in AppNavHost")
}
