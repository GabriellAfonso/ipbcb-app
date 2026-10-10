package com.ipb.castelobranco.features.admin.members.presentation.state

import com.ipb.castelobranco.features.admin.members.domain.model.HistoryLine

data class MemberProfileUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val profile: MemberProfileUi? = null,
    val lastChange: HistoryLine? = null,
    val isPhotoBusy: Boolean = false,
    val showRemovePhotoDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val deleteTyped: String = "",
    val canConfirmDelete: Boolean = false,
    val isDeleting: Boolean = false,
    /** Edit (validity included): `manage` on `members`. */
    val canEdit: Boolean = false,
    val canChangePhoto: Boolean = false,
    /** Delete and photo removal: `owner` on `members` (Admin only). */
    val canDelete: Boolean = false,
    val canRemovePhoto: Boolean = false,
    /** "Baixar" in the full-screen photo: `manage` on `members`. */
    val canDownloadPhoto: Boolean = false,
    val isDownloadingPhoto: Boolean = false,
    /** Bumps when the device copy of the photo changed, so it loads again. */
    val photoRevision: Int = 0,
)

/** Every text already formatted; the screen only lays it out. */
data class MemberProfileUi(
    val id: Int,
    val name: String,
    val initials: String,
    val photoUrl: String?,
    val headline: String,
    val statusLabel: String,
    val roleLabel: String?,
    val firstName: String,
    val lastName: String,
    val birthDateLabel: String,
    val ageLabel: String,
    val genderLabel: String,
    val roleText: String,
    val baptismLabel: String,
    val ministries: List<String>,
    val createdAtLabel: String,
    val isValid: Boolean,
)
