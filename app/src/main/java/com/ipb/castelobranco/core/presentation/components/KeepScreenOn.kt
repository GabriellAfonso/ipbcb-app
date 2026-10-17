package com.ipb.castelobranco.core.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.platform.LocalView

/** The user's "Manter tela acesa" preference, provided by `AppNavHost`. */
val LocalKeepScreenOn = compositionLocalOf { true }

/** Keeps the display awake while the calling reading screen is composed and the preference is on. */
@Composable
fun KeepScreenOn() {
    val enabled = LocalKeepScreenOn.current
    val view = LocalView.current
    DisposableEffect(view, enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
