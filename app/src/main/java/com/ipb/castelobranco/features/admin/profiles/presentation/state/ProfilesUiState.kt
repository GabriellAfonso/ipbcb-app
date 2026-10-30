package com.ipb.castelobranco.features.admin.profiles.presentation.state

import com.ipb.castelobranco.features.admin.profiles.domain.model.ManagedAccount
import com.ipb.castelobranco.features.admin.profiles.domain.model.ProfilesTab

data class ProfilesListUiState(
    val tab: ProfilesTab = ProfilesTab.PENDING,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val rows: List<ProfileRowUi> = emptyList(),
    /** Badge on "Pendentes"; `null` until that list loads. */
    val pendingCount: Int? = null,
    /** "Todos · {n}"; `null` until that list loads. */
    val allCount: Int? = null,
)

data class ProfileRowUi(
    val id: String,
    val name: String,
    val initials: String,
    val photoUrl: String?,
    /** "Pedido de acesso" / "Pedido de correção" for the oldest pending request. */
    val pendingLabel: String?,
    val isBlocked: Boolean,
    val username: String? = null,
    val hasAccess: Boolean = false,
    val isCorrection: Boolean = false,
    /** The oldest pending request's message and "dd/MM/yyyy às HH:mm"; shown on the "Pendentes" tab. */
    val pendingMessage: String? = null,
    val pendingAt: String? = null,
)

data class ProfileDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val account: ManagedAccount? = null,
    /** `manage` on `profiles`: every action; otherwise read only. */
    val canManage: Boolean = false,
    /** An action is running: the buttons wait. */
    val isBusy: Boolean = false,
    val denyDialog: DenyDialogState? = null,
    val confirm: ConfirmAction? = null,
    val picker: MemberPickerState? = null,
)

data class DenyDialogState(val requestId: Long)

enum class ConfirmAction { GRANT, REVOKE, UNLINK }

data class MemberPickerState(
    val query: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val members: List<MemberOptionUi> = emptyList(),
)

data class MemberOptionUi(val id: Int, val name: String)

/** One-shot events of the "Perfis" screens. */
sealed interface ProfilesEvent {
    data class ShowMessage(val message: String) : ProfilesEvent

    /** The server refused a read (403): leave the area. */
    data class LeaveArea(val message: String) : ProfilesEvent
}
