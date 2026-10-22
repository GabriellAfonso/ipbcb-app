package com.ipb.castelobranco.core.presentation.components.access

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.access.AccessPrompt
import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.domain.access.ObserveMemberAccessUseCase
import com.ipb.castelobranco.core.domain.access.RequestAccessUseCase
import com.ipb.castelobranco.core.domain.access.accessPromptFor
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MembersOnlyUiState(
    val prompt: AccessPrompt = AccessPrompt.NONE,
    val dialog: AccessRequestDialogState? = null,
)

/** Notice after a request was accepted by the server. */
const val ACCESS_REQUEST_SENT_MESSAGE = "Pedido enviado"

@HiltViewModel
class MembersOnlyViewModel @Inject constructor(
    observeMemberAccess: ObserveMemberAccessUseCase,
    private val requestAccess: RequestAccessUseCase,
) : ViewModel() {

    private val dialog = MutableStateFlow<AccessRequestDialogState?>(null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    val uiState: StateFlow<MembersOnlyUiState> = combine(observeMemberAccess(), dialog) { access, dialog ->
        MembersOnlyUiState(prompt = accessPromptFor(access), dialog = dialog)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), MembersOnlyUiState())

    fun openDialog() {
        dialog.value = AccessRequestDialogState(type = AccessRequestType.ACCESS)
    }

    fun dismissDialog() {
        if (dialog.value?.isSending == true) return
        dialog.value = null
    }

    fun send(message: String) {
        val current = dialog.value ?: return
        if (current.isSending) return
        dialog.value = current.copy(isSending = true, error = null)
        viewModelScope.launch {
            requestAccess(current.type, message)
                .onSuccess {
                    dialog.value = null
                    _messages.tryEmit(ACCESS_REQUEST_SENT_MESSAGE)
                }
                .onFailure { error ->
                    dialog.update { it?.copy(isSending = false, error = error.toAppError().toUserMessage()) }
                }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
