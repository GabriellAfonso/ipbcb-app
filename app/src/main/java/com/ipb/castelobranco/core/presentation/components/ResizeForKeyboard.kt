package com.ipb.castelobranco.core.presentation.components

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalContext
import com.ipb.castelobranco.core.presentation.base.findActivity

/**
 * For a screen that fits itself above the keyboard (`imePadding`): while it is composed, the window stops panning
 * up when the keyboard opens. Panning on top of the padding pushed the screen too high, with a gap over the keyboard.
 */
@Composable
fun ResizeForKeyboard() {
    val window = LocalContext.current.findActivity()?.window ?: return
    DisposableEffect(window) {
        val previous = window.attributes.softInputMode
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        onDispose { window.setSoftInputMode(previous) }
    }
}
