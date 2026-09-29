package com.ipb.castelobranco.features.profile.presentation.state

data class ProfileUiState(
    val isUploading: Boolean = false,
    val photoUrl: String? = null,
    val error: String? = null,
    val localPhotoPath: String? = null,
    val localPhotoVersion: Int = 0,
    val userName: String? = null,
    val isMember: Boolean? = null,
)
