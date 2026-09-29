package com.ipb.castelobranco.features.profile.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ipb.castelobranco.core.domain.snapshot.RefreshResult
import com.ipb.castelobranco.core.domain.error.toAppError
import com.ipb.castelobranco.core.domain.snapshot.SnapshotState
import com.ipb.castelobranco.core.presentation.error.toUserMessage
import com.ipb.castelobranco.features.profile.domain.usecase.FetchProfileUseCase
import com.ipb.castelobranco.features.profile.domain.usecase.UploadProfilePhotoUseCase
import com.ipb.castelobranco.features.profile.presentation.state.ProfileUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val fetchProfileUseCase: FetchProfileUseCase,
    private val uploadProfilePhotoUseCase: UploadProfilePhotoUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

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
                                isMember = profile.isMember,
                                photoUrl = profile.photoUrl
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
            if (_uiState.value.isUploading) return@launch
            _uiState.update { it.copy(isUploading = true, error = null) }

            when (val result = uploadProfilePhotoUseCase(bytes, fileName)) {
                UploadProfilePhotoUseCase.Result.Success -> refreshLocalPhotoPathAndBump()
                is UploadProfilePhotoUseCase.Result.Failure ->
                    _uiState.update { it.copy(error = result.message) }
            }

            _uiState.update { it.copy(isUploading = false) }
        }
    }
}
