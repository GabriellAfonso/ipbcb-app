package com.ipb.castelobranco.features.admin.members.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.ipb.castelobranco.core.presentation.base.BaseScreen
import com.ipb.castelobranco.core.presentation.components.rememberSquarePhotoPicker
import com.ipb.castelobranco.core.presentation.theme.IPBCasteloBrancoTheme
import com.ipb.castelobranco.core.presentation.theme.ipbGreen
import com.ipb.castelobranco.features.admin.members.domain.model.MemberDraft
import com.ipb.castelobranco.features.admin.members.domain.model.MemberField
import com.ipb.castelobranco.features.admin.members.domain.model.MemberOptions
import com.ipb.castelobranco.features.admin.members.domain.model.NamedRef
import com.ipb.castelobranco.features.admin.members.presentation.components.BirthDateField
import com.ipb.castelobranco.features.admin.members.presentation.components.FieldError
import com.ipb.castelobranco.features.admin.members.presentation.components.GenderSelector
import com.ipb.castelobranco.features.admin.members.presentation.components.InvalidContainer
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberAvatar
import com.ipb.castelobranco.features.admin.members.presentation.components.MemberDateField
import com.ipb.castelobranco.features.admin.members.presentation.components.MinistriesPicker
import com.ipb.castelobranco.features.admin.members.presentation.components.OnInvalidContainer
import com.ipb.castelobranco.features.admin.members.presentation.components.OptionPicker
import com.ipb.castelobranco.features.admin.members.presentation.components.TonalTextField
import com.ipb.castelobranco.features.admin.members.presentation.state.MemberFormUiState
import com.ipb.castelobranco.features.admin.members.presentation.state.MembersEvent
import com.ipb.castelobranco.features.admin.members.presentation.viewmodel.MemberFormViewModel

@Composable
fun MemberFormScreen(
    viewModel: MemberFormViewModel,
    onBack: () -> Unit,
    onEvent: (MembersEvent) -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDiscard by remember { mutableStateOf(false) }
    val pickPhoto = rememberSquarePhotoPicker(onPicked = viewModel::onPhotoPicked)
    val leave = { if (state.hasUnsavedChanges) confirmDiscard = true else onBack() }

    BackHandler(enabled = state.hasUnsavedChanges) { confirmDiscard = true }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is MembersEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                else -> onEvent(event)
            }
        }
    }

    val showBand = !state.isLoading && state.options != null
    BaseScreen(
        tabName = if (state.isEditing) "Editar membro" else "Novo membro",
        showBackArrow = true,
        onBackClick = leave,
        // The green behind the sheet's rounded corners continues the band.
        containerColor = if (showBand) ipbGreen else MaterialTheme.colorScheme.background,
        topBarExtension = if (showBand) {
            { MemberBand(state, viewModel.imageLoader, pickPhoto) }
        } else {
            null
        },
    ) { innerPadding ->
        val contentModifier = if (showBand) {
            Modifier
                .padding(top = innerPadding.calculateTopPadding())
                .clip(RoundedCornerShape(topStart = SheetRadius, topEnd = SheetRadius))
                .background(MaterialTheme.colorScheme.surfaceDim)
                .padding(bottom = innerPadding.calculateBottomPadding())
        } else {
            Modifier.padding(innerPadding)
        }
        Box(contentModifier) {
            MemberFormContent(
                state = state,
                onDraftChanged = viewModel::onDraftChanged,
                onSave = viewModel::onSave,
                onRetry = viewModel::load,
            )
            SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter))
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Descartar alterações?") },
            text = { Text("O que você mudou neste membro ainda não foi salvo.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onBack() }) { Text("Descartar") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Continuar editando") } },
        )
    }
}

@Composable
fun MemberFormContent(
    state: MemberFormUiState,
    onDraftChanged: (MemberDraft) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
) {
    val options = state.options
    Box(Modifier.fillMaxSize()) {
        when {
            state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
            options == null -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            ) {
                Text(state.loadError.orEmpty(), textAlign = TextAlign.Center)
                Button(onClick = onRetry) { Text("Tentar novamente") }
            }
            else -> Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth()) { FormFields(state, options, onDraftChanged) }
                SaveBar(isSaving = state.isSaving, onSave = onSave)
            }
        }
    }
}

/** The same fields as before, in four cards: Nome, Dados pessoais, Vida na igreja and No app. */
@Composable
private fun FormFields(
    state: MemberFormUiState,
    options: MemberOptions,
    onChange: (MemberDraft) -> Unit,
) {
    val draft = state.draft
    val errors = state.fieldErrors
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        state.generalError?.let { message ->
            Text(
                text = message,
                color = OnInvalidContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(InvalidContainer)
                    .padding(12.dp),
            )
        }
        FormGroup("Nome") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TonalTextField("Primeiro nome", draft.firstName, errors[MemberField.FIRST_NAME], Modifier.weight(1f)) {
                    onChange(draft.copy(firstName = it))
                }
                TonalTextField("Sobrenome", draft.lastName, errors[MemberField.LAST_NAME], Modifier.weight(1f)) {
                    onChange(draft.copy(lastName = it))
                }
            }
            TonalTextField("Nome que aparece no app *", draft.name, errors[MemberField.NAME]) {
                onChange(draft.copy(name = it))
            }
        }
        FormGroup("Dados pessoais") {
            BirthDateField(
                birth = draft.birth,
                onChange = { onChange(draft.copy(birth = it)) },
                birthdayError = errors[MemberField.BIRTH_DAY] ?: errors[MemberField.BIRTH_MONTH],
                yearError = errors[MemberField.BIRTH_YEAR],
            )
            GenderSelector(selected = draft.gender, onSelect = { onChange(draft.copy(gender = it)) })
            FieldError(errors[MemberField.GENDER])
        }
        FormGroup("Vida na igreja") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OptionPicker(
                    label = "Situação",
                    options = options.statuses,
                    selectedId = draft.statusId,
                    onSelect = { onChange(draft.copy(statusId = it)) },
                    error = errors[MemberField.STATUS],
                    modifier = Modifier.weight(1f),
                )
                OptionPicker(
                    label = "Cargo",
                    options = options.roles,
                    selectedId = draft.roleId,
                    onSelect = { onChange(draft.copy(roleId = it)) },
                    error = errors[MemberField.ROLE],
                    modifier = Modifier.weight(1f),
                )
            }
            MemberDateField(
                label = "Batismo",
                date = draft.baptismDate,
                onChange = { onChange(draft.copy(baptismDate = it)) },
                error = errors[MemberField.BAPTISM_DATE],
            )
            MinistriesPicker(
                options = options.ministries,
                selectedIds = draft.ministryIds,
                onChange = { onChange(draft.copy(ministryIds = it)) },
                error = errors[MemberField.MINISTRIES],
            )
        }
        FormGroup("No app") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Perfil válido", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = if (draft.isValid) VALID_PROFILE_NOTE else INVALID_PROFILE_NOTE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = draft.isValid, onCheckedChange = { onChange(draft.copy(isValid = it)) })
            }
        }
    }
}

/** A green bar and the title, then the fields in a card. */
@Composable
private fun FormGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp),
        ) {
            Box(
                Modifier
                    .size(width = 4.dp, height = 15.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(CardRadius))
                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                .padding(12.dp),
            content = content,
        )
    }
}

/** Fixed at the bottom; disabled while saving. */
@Composable
private fun SaveBar(isSaving: Boolean, onSave: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Button(
            onClick = onSave,
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(containerColor = ipbGreen, contentColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .height(50.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text("Salvar")
            }
        }
    }
}

/**
 * The photo (tap to pick; a picked one is a preview, uploaded only on "Salvar"), the name and "situação · cargo",
 * inside the green top-bar block.
 */
@Composable
private fun MemberBand(state: MemberFormUiState, imageLoader: ImageLoader?, onPickPhoto: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(ipbGreen)
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 18.dp),
    ) {
        Box(Modifier.clickable(enabled = !state.isSaving, onClick = onPickPhoto)) {
            MemberAvatar(
                initials = state.initials,
                photoUrl = state.photoUrl,
                imageLoader = imageLoader,
                pickedPhoto = state.pickedPhoto,
                photoRevision = state.photoRevision,
                initialsSize = 22.sp,
                modifier = Modifier
                    .size(BAND_PHOTO_SIZE.dp)
                    .border(2.dp, Color.White.copy(alpha = BAND_BORDER_ALPHA), CircleShape),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 4.dp, y = 4.dp)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color.White),
            ) {
                Icon(
                    Icons.Filled.PhotoCamera,
                    contentDescription = "Trocar foto",
                    tint = ipbGreen,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
        Column {
            Text(
                text = state.bandTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
            state.bandSubtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = BAND_SECONDARY_ALPHA),
                )
            }
        }
    }
}

private const val VALID_PROFILE_NOTE = "Aparece na lista de membros e nos aniversários"
private const val INVALID_PROFILE_NOTE = "Não aparece na lista de membros nem nos aniversários"
private const val BAND_PHOTO_SIZE = 64
private const val BAND_BORDER_ALPHA = 0.6f
private const val BAND_SECONDARY_ALPHA = 0.82f
private val SheetRadius = 18.dp
private val CardRadius = 18.dp
@Preview(showBackground = true, widthDp = 390, heightDp = 1400)
@Composable
private fun MemberFormContentPreview() {
    IPBCasteloBrancoTheme(darkThemeOverride = false) {
        MemberFormContent(
            state = MemberFormUiState(
                isEditing = true,
                initials = "AS",
                isLoading = false,
                draft = MemberDraft(name = "", statusId = 3),
                options = MemberOptions(
                    statuses = listOf(NamedRef(1, "Ativo"), NamedRef(2, "Inativo"), NamedRef(3, "Visitante")),
                    roles = listOf(NamedRef(4, "Diaconisa")),
                    ministries = listOf(NamedRef(2, "Louvor"), NamedRef(5, "Recepção")),
                ),
                fieldErrors = mapOf(MemberField.NAME to "Informe o nome."),
            ),
            onDraftChanged = {},
            onSave = {},
            onRetry = {},
        )
    }
}
