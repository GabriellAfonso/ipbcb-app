package com.ipb.castelobranco.features.admin.profiles.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberAvatar
import com.ipb.castelobranco.features.admin.members.presentation.util.initialsOf
import com.ipb.castelobranco.features.admin.profiles.domain.model.AccessRequest
import com.ipb.castelobranco.features.admin.profiles.domain.model.LinkedMember
import com.ipb.castelobranco.features.admin.profiles.domain.model.ManagedAccount
import com.ipb.castelobranco.features.admin.profiles.domain.model.RequestStatus
import com.ipb.castelobranco.features.admin.profiles.presentation.components.ConfirmActionDialog
import com.ipb.castelobranco.features.admin.profiles.presentation.components.DenyRequestDialog
import com.ipb.castelobranco.features.admin.profiles.presentation.components.MemberPickerDialog
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ProfileDetailUiState
import com.ipb.castelobranco.features.admin.profiles.presentation.state.ProfilesEvent
import com.ipb.castelobranco.features.admin.profiles.presentation.util.formatDateTime
import com.ipb.castelobranco.features.admin.profiles.presentation.util.grantedLabel
import com.ipb.castelobranco.features.admin.profiles.presentation.util.joinedSinceLabel
import com.ipb.castelobranco.features.admin.profiles.presentation.util.requestStatusLabel
import com.ipb.castelobranco.features.admin.profiles.presentation.util.requestTypeLabel
import com.ipb.castelobranco.features.admin.profiles.presentation.viewmodel.ProfileDetailViewModel
import java.time.LocalDate

/** Every action the detail can ask for; all of them come from the ViewModel. */
data class ProfileDetailActions(
    val onRetry: () -> Unit = {},
    val onApprove: (Long) -> Unit = {},
    val onOpenDeny: (Long) -> Unit = {},
    val onDeny: (String) -> Unit = {},
    val onDismissDeny: () -> Unit = {},
    val onAskSetAccess: (Boolean) -> Unit = {},
    val onAskUnlink: () -> Unit = {},
    val onConfirm: () -> Unit = {},
    val onDismissConfirm: () -> Unit = {},
    val onSetBlocked: (Boolean) -> Unit = {},
    val onOpenPicker: () -> Unit = {},
    val onPickerQuery: (String) -> Unit = {},
    val onPick: (Int) -> Unit = {},
    val onDismissPicker: () -> Unit = {},
)

@Composable
fun ProfileDetailScreen(
    viewModel: ProfileDetailViewModel,
    onBack: () -> Unit,
    onLeaveArea: (String) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfilesEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is ProfilesEvent.LeaveArea -> onLeaveArea(event.message)
            }
        }
    }

    BaseScreen(tabName = state.account?.name ?: "Perfil", showBackArrow = true, onBackClick = onBack) { padding ->
        ProfileDetailContent(
            state = state,
            imageLoader = viewModel.imageLoader,
            snackbarHostState = snackbarHostState,
            modifier = Modifier.padding(padding),
            actions = ProfileDetailActions(
                onRetry = viewModel::load,
                onApprove = viewModel::approve,
                onOpenDeny = viewModel::openDeny,
                onDeny = viewModel::deny,
                onDismissDeny = viewModel::dismissDeny,
                onAskSetAccess = viewModel::askSetAccess,
                onAskUnlink = viewModel::askUnlink,
                onConfirm = viewModel::confirm,
                onDismissConfirm = viewModel::dismissConfirm,
                onSetBlocked = viewModel::setBlocked,
                onOpenPicker = viewModel::openPicker,
                onPickerQuery = viewModel::onPickerQuery,
                onPick = viewModel::pick,
                onDismissPicker = viewModel::dismissPicker,
            ),
        )
    }
}

@Composable
fun ProfileDetailContent(
    state: ProfileDetailUiState,
    imageLoader: ImageLoader?,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    actions: ProfileDetailActions = ProfileDetailActions(),
) {
    Box(modifier = modifier.fillMaxSize()) {
        val account = state.account
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            account == null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            ) {
                Text(state.error.orEmpty(), textAlign = TextAlign.Center)
                Button(onClick = actions.onRetry) { Text("Tentar novamente") }
            }
            else -> AccountDetail(account, state, imageLoader, actions)
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }

    val account = state.account ?: return
    if (state.denyDialog != null) {
        DenyRequestDialog(isBusy = state.isBusy, onDeny = actions.onDeny, onDismiss = actions.onDismissDeny)
    }
    state.confirm?.let { action ->
        ConfirmActionDialog(
            action = action,
            accountName = account.name,
            accessViaRole = account.accessViaRole,
            isBusy = state.isBusy,
            onConfirm = actions.onConfirm,
            onDismiss = actions.onDismissConfirm,
        )
    }
    state.picker?.let { picker ->
        MemberPickerDialog(
            state = picker,
            isBusy = state.isBusy,
            onQueryChange = actions.onPickerQuery,
            onPick = actions.onPick,
            onDismiss = actions.onDismissPicker,
        )
    }
}

@Composable
private fun AccountDetail(
    account: ManagedAccount,
    state: ProfileDetailUiState,
    imageLoader: ImageLoader?,
    actions: ProfileDetailActions,
) {
    val enabled = !state.isBusy
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Header(account, imageLoader)

        Section("Acesso") {
            if (state.canManage) {
                // A switch over the stored flag, like assigning a role; the ViewModel asks before acting.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Acesso de membro", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Switch(checked = account.memberAccess, onCheckedChange = actions.onAskSetAccess, enabled = enabled)
                }
            } else {
                val status = if (account.memberAccess) "Liberado" else "Não liberado"
                Text(status, style = MaterialTheme.typography.bodyLarge)
            }
            if (account.memberAccess) {
                grantedLabel(account.accessGrantedBy, account.accessGrantedAt)?.let { Note(it) }
            }
            if (account.accessViaRole) Note("Acesso pelo papel")
            if (state.canManage) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Bloquear pedidos", modifier = Modifier.weight(1f))
                    Switch(checked = account.requestsBlocked, onCheckedChange = actions.onSetBlocked, enabled = enabled)
                }
            } else if (account.requestsBlocked) {
                Note("Pedidos bloqueados")
            }
        }

        Section("Rol") {
            Text(account.member?.name ?: "Sem vínculo", style = MaterialTheme.typography.bodyLarge)
            if (state.canManage) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = actions.onOpenPicker, enabled = enabled) {
                        Text(if (account.member == null) "Vincular ao rol" else "Trocar vínculo")
                    }
                    if (account.member != null) {
                        TextButton(onClick = actions.onAskUnlink, enabled = enabled) { Text("Remover vínculo") }
                    }
                }
            }
        }

        if (account.pendingRequests.isNotEmpty()) {
            Section("Pendentes") {
                account.pendingRequests.forEach { request ->
                    PendingRequestCard(request, state.canManage, enabled, actions)
                }
            }
        }

        val history = account.requests.filter { it.status != RequestStatus.PENDING }
        if (history.isNotEmpty()) {
            Section("Histórico") {
                history.forEachIndexed { index, request ->
                    if (index > 0) HorizontalDivider()
                    HistoryEntry(request)
                }
            }
        }
    }
}

@Composable
private fun Header(account: ManagedAccount, imageLoader: ImageLoader?) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        MemberAvatar(
            initials = initialsOf(account.name),
            photoUrl = account.photoUrl,
            imageLoader = imageLoader,
            modifier = Modifier.size(72.dp),
        )
        Column {
            Text(account.name, style = MaterialTheme.typography.titleLarge)
            account.username?.let { Note("@$it") }
            account.dateJoined?.let { Note(joinedSinceLabel(it)) }
        }
    }
}

@Composable
private fun PendingRequestCard(
    request: AccessRequest,
    canManage: Boolean,
    enabled: Boolean,
    actions: ProfileDetailActions,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(requestTypeLabel(request.type), style = MaterialTheme.typography.titleSmall)
            request.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            request.createdAt?.let { Note(formatDateTime(it)) }
            if (canManage) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { actions.onApprove(request.id) }, enabled = enabled) { Text("Aprovar") }
                    OutlinedButton(onClick = { actions.onOpenDeny(request.id) }, enabled = enabled) { Text("Negar") }
                }
            }
        }
    }
}

@Composable
private fun HistoryEntry(request: AccessRequest) {
    Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "${requestTypeLabel(request.type)} · ${requestStatusLabel(request.type, request.status)}",
            style = MaterialTheme.typography.bodyLarge,
        )
        request.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        val decided = listOfNotNull(request.decidedBy, request.decidedAt?.let(::formatDateTime)).joinToString(" · ")
        if (decided.isNotEmpty()) Note(decided)
        request.denialReason?.let { Note("Motivo: $it") }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ProfileDetailContentPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        ProfileDetailContent(
            state = ProfileDetailUiState(
                isLoading = false,
                canManage = true,
                account = ManagedAccount(
                    id = "1",
                    name = "Ana Souza",
                    username = "ana",
                    photoUrl = null,
                    memberAccess = false,
                    accessViaRole = false,
                    accessGrantedBy = null,
                    accessGrantedAt = null,
                    requestsBlocked = false,
                    member = LinkedMember(12, "Ana Souza"),
                    dateJoined = LocalDate.of(2025, 3, 14),
                    pendingRequests = listOf(
                        AccessRequest(41, AccessRequestType.ACCESS, "Sou membro desde 2010.", RequestStatus.PENDING,
                            null, null, null, null),
                    ),
                    requests = emptyList(),
                ),
            ),
            imageLoader = null,
            snackbarHostState = remember { SnackbarHostState() },
        )
    }
}
