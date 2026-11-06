package com.ipb.castelobranco.core.presentation.components

import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest

/** A short notice for the whole app; [long] keeps it on screen longer. */
data class AppMessage(val text: String, val long: Boolean = false)

/**
 * The app's notices (`specs/core/spec.md`): any screen or nav graph calls [show], and the one [AppMessageHost] in
 * `CoreActivity` shows it. Since the host sits above the navigation, a notice survives the screen that sent it
 * closing (e.g. "saved", then back). Screens with their own Snackbar (an undo action) keep it.
 */
object AppMessages {
    private val _messages = MutableSharedFlow<AppMessage>(
        extraBufferCapacity = BUFFER,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<AppMessage> = _messages.asSharedFlow()

    fun show(text: String, long: Boolean = false) {
        _messages.tryEmit(AppMessage(text, long))
    }

    private const val BUFFER = 8
}

/** Above the navigation bar, the keyboard and the screens' floating buttons. A new notice replaces the shown one. */
@Composable
fun AppMessageHost(modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    LaunchedEffect(hostState) {
        AppMessages.messages.collectLatest { message ->
            hostState.showSnackbar(
                message = message.text,
                duration = if (message.long) SnackbarDuration.Long else SnackbarDuration.Short,
            )
        }
    }
    SnackbarHost(
        hostState = hostState,
        modifier = modifier
            .navigationBarsPadding()
            .imePadding()
            .padding(bottom = ABOVE_FAB),
    )
}

/** A 56dp floating button plus its 16dp margin, and a little air. */
private val ABOVE_FAB = 80.dp

@Preview
@Composable
private fun AppMessagePreview() {
    IPBCasteloBrancoTheme(dynamicColor = false) {
        Snackbar { Text("Sessão iniciada") }
    }
}
