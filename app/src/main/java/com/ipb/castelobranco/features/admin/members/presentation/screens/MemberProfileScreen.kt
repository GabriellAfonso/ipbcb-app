package com.ipb.castelobranco.features.admin.members.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionOnScreen
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.rememberSquarePhotoPicker
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.features.admin.members.presentation.components.DeleteMemberDialog
import com.ipb.castelobranco.features.admin.members.presentation.components.HistoryCard
import com.ipb.castelobranco.features.admin.members.presentation.components.InfoRow
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberAvatar
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberPhotoPreview
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberPhotoViewer
import com.ipb.castelobranco.features.admin.members.presentation.components.MinistriesRow
import com.ipb.castelobranco.features.admin.members.presentation.components.SectionCard
import com.ipb.castelobranco.features.admin.members.presentation.components.StatusChip
import com.ipb.castelobranco.features.admin.members.presentation.components.Tag
import com.ipb.castelobranco.features.admin.members.presentation.components.ValidityCard
import com.ipb.castelobranco.features.admin.members.presentation.components.rememberStoragePermissionGate
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberProfileUi
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberProfileUiState
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.presentation.viewmodel.MemberProfileViewModel

/** What the profile screen can ask for; grouped so the content signature stays readable. */
data class MemberProfileActions(
    val onRetry: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onPickPhoto: () -> Unit = {},
    val onRemovePhotoRequested: () -> Unit = {},
    val onRemovePhotoConfirmed: () -> Unit = {},
    val onRemovePhotoDismissed: () -> Unit = {},
    val onDownloadPhoto: () -> Unit = {},
    val onOpenHistory: () -> Unit = {},
    val onDeleteRequested: () -> Unit = {},
    val onDeleteTypedChange: (String) -> Unit = {},
    val onDeleteConfirmed: () -> Unit = {},
    val onDeleteDismissed: () -> Unit = {},
)

@Composable
fun MemberProfileScreen(
    viewModel: MemberProfileViewModel,
    onBack: () -> Unit,
    onEdit: (Int) -> Unit,
    onOpenHistory: (Int) -> Unit,
    onNavigationEvent: (MembersEvent) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val pickPhoto = rememberSquarePhotoPicker(onPicked = viewModel::onPhotoPicked)
    val downloadPhoto = rememberStoragePermissionGate(
        onGranted = viewModel::onDownloadPhoto,
        onDenied = { message -> snackbarHostState.showSnackbar(message) },
    )

    // Coming back from the form shows the edit at once.
    LifecycleResumeEffect(Unit) {
        viewModel.load()
        onPauseOrDispose {}
    }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MembersEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> onNavigationEvent(event)
            }
        }
    }

    val memberId = state.profile?.id
    BaseScreen(
        tabName = "Membro",
        showBackArrow = true,
        onBackClick = onBack,
        extraActions = {
            if (memberId != null && state.canEdit) {
                IconButton(onClick = { onEdit(memberId) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Editar membro")
                }
            }
        },
    ) { innerPadding ->
        Box(Modifier.padding(innerPadding)) {
            MemberProfileContent(
                state = state,
                imageLoader = viewModel.imageLoader,
                actions = MemberProfileActions(
                    onRetry = viewModel::load,
                    onEdit = { memberId?.let(onEdit) },
                    onPickPhoto = pickPhoto,
                    onRemovePhotoRequested = viewModel::onRemovePhotoRequested,
                    onDownloadPhoto = downloadPhoto,
                    onRemovePhotoConfirmed = viewModel::onRemovePhotoConfirmed,
                    onRemovePhotoDismissed = viewModel::onRemovePhotoDismissed,
                    onOpenHistory = { memberId?.let(onOpenHistory) },
                    onDeleteRequested = viewModel::onDeleteRequested,
                    onDeleteTypedChange = viewModel::onDeleteTypedChange,
                    onDeleteConfirmed = viewModel::onDeleteConfirmed,
                    onDeleteDismissed = viewModel::onDeleteDismissed,
                ),
            )
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
fun MemberProfileContent(
    state: MemberProfileUiState,
    imageLoader: ImageLoader?,
    actions: MemberProfileActions,
) {
    val profile = state.profile
    Box(Modifier.fillMaxSize()) {
        when {
            profile != null -> ProfileBody(state, profile, imageLoader, actions)
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            else -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Text(state.error.orEmpty(), textAlign = TextAlign.Center)
                Button(onClick = actions.onRetry) { Text("Tentar novamente") }
            }
        }
    }

    if (profile != null && state.showRemovePhotoDialog) {
        AlertDialog(
            onDismissRequest = actions.onRemovePhotoDismissed,
            title = { Text("Remover foto") },
            text = { Text("A foto de ${profile.name} será removida.") },
            confirmButton = { TextButton(onClick = actions.onRemovePhotoConfirmed) { Text("Remover") } },
            dismissButton = { TextButton(onClick = actions.onRemovePhotoDismissed) { Text("Cancelar") } },
        )
    }
    if (profile != null && state.showDeleteDialog) {
        DeleteMemberDialog(
            memberName = profile.name,
            typed = state.deleteTyped,
            canConfirm = state.canConfirmDelete,
            isDeleting = state.isDeleting,
            onTypedChange = actions.onDeleteTypedChange,
            onConfirm = actions.onDeleteConfirmed,
            onDismiss = actions.onDeleteDismissed,
        )
    }
}

@Composable
private fun ProfileBody(
    state: MemberProfileUiState,
    profile: MemberProfileUi,
    imageLoader: ImageLoader?,
    actions: MemberProfileActions,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(HEADER_BAND.dp)
                    .background(colors.primary),
            )
            ExpandablePhoto(
                profile = profile,
                imageLoader = imageLoader,
                isBusy = state.isPhotoBusy || state.isDownloadingPhoto,
                photoRevision = state.photoRevision,
                canChangePhoto = state.canChangePhoto,
                canRemovePhoto = state.canRemovePhoto,
                canDownloadPhoto = state.canDownloadPhoto,
                actions = actions,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = (HEADER_BAND - PHOTO_OVERLAP).dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        ProfileHeadline(profile)
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 32.dp),
        ) {
            SectionCard("Dados pessoais") {
                InfoRow("Nome", profile.firstName)
                InfoRow("Sobrenome", profile.lastName)
                InfoRow("Nascimento", profile.birthDateLabel)
                InfoRow("Idade", profile.ageLabel)
                InfoRow("Sexo", profile.genderLabel)
            }
            SectionCard("Vida na igreja") {
                InfoRow("Situação", profile.statusLabel)
                InfoRow("Cargo", profile.roleText)
                InfoRow("Batismo", profile.baptismLabel)
                MinistriesRow(profile.ministries)
            }
            ValidityCard(isValid = profile.isValid)
            HistoryCard(
                summary = state.lastChange?.let { "Última: ${it.editorName} ${it.text}" },
                onClick = actions.onOpenHistory,
            )
            Text(
                text = "Cadastrado em ${profile.createdAtLabel}",
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.canDelete) {
                OutlinedButton(
                    onClick = actions.onDeleteRequested,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, colors.error.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = colors.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Excluir membro", color = colors.error, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** How far the photo is opened: in place, popped in front of the screen, or full screen. */
private enum class PhotoStage { IN_PLACE, PREVIEW, FULL_SCREEN }

/**
 * The photo has no button of its own: a tap pops it in front of the screen as a square, a tap on
 * that opens it full screen, where the camera lives. Without a photo the initials go the same way.
 */
@Composable
private fun ExpandablePhoto(
    profile: MemberProfileUi,
    imageLoader: ImageLoader?,
    isBusy: Boolean,
    photoRevision: Int,
    canChangePhoto: Boolean,
    canRemovePhoto: Boolean,
    canDownloadPhoto: Boolean,
    actions: MemberProfileActions,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    var stage by rememberSaveable { mutableStateOf(PhotoStage.IN_PLACE) }
    var bounds by remember { mutableStateOf<Rect?>(null) }

    Box(modifier = modifier) {
        MemberAvatar(
            initials = profile.initials,
            photoUrl = profile.photoUrl,
            imageLoader = imageLoader,
            initialsSize = PHOTO_INITIALS.sp,
            photoRevision = photoRevision,
            modifier = Modifier
                .size(PHOTO_SIZE.dp)
                .onGloballyPositioned { bounds = Rect(it.positionOnScreen(), it.size.toSize()) }
                // While popped out, the photo is the one in front, not a copy left behind.
                .alpha(if (stage == PhotoStage.PREVIEW) 0f else 1f)
                .clip(CircleShape)
                .clickable(onClickLabel = "Ver foto") { stage = PhotoStage.PREVIEW }
                .border(5.dp, colors.background, CircleShape),
        )
        if (isBusy) CircularProgressIndicator(Modifier.align(Alignment.Center))
    }

    when (stage) {
        PhotoStage.IN_PLACE -> Unit
        PhotoStage.PREVIEW -> MemberPhotoPreview(
            initials = profile.initials,
            photoUrl = profile.photoUrl,
            imageLoader = imageLoader,
            origin = bounds,
            originInitialsSize = PHOTO_INITIALS.sp,
            onOpenFullScreen = { stage = PhotoStage.FULL_SCREEN },
            onDismiss = { stage = PhotoStage.IN_PLACE },
            photoRevision = photoRevision,
        )
        PhotoStage.FULL_SCREEN -> MemberPhotoViewer(
            initials = profile.initials,
            photoUrl = profile.photoUrl,
            imageLoader = imageLoader,
            isBusy = isBusy,
            onPickPhoto = actions.onPickPhoto.takeIf { canChangePhoto },
            onRemovePhoto = actions.onRemovePhotoRequested.takeIf { canRemovePhoto },
            onDownload = actions.onDownloadPhoto.takeIf { canDownloadPhoto },
            onDismiss = { stage = PhotoStage.IN_PLACE },
            photoRevision = photoRevision,
        )
    }
}

@Composable
private fun ProfileHeadline(profile: MemberProfileUi) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(horizontal = 24.dp),
    ) {
        Text(
            text = profile.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            color = colors.onBackground,
        )
        if (profile.headline.isNotEmpty()) {
            Text(profile.headline, fontSize = 15.sp, color = colors.onSurfaceVariant)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip(profile.statusLabel, fontSize = 13)
            profile.roleLabel?.let { Tag(it, colors.tertiaryContainer, colors.onTertiaryContainer, fontSize = 13) }
        }
    }
}

private const val HEADER_BAND = 120
private const val PHOTO_OVERLAP = 84
private const val PHOTO_SIZE = 148
private const val PHOTO_INITIALS = 44

@Preview(showBackground = true, widthDp = 390, heightDp = 1200)
@Composable
private fun MemberProfileContentPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        MemberProfileContent(
            state = MemberProfileUiState(
                isLoading = false,
                canEdit = true,
                canChangePhoto = true,
                canDelete = true,
                canRemovePhoto = true,
                profile = MemberProfileUi(
                    id = 12, name = "Ana Souza", initials = "AS", photoUrl = null,
                    headline = "36 anos · Feminino", statusLabel = "Ativo", roleLabel = "Diaconisa",
                    firstName = "Ana", lastName = "Souza", birthDateLabel = "02/04/1990", ageLabel = "36 anos",
                    genderLabel = "Feminino", roleText = "Diaconisa", baptismLabel = "12/06/2005 · há 21 anos",
                    ministries = listOf("Louvor", "Recepção"), createdAtLabel = "14/03/2026", isValid = true,
                ),
            ),
            imageLoader = null,
            actions = MemberProfileActions(),
        )
    }
}
