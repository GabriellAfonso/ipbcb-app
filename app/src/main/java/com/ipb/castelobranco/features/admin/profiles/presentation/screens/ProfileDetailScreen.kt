package com.ipb.castelobranco.features.admin.profiles.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.core.domain.access.AccessRequestType
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.theme.BrandColors
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberAvatar
import com.ipb.castelobranco.features.admin.members.presentation.util.initialsOf
import com.ipb.castelobranco.features.admin.profiles.domain.model.AccessRequest
import com.ipb.castelobranco.features.admin.profiles.domain.model.LinkedMember
import com.ipb.castelobranco.features.admin.profiles.domain.model.ManagedAccount
import com.ipb.castelobranco.features.admin.profiles.domain.model.RequestStatus
import com.ipb.castelobranco.features.admin.profiles.presentation.components.ConfirmActionDialog
import com.ipb.castelobranco.features.admin.profiles.presentation.components.DenyRequestDialog
import com.ipb.castelobranco.features.admin.profiles.presentation.components.MemberPickerDialog
import com.ipb.castelobranco.features.admin.profiles.presentation.components.RequestQuote
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

    val account = state.account
    BaseScreen(
        tabName = "Perfil",
        showBackArrow = true,
        onBackClick = onBack,
        // The green behind the sheet's rounded corners continues the band.
        containerColor = if (account != null) ipbGreen else MaterialTheme.colorScheme.background,
        topBarExtension = account?.let { { AccountBand(it, viewModel.imageLoader) } },
    ) { padding ->
        val contentModifier = if (account != null) {
            Modifier
                .padding(top = padding.calculateTopPadding())
                .clip(RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius))
                .background(MaterialTheme.colorScheme.surfaceDim)
                .padding(bottom = padding.calculateBottomPadding())
        } else {
            Modifier.padding(padding)
        }
        ProfileDetailContent(
            state = state,
            imageLoader = viewModel.imageLoader,
            snackbarHostState = snackbarHostState,
            modifier = contentModifier,
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

/** Pending requests come first: deciding them is why a leader opens the profile. */
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
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
    ) {
        account.pendingRequests.forEach { request ->
            PendingRequestCard(request, state.canManage, enabled, actions)
        }

        Section("Acesso") {
            SettingRow(title = "Acesso de membro", subtitle = accessSubtitle(account)) {
                // A switch over the stored flag, like assigning a role; the ViewModel asks before acting.
                if (state.canManage) {
                    Switch(checked = account.memberAccess, onCheckedChange = actions.onAskSetAccess, enabled = enabled)
                }
            }
            RowDivider()
            SettingRow(
                title = "Bloquear pedidos",
                subtitle = if (account.requestsBlocked) {
                    "A pessoa não consegue pedir de novo"
                } else {
                    "A pessoa pode fazer novos pedidos"
                },
            ) {
                if (state.canManage) {
                    Switch(checked = account.requestsBlocked, onCheckedChange = actions.onSetBlocked, enabled = enabled)
                }
            }
        }

        Section("Rol") {
            MemberRow(account.member, imageLoader, state.canManage, enabled, actions)
        }

        val history = account.requests.filter { it.status != RequestStatus.PENDING }
        if (history.isNotEmpty()) {
            Section("Histórico") {
                history.forEachIndexed { index, request ->
                    if (index > 0) RowDivider()
                    HistoryEntry(request)
                }
            }
        }
    }
}

/** Avatar, name, "@login · no app desde …" inside the green top-bar block. */
@Composable
private fun AccountBand(account: ManagedAccount, imageLoader: ImageLoader?) {
    val subtitle = listOfNotNull(
        account.username?.let { "@$it" },
        account.dateJoined?.let { joinedSinceLabel(it).replaceFirstChar(Char::lowercase) },
    ).joinToString(" · ")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 18.dp),
    ) {
        MemberAvatar(
            initials = initialsOf(account.name),
            photoUrl = account.photoUrl,
            imageLoader = imageLoader,
            initialsSize = 20.sp,
            modifier = Modifier
                .size(60.dp)
                .border(2.dp, Color.White.copy(alpha = BAND_BORDER_ALPHA), CircleShape),
        )
        Column {
            Text(account.name, style = MaterialTheme.typography.titleLarge, color = Color.White)
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = BAND_SUBTITLE_ALPHA),
                )
            }
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
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(CardRadius))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(2.dp, BrandColors.Orange, RoundedCornerShape(CardRadius))
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Filled.Schedule, contentDescription = null, tint = PendingInk, modifier = Modifier.size(20.dp))
            Text(
                text = "${requestTypeLabel(request.type)} aguardando".uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = PendingInk,
            )
        }
        request.message?.let { RequestQuote(it) }
        request.createdAt?.let { Note(formatDateTime(it)) }
        if (canManage) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                OutlinedButton(
                    onClick = { actions.onOpenDeny(request.id) },
                    enabled = enabled,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f),
                ) { Text("Negar") }
                Button(
                    onClick = { actions.onApprove(request.id) },
                    enabled = enabled,
                    colors = ButtonDefaults.buttonColors(containerColor = ipbGreen, contentColor = Color.White),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (request.type == AccessRequestType.ACCESS) "Liberar acesso" else "Marcar resolvida")
                }
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: LinkedMember?,
    imageLoader: ImageLoader?,
    canManage: Boolean,
    enabled: Boolean,
    actions: ProfileDetailActions,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
    ) {
        MemberAvatar(
            initials = member?.name?.let(::initialsOf) ?: "?",
            photoUrl = null,
            imageLoader = imageLoader,
            initialsSize = 12.sp,
            modifier = Modifier.size(36.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(member?.name ?: "Sem vínculo", style = MaterialTheme.typography.bodyLarge)
            Note(if (member != null) "Membro do rol vinculado a esta conta" else "Ligue a conta a um membro do rol")
        }
        if (canManage) {
            TextButton(onClick = actions.onOpenPicker, enabled = enabled) {
                Text(if (member == null) "Vincular" else "Trocar")
            }
            if (member != null) {
                TextButton(
                    onClick = actions.onAskUnlink,
                    enabled = enabled,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Remover") }
            }
        }
    }
}

/** One step of the timeline: green dot when approved/resolved, red when denied. */
@Composable
private fun HistoryEntry(request: AccessRequest) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        val dotColor = if (request.status == RequestStatus.DENIED) MaterialTheme.colorScheme.error else ipbGreen
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(10.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                "${requestTypeLabel(request.type)} · ${requestStatusLabel(request.type, request.status)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            request.message?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            val decided = listOfNotNull(request.decidedBy, request.decidedAt?.let(::formatDateTime))
                .joinToString(" · ")
            if (decided.isNotEmpty()) Note(decided)
            request.denialReason?.let { Note("Motivo: $it") }
        }
    }
}

/** "Liberado por …" when known, plus "Acesso pelo papel" when a role grants it anyway. */
private fun accessSubtitle(account: ManagedAccount): String {
    val stored = if (account.memberAccess) {
        grantedLabel(account.accessGrantedBy, account.accessGrantedAt) ?: "Liberado"
    } else {
        "Não liberado"
    }
    return if (account.accessViaRole) "$stored · Acesso pelo papel" else stored
}

@Composable
private fun SettingRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Note(subtitle)
        }
        trailing()
    }
}

/** A title with a green bar, over a card holding [content]. */
@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(width = 4.dp, height = 15.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CardRadius))
                .background(MaterialTheme.colorScheme.surfaceContainer),
        ) { content() }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = DIVIDER_ALPHA))
}

@Composable
private fun Note(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

private val SheetRadius = 18.dp
private val CardRadius = 18.dp
private val PendingInk = Color(0xFF9A5B00)
private const val BAND_BORDER_ALPHA = 0.6f
private const val BAND_SUBTITLE_ALPHA = 0.82f
private const val DIVIDER_ALPHA = 0.6f

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
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceDim),
        )
    }
}
