package com.ipb.castelobranco.features.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.error.AppError
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.profile.domain.model.AccountDeletionProof
import com.ipb.castelobranco.features.profile.domain.usecase.ChangePasswordUseCase
import com.ipb.castelobranco.features.profile.domain.usecase.DeleteAccountUseCase
import com.ipb.castelobranco.features.profile.domain.usecase.FetchProfileUseCase
import com.ipb.castelobranco.features.profile.domain.usecase.RemoveProfilePhotoUseCase
import com.ipb.castelobranco.features.profile.domain.usecase.UpdateProfileNameUseCase
import com.ipb.castelobranco.features.profile.domain.usecase.UploadProfilePhotoUseCase
import com.ipb.castelobranco.features.profile.presentation.format.formatJoinedSince
import com.ipb.castelobranco.features.profile.presentation.state.DeleteDialogState
import com.ipb.castelobranco.features.profile.presentation.state.NameDialogState
import com.ipb.castelobranco.features.profile.presentation.state.PasswordDialogState
import com.ipb.castelobranco.features.profile.presentation.state.ProfileUiEvent
import com.ipb.castelobranco.features.profile.presentation.state.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val fetchProfileUseCase: FetchProfileUseCase,
    private val uploadProfilePhotoUseCase: UploadProfilePhotoUseCase,
    private val removeProfilePhotoUseCase: RemoveProfilePhotoUseCase,
    private val updateProfileNameUseCase: UpdateProfileNameUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val deleteAccountUseCase: DeleteAccountUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<ProfileUiEvent>(extraBufferCapacity = EVENT_BUFFER)
    val events: SharedFlow<ProfileUiEvent> = _events.asSharedFlow()

    fun initialize() {
        observeProfile()
        refreshLocalPhotoPathAndBump()
        refreshFromServer()
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun observeProfile() {
        viewModelScope.launch {
            fetchProfileUseCase.observe().collect { state ->
                when (state) {
                    is SnapshotState.Data -> {
                        val profile = state.value
                        _uiState.update {
                            it.copy(
                                userName = profile.name.trim().ifBlank { "Usuário" },
                                photoUrl = profile.photoUrl,
                                joinedSince = profile.dateJoined?.let(::formatJoinedSince),
                                hasPassword = profile.hasPassword,
                            )
                        }
                        if (!profile.photoUrl.isNullOrBlank()) {
                            fetchProfileUseCase.downloadAndPersistPhoto(profile.photoUrl)
                        }
                        refreshLocalPhotoPathAndBump()
                    }

                    SnapshotState.Loading -> {
                        // opcional: nada ou loading
                    }

                    is SnapshotState.Error -> {
                        _uiState.update {
                            it.copy(error = state.error.toUserMessage())
                        }
                    }
                }
            }
        }
    }

    private fun refreshLocalPhotoPathAndBump() {
        val file = fetchProfileUseCase.getLocalPhoto()
        _uiState.update {
            it.copy(
                localPhotoPath = file?.absolutePath,
                localPhotoVersion = it.localPhotoVersion + 1
            )
        }
    }

    fun refreshFromServer() {
        viewModelScope.launch {
            _uiState.update { it.copy(error = null) }
            try {
                when (fetchProfileUseCase.refresh()) {
                    is RefreshResult.Error -> {
                        _uiState.update { it.copy(error = "Falha ao atualizar perfil") }
                        refreshLocalPhotoPathAndBump()
                    }
                    else -> Unit
                }
            } catch (t: Throwable) {
                Timber.w(t, "Failed to refresh profile from server")
                _uiState.update { it.copy(error = t.toAppError().toUserMessage()) }
                refreshLocalPhotoPathAndBump()
            }
        }
    }

    fun uploadProfilePhoto(bytes: ByteArray, fileName: String = "profile.jpg") {
        viewModelScope.launch {
            if (_uiState.value.isPhotoBusy) return@launch
            _uiState.update { it.copy(isUploading = true, error = null) }

            when (val result = uploadProfilePhotoUseCase(bytes, fileName)) {
                UploadProfilePhotoUseCase.Result.Success -> refreshLocalPhotoPathAndBump()
                is UploadProfilePhotoUseCase.Result.Failure ->
                    _uiState.update { it.copy(error = result.message) }
            }

            _uiState.update { it.copy(isUploading = false) }
        }
    }

    // region photo removal

    fun requestRemovePhoto() {
        if (_uiState.value.isPhotoBusy) return
        _uiState.update { it.copy(showRemovePhotoConfirm = true) }
    }

    fun dismissRemovePhoto() {
        _uiState.update { it.copy(showRemovePhotoConfirm = false) }
    }

    fun confirmRemovePhoto() {
        if (_uiState.value.isPhotoBusy) return
        _uiState.update { it.copy(showRemovePhotoConfirm = false, isRemovingPhoto = true, error = null) }
        viewModelScope.launch {
            removeProfilePhotoUseCase()
                .onSuccess {
                    refreshLocalPhotoPathAndBump()
                    _events.emit(ProfileUiEvent.ShowMessage(PHOTO_REMOVED_MESSAGE))
                }
                .onFailure { error ->
                    Timber.w(error, "Failed to remove profile photo")
                    _uiState.update { it.copy(error = error.toAppError().toUserMessage()) }
                }
            _uiState.update { it.copy(isRemovingPhoto = false) }
        }
    }

    // endregion

    // region name

    fun openNameDialog() {
        _uiState.update { it.copy(nameDialog = NameDialogState()) }
    }

    fun dismissNameDialog() {
        if (_uiState.value.nameDialog?.isSaving == true) return
        _uiState.update { it.copy(nameDialog = null) }
    }

    fun saveName(name: String) {
        val dialog = _uiState.value.nameDialog ?: return
        if (dialog.isSaving) return
        _uiState.update { it.copy(nameDialog = NameDialogState(isSaving = true)) }
        viewModelScope.launch {
            updateProfileNameUseCase(name)
                .onSuccess {
                    _uiState.update { it.copy(nameDialog = null) }
                    _events.emit(ProfileUiEvent.ShowMessage(NAME_UPDATED_MESSAGE))
                }
                .onFailure { throwable ->
                    val error = throwable.toAppError()
                    val nameError = error.fieldError(FIELD_NAME)
                    _uiState.update {
                        it.copy(
                            nameDialog = NameDialogState(
                                nameError = nameError,
                                error = if (nameError == null) error.toUserMessage() else null,
                            )
                        )
                    }
                }
        }
    }

    // endregion

    // region password

    fun openPasswordDialog() {
        if (_uiState.value.hasPassword != true) return
        _uiState.update { it.copy(passwordDialog = PasswordDialogState()) }
    }

    fun dismissPasswordDialog() {
        if (_uiState.value.passwordDialog?.isSaving == true) return
        _uiState.update { it.copy(passwordDialog = null) }
    }

    fun changePassword(current: String, new: String, confirm: String) {
        val dialog = _uiState.value.passwordDialog ?: return
        if (dialog.isSaving) return
        _uiState.update { it.copy(passwordDialog = PasswordDialogState(isSaving = true)) }
        viewModelScope.launch {
            changePasswordUseCase(current = current, new = new, confirm = confirm)
                .onSuccess {
                    _uiState.update { it.copy(passwordDialog = null) }
                    _events.emit(ProfileUiEvent.ShowMessage(PASSWORD_CHANGED_MESSAGE))
                }
                .onFailure { throwable ->
                    val error = throwable.toAppError()
                    val currentError = error.fieldError(FIELD_CURRENT_PASSWORD) ?: error.invalidPasswordMessage()
                    val newError = error.fieldError(FIELD_NEW_PASSWORD)
                    val confirmError = error.fieldError(FIELD_NEW_PASSWORD_CONFIRM)
                    val onAField = currentError != null || newError != null || confirmError != null
                    _uiState.update {
                        it.copy(
                            passwordDialog = PasswordDialogState(
                                currentError = currentError,
                                newError = newError,
                                confirmError = confirmError,
                                error = if (onAField) null else error.toUserMessage(),
                            )
                        )
                    }
                }
        }
    }

    // endregion

    // region account deletion

    fun openDeleteDialog() {
        val hasPassword = _uiState.value.hasPassword ?: return
        _uiState.update { it.copy(deleteDialog = DeleteDialogState(requiresPassword = hasPassword)) }
    }

    fun dismissDeleteDialog() {
        if (_uiState.value.deleteDialog?.isDeleting == true) return
        _uiState.update { it.copy(deleteDialog = null) }
    }

    /** The person typed "EXCLUIR" (and the password, when the account has one) and confirmed. */
    fun confirmDelete(password: String) {
        val dialog = _uiState.value.deleteDialog ?: return
        if (dialog.isDeleting) return
        _uiState.update { it.copy(deleteDialog = dialog.copy(isDeleting = true, passwordError = null, error = null)) }
        if (dialog.requiresPassword) {
            deleteAccount(AccountDeletionProof.Password(password))
        } else {
            viewModelScope.launch { _events.emit(ProfileUiEvent.RequestGoogleIdToken) }
        }
    }

    /** The Google picker answered; `null` means cancelled or failed, which leaves the dialog as it was. */
    fun onGoogleIdToken(idToken: String?) {
        val dialog = _uiState.value.deleteDialog ?: return
        if (idToken.isNullOrBlank()) {
            _uiState.update { it.copy(deleteDialog = dialog.copy(isDeleting = false)) }
            return
        }
        deleteAccount(AccountDeletionProof.Google(idToken))
    }

    private fun deleteAccount(proof: AccountDeletionProof) {
        viewModelScope.launch {
            deleteAccountUseCase(proof)
                .onSuccess {
                    _uiState.update { it.copy(deleteDialog = null) }
                    _events.emit(ProfileUiEvent.AccountDeleted)
                }
                .onFailure { throwable ->
                    Timber.w(throwable, "Account deletion refused")
                    val error = throwable.toAppError()
                    val passwordError = error.fieldError(FIELD_PASSWORD) ?: error.invalidPasswordMessage()
                    _uiState.update { state ->
                        state.copy(
                            deleteDialog = state.deleteDialog?.copy(
                                isDeleting = false,
                                passwordError = passwordError,
                                error = if (passwordError == null) error.toUserMessage() else null,
                            )
                        )
                    }
                }
        }
    }

    // endregion

    private fun AppError.fieldError(field: String): String? =
        (this as? AppError.Server)?.fieldErrors?.get(field)?.firstOrNull()

    /** A wrong password is a form error on the password field, never a session problem. */
    private fun AppError.invalidPasswordMessage(): String? =
        if (this is AppError.Server && errorCode == ERROR_INVALID_PASSWORD) toUserMessage() else null

    private companion object {
        const val EVENT_BUFFER = 8

        const val ERROR_INVALID_PASSWORD = "INVALID_PASSWORD"
        const val FIELD_NAME = "name"
        const val FIELD_CURRENT_PASSWORD = "current_password"
        const val FIELD_NEW_PASSWORD = "new_password"
        const val FIELD_NEW_PASSWORD_CONFIRM = "new_password_confirm"
        const val FIELD_PASSWORD = "password"

        const val PHOTO_REMOVED_MESSAGE = "Foto removida"
        const val NAME_UPDATED_MESSAGE = "Nome atualizado"
        const val PASSWORD_CHANGED_MESSAGE = "Senha alterada"
    }
}
