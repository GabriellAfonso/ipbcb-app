package com.ipb.castelobranco.features.profile.presentation.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ipb.castelobranco.R
import com.ipb.castelobranco.core.presentation.auth.requestGoogleIdToken
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.access.AccessRequestDialog
import com.ipb.castelobranco.core.presentation.components.rememberSquarePhotoPicker
import com.ipb.castelobranco.features.profile.presentation.components.AccessCard
import com.ipb.castelobranco.features.profile.presentation.components.AccountFooter
import com.ipb.castelobranco.features.profile.presentation.components.OwnMemberCard
import com.ipb.castelobranco.features.profile.presentation.components.ChangePasswordDialog
import com.ipb.castelobranco.features.profile.presentation.components.DeleteAccountDialog
import com.ipb.castelobranco.features.profile.presentation.components.EditNameDialog
import com.ipb.castelobranco.features.profile.presentation.components.ProfileOverflowMenu
import com.ipb.castelobranco.features.profile.presentation.components.ProfilePhotoSection
import com.ipb.castelobranco.features.profile.presentation.components.RemovePhotoDialog
import com.ipb.castelobranco.features.profile.presentation.state.ProfileUiEvent
import com.ipb.castelobranco.features.profile.presentation.state.ProfileUiState
import com.ipb.castelobranco.features.profile.presentation.viewmodel.ProfileViewModel
import kotlinx.coroutines.launch
import java.io.File

private const val DEFAULT_USER_NAME = "Usuário"

@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onAccountDeleted: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {

    LaunchedEffect(Unit) { viewModel.initialize() }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentOnAccountDeleted by rememberUpdatedState(onAccountDeleted)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileUiEvent.ShowMessage -> Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                ProfileUiEvent.RequestGoogleIdToken ->
                    scope.launch { viewModel.onGoogleIdToken(requestGoogleIdToken(context)) }
                ProfileUiEvent.AccountDeleted -> currentOnAccountDeleted()
            }
        }
    }

    val pickPhoto = rememberSquarePhotoPicker(
        onPicked = { bytes -> viewModel.uploadProfilePhoto(bytes, "profile.jpg") },
        onCancelled = viewModel::clearError,
    )

    BaseScreen(
        tabName = "Perfil",
        logoRes = R.drawable.ic_profile,
        showBackArrow = true,
        onBackClick = onBackClick,
        extraActions = {
            ProfileOverflowMenu(
                hasPassword = uiState.hasPassword == true,
                onEditName = viewModel::openNameDialog,
                onChangePassword = viewModel::openPasswordDialog,
            )
        },
    ) { innerPadding ->
        ProfileContent(
            state = uiState,
            modifier = Modifier.padding(innerPadding),
            onChangePhoto = pickPhoto,
            onRemovePhoto = viewModel::requestRemovePhoto,
            onConfirmRemovePhoto = viewModel::confirmRemovePhoto,
            onDismissRemovePhoto = viewModel::dismissRemovePhoto,
            onSaveName = viewModel::saveName,
            onDismissName = viewModel::dismissNameDialog,
            onConfirmPassword = viewModel::changePassword,
            onDismissPassword = viewModel::dismissPasswordDialog,
            onDeleteAccount = viewModel::openDeleteDialog,
            onConfirmDelete = viewModel::confirmDelete,
            onDismissDelete = viewModel::dismissDeleteDialog,
            onRequestAccess = viewModel::openAccessRequest,
            onRequestCorrection = viewModel::openCorrectionRequest,
            onSendRequest = viewModel::sendRequest,
            onDismissRequest = viewModel::dismissRequestDialog,
            onRetryOwnMember = viewModel::retryOwnMember,
        )
    }
}

@Composable
fun ProfileContent(
    state: ProfileUiState,
    modifier: Modifier = Modifier,
    onChangePhoto: () -> Unit = {},
    onRemovePhoto: () -> Unit = {},
    onConfirmRemovePhoto: () -> Unit = {},
    onDismissRemovePhoto: () -> Unit = {},
    onSaveName: (String) -> Unit = {},
    onDismissName: () -> Unit = {},
    onConfirmPassword: (String, String, String) -> Unit = { _, _, _ -> },
    onDismissPassword: () -> Unit = {},
    onDeleteAccount: () -> Unit = {},
    onConfirmDelete: (String) -> Unit = {},
    onDismissDelete: () -> Unit = {},
    onRequestAccess: () -> Unit = {},
    onRequestCorrection: () -> Unit = {},
    onSendRequest: (String) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onRetryOwnMember: () -> Unit = {},
) {
    val userName = state.userName?.takeIf { it.isNotBlank() } ?: DEFAULT_USER_NAME
    val photoFile: File? = state.localPhotoPath
        ?.takeIf { it.isNotBlank() }
        ?.let { File(it) }
        ?.takeIf { it.exists() && it.length() > 0L }

    val bitmap = remember(state.localPhotoVersion, photoFile?.absolutePath) {
        photoFile?.absolutePath?.let { BitmapFactory.decodeFile(it) }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))

            ProfilePhotoSection(
                bitmap = bitmap,
                hasPhoto = photoFile != null,
                isBusy = state.isPhotoBusy,
                onChangePhoto = onChangePhoto,
                onRemovePhoto = onRemovePhoto,
            )

            Spacer(Modifier.height(14.dp))

            Text(text = userName, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)

            AccessCard(access = state.access, onRequestAccess = onRequestAccess)

            OwnMemberCard(
                section = state.ownMember,
                access = state.access,
                onRequestCorrection = onRequestCorrection,
                onRetry = onRetryOwnMember,
            )

            if (!state.error.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Erro: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        AccountFooter(
            canDeleteAccount = state.canDeleteAccount,
            joinedSince = state.joinedSince,
            onDeleteAccount = onDeleteAccount,
        )
    }

    if (state.showRemovePhotoConfirm) {
        RemovePhotoDialog(onConfirm = onConfirmRemovePhoto, onDismiss = onDismissRemovePhoto)
    }
    state.nameDialog?.let { dialog ->
        EditNameDialog(
            state = dialog,
            currentName = state.userName.orEmpty(),
            onSave = onSaveName,
            onDismiss = onDismissName,
        )
    }
    state.passwordDialog?.let { dialog ->
        ChangePasswordDialog(state = dialog, onConfirm = onConfirmPassword, onDismiss = onDismissPassword)
    }
    state.deleteDialog?.let { dialog ->
        DeleteAccountDialog(state = dialog, onConfirm = onConfirmDelete, onDismiss = onDismissDelete)
    }
    state.requestDialog?.let { dialog ->
        AccessRequestDialog(state = dialog, onSend = onSendRequest, onDismiss = onDismissRequest)
    }
}
